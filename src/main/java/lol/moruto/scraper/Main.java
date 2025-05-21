package lol.moruto.scraper;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class Main {

    private final DefaultListModel<CapeType> desiredModel = new DefaultListModel<>();
    private final DefaultListModel<CapeType> blockedModel = new DefaultListModel<>();
    private boolean isDarkMode = true;

    private final List<String> scrapedIGNs = new ArrayList<>();

    public Main() {
        JFrame frame = new JFrame("capes.me Scraper");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1100, 750);
        frame.setLocationRelativeTo(null);

        JComboBox<CapeType> capeSelector = new JComboBox<>(CapeType.values());
        JButton addToDesired = new JButton("➕ Desired");
        JButton addToBlocked = new JButton("➖ Block");
        JCheckBox filterOutRest = new JCheckBox("Filter Out All Other Capes");

        JList<CapeType> desiredList = new JList<>(desiredModel);
        JList<CapeType> blockedList = new JList<>(blockedModel);

        JScrollPane desiredScroll = new JScrollPane(desiredList);
        desiredScroll.setPreferredSize(new Dimension(250, 300));

        JScrollPane blockedScroll = new JScrollPane(blockedList);
        blockedScroll.setPreferredSize(new Dimension(250, 300));

        JTextArea consoleArea = new JTextArea();
        consoleArea.setEditable(false);
        consoleArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        JScrollPane consoleScroll = new JScrollPane(consoleArea);
        consoleScroll.setPreferredSize(new Dimension(1050, 200));

        JComboBox<String> rankComboBox = new JComboBox<>();
        rankComboBox.addItem("Don't Filter");
        for (FilterByHypixelRank.Rank rank : FilterByHypixelRank.Rank.values()) {
            rankComboBox.addItem(rank.name());
        }

        JButton startButton = new JButton("\ud83d\ude80 Start Scraping");

        updateButtons(capeSelector, addToDesired, addToBlocked);

        startButton.addActionListener(e -> {
            if (desiredModel.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Please select at least one desired cape.", "Warning", JOptionPane.WARNING_MESSAGE);
                return;
            }

            startButton.setEnabled(false);
            scrapedIGNs.clear();
            consoleArea.setText("");

            Set<CapeType> desiredSet = toSet(desiredModel);
            Set<CapeType> blockedSet = blockedModel.isEmpty() ? EnumSet.noneOf(CapeType.class) : toSet(blockedModel);

            FilterByCapes filterByCapes = new FilterByCapes(desiredSet, blockedSet, consoleArea);
            FilterByHypixelRank filterByHypixelRank = new FilterByHypixelRank(consoleArea);

            new Thread(() -> {
                logToConsole(consoleArea, "Starting cape filtering...");
                List<String> ignsFromCapes = filterByCapes.startScraping();

                if (ignsFromCapes.isEmpty()) {
                    logToConsole(consoleArea, "No players found from cape filtering, aborting further steps.");
                    SwingUtilities.invokeLater(() -> startButton.setEnabled(true));
                    return;
                }
                scrapedIGNs.addAll(ignsFromCapes);

                String selectedRank = (String) rankComboBox.getSelectedItem();
                if (selectedRank != null && !"Don't Filter".equals(selectedRank)) {
                    logToConsole(consoleArea, "\n--- Starting Hypixel Rank filtering ---\n");
                    filterByHypixelRank.startFiltering(scrapedIGNs, selectedRank);
                    filterByHypixelRank.waitTillFinished();
                } else {
                    logToConsole(consoleArea, "Skipped Hypixel Rank filtering.");
                }

                logToConsole(consoleArea, "\nAll steps complete.");

                SwingUtilities.invokeLater(() -> {
                    desiredModel.clear();
                    blockedModel.clear();
                    rankComboBox.setSelectedIndex(0);
                    filterOutRest.setSelected(false);
                    startButton.setEnabled(true);
                });

            }).start();
        });

        addToDesired.addActionListener(e -> {
            CapeType selected = (CapeType) capeSelector.getSelectedItem();
            if (selected != null && !contains(desiredModel, selected) && !contains(blockedModel, selected)) {
                desiredModel.addElement(selected);
                if (filterOutRest.isSelected()) updateAutoFilteredCapes();
            }
            updateButtons(capeSelector, addToDesired, addToBlocked);
        });

        addToBlocked.addActionListener(e -> {
            CapeType selected = (CapeType) capeSelector.getSelectedItem();
            if (selected != null && !contains(blockedModel, selected) && !contains(desiredModel, selected)) {
                blockedModel.addElement(selected);
                if (filterOutRest.isSelected()) updateAutoFilteredCapes();
            }
            updateButtons(capeSelector, addToDesired, addToBlocked);
        });

        filterOutRest.addActionListener(e -> updateAutoFilteredCapes());
        capeSelector.addActionListener(e -> updateButtons(capeSelector, addToDesired, addToBlocked));

        JToggleButton themeToggle = new JToggleButton("\ud83c\udf11 Theme");
        themeToggle.setSelected(true);
        themeToggle.addActionListener(e -> {
            isDarkMode = !isDarkMode;
            try {
                if (isDarkMode) FlatDarkLaf.setup();
                else FlatLightLaf.setup();
                SwingUtilities.updateComponentTreeUI(frame);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        JPanel selectorPanel = new JPanel();
        selectorPanel.setLayout(new BoxLayout(selectorPanel, BoxLayout.Y_AXIS));
        selectorPanel.setBorder(BorderFactory.createTitledBorder("Cape Selection"));
        selectorPanel.add(capeSelector);
        selectorPanel.add(Box.createVerticalStrut(5));
        selectorPanel.add(addToDesired);
        selectorPanel.add(Box.createVerticalStrut(5));
        selectorPanel.add(addToBlocked);
        selectorPanel.add(Box.createVerticalStrut(10));
        selectorPanel.add(filterOutRest);

        JPanel desiredPanel = new JPanel(new BorderLayout());
        desiredPanel.setBorder(BorderFactory.createTitledBorder("✅ Desired Capes"));
        desiredPanel.add(new JScrollPane(new JList<>(desiredModel)), BorderLayout.CENTER);

        JPanel blockedPanel = new JPanel(new BorderLayout());
        blockedPanel.setBorder(BorderFactory.createTitledBorder("❌ Blocked Capes"));
        blockedPanel.add(new JScrollPane(new JList<>(blockedModel)), BorderLayout.CENTER);

        JPanel listPanel = new JPanel(new GridLayout(1, 3, 10, 10));
        listPanel.add(selectorPanel);
        listPanel.add(desiredPanel);
        listPanel.add(blockedPanel);

        JPanel rankFilterPanel = new JPanel();
        rankFilterPanel.setBorder(BorderFactory.createTitledBorder("Hypixel Rank Filter (optional)"));
        rankFilterPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        rankFilterPanel.add(new JLabel("Select Rank:"));
        rankFilterPanel.add(rankComboBox);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(listPanel, BorderLayout.CENTER);
        topPanel.add(rankFilterPanel, BorderLayout.EAST);

        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        controlPanel.add(startButton);
        controlPanel.add(themeToggle);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.add(topPanel, BorderLayout.CENTER);
        mainPanel.add(controlPanel, BorderLayout.NORTH);
        mainPanel.add(consoleScroll, BorderLayout.SOUTH);

        frame.setContentPane(mainPanel);
        frame.setVisible(true);
    }

    private void updateAutoFilteredCapes() {
        blockedModel.removeAllElements();
        List<CapeType> desired = Collections.list(desiredModel.elements());
        for (CapeType cape : CapeType.values()) {
            if (!desired.contains(cape)) {
                blockedModel.addElement(cape);
            }
        }
    }

    private void updateButtons(JComboBox<CapeType> selector, JButton addToDesired, JButton addToBlocked) {
        CapeType selected = (CapeType) selector.getSelectedItem();
        if (selected == null) return;
        boolean inDesired = contains(desiredModel, selected);
        boolean inBlocked = contains(blockedModel, selected);
        addToDesired.setEnabled(!inDesired && !inBlocked);
        addToBlocked.setEnabled(!inBlocked && !inDesired);
    }

    private boolean contains(DefaultListModel<CapeType> model, CapeType cape) {
        return Collections.list(model.elements()).contains(cape);
    }

    private Set<CapeType> toSet(DefaultListModel<CapeType> model) {
        return EnumSet.copyOf(Collections.list(model.elements()));
    }

    private static void logToConsole(JTextArea console, String message) {
        SwingUtilities.invokeLater(() -> {
            console.append(message + "\n");
            console.setCaretPosition(console.getDocument().getLength());
        });
    }

    public static void main(String[] args) {
        try {
            FlatDarkLaf.setup();
        } catch (Exception ex) {
            System.err.println("Failed to initialize FlatLaf");
        }
        SwingUtilities.invokeLater(Main::new);
    }
}
