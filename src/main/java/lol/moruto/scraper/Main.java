package lol.moruto.scraper;

import com.formdev.flatlaf.*;
import lol.moruto.scraper.filter.*;
import lol.moruto.scraper.filter.impl.*;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;

public class Main {
    private final DefaultListModel<CapeType> desiredModel = new DefaultListModel<>(), blockedModel = new DefaultListModel<>();
    private final java.util.List<String> scrapedIGNs = new ArrayList<>();
    private boolean darkMode = true;
    static JTextArea consoleArea = new JTextArea();

    public Main() {
        JFrame f = new JFrame("capes.me Scraper");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setSize(1100, 750);
        f.setLocationRelativeTo(null);

        JComboBox<CapeType> capeSel = new JComboBox<>(CapeType.values());
        JButton addDesired = new JButton("➕ Desired"), addBlocked = new JButton("➖ Block"), start = new JButton("🚀 Start Scraping");
        JCheckBox filterRest = new JCheckBox("Filter Out All Other Capes");
        JComboBox<String> rankFilter = new JComboBox<>(java.util.stream.Stream.concat(java.util.stream.Stream.of("Don't Filter"), Arrays.stream(FilterByHypixelRank.Rank.values()).map(Enum::name)).toArray(String[]::new));

        JList<CapeType> desiredList = new JList<>(desiredModel), blockedList = new JList<>(blockedModel);

        consoleArea.setEditable(false);
        consoleArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        JScrollPane consoleScroll = new JScrollPane(consoleArea);
        consoleScroll.setBorder(BorderFactory.createTitledBorder("Console Output"));
        consoleScroll.setPreferredSize(new Dimension(1050, 200));

        Runnable updateButtons = () -> {
            CapeType sel = (CapeType) capeSel.getSelectedItem();
            addDesired.setEnabled(sel != null && !desiredModel.contains(sel) && !blockedModel.contains(sel));
            addBlocked.setEnabled(sel != null && !blockedModel.contains(sel) && !desiredModel.contains(sel));
        };
        Runnable autoFilter = () -> {
            blockedModel.clear();
            for (CapeType c : CapeType.values()) if (!desiredModel.contains(c)) blockedModel.addElement(c);
        };

        addDesired.addActionListener(e -> {
            CapeType sel = (CapeType) capeSel.getSelectedItem();
            if (sel != null && !desiredModel.contains(sel) && !blockedModel.contains(sel)) desiredModel.addElement(sel);
            if (filterRest.isSelected()) autoFilter.run();
            updateButtons.run();
        });
        addBlocked.addActionListener(e -> {
            CapeType sel = (CapeType) capeSel.getSelectedItem();
            if (sel != null && !blockedModel.contains(sel) && !desiredModel.contains(sel)) blockedModel.addElement(sel);
            if (filterRest.isSelected()) autoFilter.run();
            updateButtons.run();
        });
        filterRest.addActionListener(e -> autoFilter.run());
        capeSel.addActionListener(e -> updateButtons.run());

        start.addActionListener(e -> {
            if (desiredModel.isEmpty()) {
                JOptionPane.showMessageDialog(f, "Please select at least one desired cape.", "Warning", JOptionPane.WARNING_MESSAGE);
                return;
            }

            start.setEnabled(false);
            consoleArea.setText("");
            EnumSet<CapeType> desired = toEnumSet(desiredModel);
            EnumSet<CapeType> blocked = blockedModel.isEmpty() ? EnumSet.noneOf(CapeType.class) : toEnumSet(blockedModel);
            FilterContext ctx = new FilterContext();
            ctx.put("desiredCapes", desired);
            ctx.put("blockedCapes", blocked);
            ctx.put("desiredRank", rankFilter.getSelectedItem());

            new Thread(() -> {
                log("Starting cape filtering...");
                List<String> results = new FilterManager().startFiltering(ctx);

                if (results.isEmpty()) {
                    log("No players found after filtering.");
                    SwingUtilities.invokeLater(() -> start.setEnabled(true));
                    return;
                }

                try (BufferedWriter w = new BufferedWriter(new FileWriter("results.txt"))) {
                    for (String ign : results) {
                        w.write(ign + "\n");
                    }
                    log("Results saved to results.txt");
                } catch (IOException ex) {
                    log("Failed to write results: " + ex.getMessage());
                }

                log("\nAll steps complete.");

                SwingUtilities.invokeLater(() -> {
                    desiredModel.clear();
                    blockedModel.clear();
                    rankFilter.setSelectedIndex(0);
                    filterRest.setSelected(false);
                    start.setEnabled(true);
                });
            }).start();
        });


        JToggleButton themeToggle = new JToggleButton("🌑 Theme", true);
        themeToggle.addActionListener(e -> {
            try {
                darkMode = !darkMode;
                UIManager.setLookAndFeel(darkMode ? new FlatDarkLaf() : new FlatLightLaf());
                SwingUtilities.updateComponentTreeUI(f);
            } catch (Exception ignored) {}
        });

        JPanel listPanel = new JPanel(new GridLayout(1, 3, 10, 10));
        listPanel.add(makePanel("Cape Selection", capeSel, addDesired, addBlocked, filterRest));
        listPanel.add(makePanel("✅ Desired Capes", desiredList));
        listPanel.add(makePanel("❌ Blocked Capes", blockedList));

        JPanel rankPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        rankPanel.setBorder(BorderFactory.createTitledBorder("Hypixel Rank Filter (optional)"));
        rankPanel.add(new JLabel("Select Rank:"));
        rankPanel.add(rankFilter);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(listPanel, BorderLayout.CENTER);
        topPanel.add(rankPanel, BorderLayout.EAST);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actionPanel.add(start);
        actionPanel.add(themeToggle);

        JPanel content = new JPanel(new BorderLayout(10, 10));
        content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        content.add(topPanel, BorderLayout.CENTER);
        content.add(actionPanel, BorderLayout.NORTH);
        content.add(consoleScroll, BorderLayout.SOUTH);

        f.setContentPane(content);
        updateButtons.run();
        f.setVisible(true);
    }

    private static JPanel makePanel(String title, JComponent... comps) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createTitledBorder(title));
        for (JComponent c : comps) {
            p.add(c);
            p.add(Box.createVerticalStrut(5));
        }
        return p;
    }

    private static JPanel makePanel(String title, JList<?> list) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder(title));
        p.add(new JScrollPane(list), BorderLayout.CENTER);
        return p;
    }

    private static EnumSet<CapeType> toEnumSet(DefaultListModel<CapeType> model) {
        EnumSet<CapeType> set = EnumSet.noneOf(CapeType.class);
        for (int i = 0; i < model.size(); i++) set.add(model.get(i));
        return set;
    }

    public static void log(String msg) {
        SwingUtilities.invokeLater(() -> {
            consoleArea.append(msg + "\n");
            consoleArea.setCaretPosition(consoleArea.getDocument().getLength());
        });
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Exception ignored) {}
        SwingUtilities.invokeLater(Main::new);
    }
}
