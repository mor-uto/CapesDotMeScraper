package lol.moruto.scraper;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import lol.moruto.scraper.filter.FilterContext;
import lol.moruto.scraper.filter.impl.FilterByHypixelRank;

import javax.swing.*;
import java.awt.*;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Stream;

public class Gui {
    private final DefaultListModel<CapeType> desiredModel = new DefaultListModel<>();
    private final DefaultListModel<CapeType> blockedModel = new DefaultListModel<>();
    private JFrame frame;
    private static JTextArea consoleArea;
    private boolean darkMode = true;

    private final FilterContext filterContext;

    public Gui(FilterContext filterContext) {
        this.filterContext = filterContext;
        SwingUtilities.invokeLater(this::createAndShowGui);
    }

    private void createAndShowGui() {
        frame = new JFrame("capes.me Scraper");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1100, 750);
        frame.setLocationRelativeTo(null);

        JComboBox<CapeType> capeSelector = new JComboBox<>(CapeType.values());

        JList<CapeType> desiredList = new JList<>(desiredModel);

        JList<CapeType> blockedList = new JList<>(blockedModel);

        JButton addDesiredBtn = new JButton("➕ Desired");
        JButton addBlockedBtn = new JButton("➖ Block");
        JButton startBtn = new JButton("🚀 Start Scraping");
        JCheckBox filterRestChk = new JCheckBox("Filter Out All Other Capes");

        JComboBox<String> rankFilter = new JComboBox<>(Stream.concat(Stream.of("Don't Filter"), Stream.of(FilterByHypixelRank.Rank.values()).map(Enum::name)).toArray(String[]::new));

        JCheckBox outputJsonChk = new JCheckBox("Output as JSON");

        consoleArea = new JTextArea();
        consoleArea.setEditable(false);
        consoleArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        JScrollPane consoleScroll = new JScrollPane(consoleArea);
        consoleScroll.setBorder(BorderFactory.createTitledBorder("Console Output"));
        consoleScroll.setPreferredSize(new Dimension(1050, 200));

        Runnable updateButtons = () -> {
            CapeType sel = (CapeType) capeSelector.getSelectedItem();
            addDesiredBtn.setEnabled(sel != null && !desiredModel.contains(sel) && !blockedModel.contains(sel));
            addBlockedBtn.setEnabled(sel != null && !blockedModel.contains(sel) && !desiredModel.contains(sel));
        };

        Runnable autoFilter = () -> {
            blockedModel.clear();
            for (CapeType c : CapeType.values()) {
                if (!desiredModel.contains(c)) blockedModel.addElement(c);
            }
        };

        addDesiredBtn.addActionListener(e -> {
            CapeType sel = (CapeType) capeSelector.getSelectedItem();
            if (sel != null && !desiredModel.contains(sel) && !blockedModel.contains(sel)) {
                desiredModel.addElement(sel);
            }
            if (filterRestChk.isSelected()) autoFilter.run();
            updateButtons.run();
        });

        addBlockedBtn.addActionListener(e -> {
            CapeType sel = (CapeType) capeSelector.getSelectedItem();
            if (sel != null && !blockedModel.contains(sel) && !desiredModel.contains(sel)) {
                blockedModel.addElement(sel);
            }
            if (filterRestChk.isSelected()) autoFilter.run();
            updateButtons.run();
        });

        filterRestChk.addActionListener(e -> autoFilter.run());
        capeSelector.addActionListener(e -> updateButtons.run());

        startBtn.addActionListener(e -> {
            if (desiredModel.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Please select at least one desired cape.", "Warning", JOptionPane.WARNING_MESSAGE);
                return;
            }
            startBtn.setEnabled(false);
            consoleArea.setText("");

            EnumSet<CapeType> desired = toEnumSet(desiredModel);
            EnumSet<CapeType> blocked = blockedModel.isEmpty() ? EnumSet.noneOf(CapeType.class) : toEnumSet(blockedModel);

            filterContext.put("desiredCapes", desired);
            filterContext.put("blockedCapes", blocked);
            filterContext.put("desiredRank", rankFilter.getSelectedItem());
            filterContext.put("outputJson", outputJsonChk.isSelected());

            new Thread(() -> {
                log("Starting cape filtering...");
                List<String> results = Main.startFiltering(filterContext);

                if (results.isEmpty()) {
                    log("No players found after filtering.");
                    SwingUtilities.invokeLater(() -> startBtn.setEnabled(true));
                    return;
                }

                boolean outputJson = Boolean.TRUE.equals(filterContext.get("outputJson", Boolean.class));
                Main.writeResults(results, outputJson);

                log("\nAll steps complete.");
                SwingUtilities.invokeLater(() -> {
                    desiredModel.clear();
                    blockedModel.clear();
                    rankFilter.setSelectedIndex(0);
                    filterRestChk.setSelected(false);
                    outputJsonChk.setSelected(false);
                    startBtn.setEnabled(true);
                });
            }).start();
        });

        JToggleButton themeToggle = new JToggleButton("🌑 Theme", true);
        themeToggle.addActionListener(e -> {
            try {
                darkMode = !darkMode;
                UIManager.setLookAndFeel(darkMode ? new FlatDarkLaf() : new FlatLightLaf());
                SwingUtilities.updateComponentTreeUI(frame);
            } catch (Exception ignored) {}
        });

        JPanel capePanel = makePanel("Cape Selection", capeSelector, addDesiredBtn, addBlockedBtn, filterRestChk);
        JPanel desiredPanel = makePanel("✅ Desired Capes", desiredList);
        JPanel blockedPanel = makePanel("❌ Blocked Capes", blockedList);

        JPanel listPanel = new JPanel(new GridLayout(1, 3, 10, 10));
        listPanel.add(capePanel);
        listPanel.add(desiredPanel);
        listPanel.add(blockedPanel);

        JPanel rankPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        rankPanel.setBorder(BorderFactory.createTitledBorder("Hypixel Rank Filter"));
        rankPanel.add(new JLabel("Select Rank:"));
        rankPanel.add(rankFilter);

        JPanel outputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        outputPanel.setBorder(BorderFactory.createTitledBorder("Output Options"));
        outputPanel.add(outputJsonChk);

        JPanel topRightPanel = new JPanel(new BorderLayout());
        topRightPanel.add(rankPanel, BorderLayout.NORTH);
        topRightPanel.add(outputPanel, BorderLayout.SOUTH);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(listPanel, BorderLayout.CENTER);
        topPanel.add(topRightPanel, BorderLayout.EAST);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actionPanel.add(startBtn);
        actionPanel.add(themeToggle);

        JPanel content = new JPanel(new BorderLayout(10, 10));
        content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        content.add(topPanel, BorderLayout.CENTER);
        content.add(actionPanel, BorderLayout.NORTH);
        content.add(consoleScroll, BorderLayout.SOUTH);

        frame.setContentPane(content);
        updateButtons.run();
        frame.setVisible(true);
    }

    private static JPanel makePanel(String title, JComponent... comps) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder(title));
        for (JComponent comp : comps) {
            panel.add(comp);
            panel.add(Box.createVerticalStrut(5));
        }
        return panel;
    }

    private static EnumSet<CapeType> toEnumSet(DefaultListModel<CapeType> model) {
        EnumSet<CapeType> set = EnumSet.noneOf(CapeType.class);
        for (int i = 0; i < model.size(); i++) {
            set.add(model.get(i));
        }
        return set;
    }

    public static void log(String text) {
        SwingUtilities.invokeLater(() -> {
            consoleArea.append(text + "\n");
            consoleArea.setCaretPosition(consoleArea.getDocument().getLength());
        });
    }
}
