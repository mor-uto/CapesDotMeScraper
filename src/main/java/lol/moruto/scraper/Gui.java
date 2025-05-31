package lol.moruto.scraper;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import lol.moruto.scraper.filter.FilterContext;
import lol.moruto.scraper.filter.impl.FilterByHypixelRank;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class Gui {
    private final DefaultListModel<CapeType> desiredModel = new DefaultListModel<>();
    private final DefaultListModel<CapeType> blockedModel = new DefaultListModel<>();
    private final FilterContext filterContext;

    private JFrame frame;
    private static JTextArea consoleArea;
    private boolean darkMode = true;

    public Gui(FilterContext filterContext) {
        this.filterContext = filterContext;
        SwingUtilities.invokeLater(this::createAndShowGui);
    }

    private void createAndShowGui() {
        frame = new JFrame("capes.me Scraper");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1150, 800);
        frame.setLocationRelativeTo(null);

        JComboBox<CapeType> capeSelector = new JComboBox<>(CapeType.values());
        JList<CapeType> desiredList = new JList<>(desiredModel);
        JList<CapeType> blockedList = new JList<>(blockedModel);
        JButton addDesiredBtn = new JButton("➕ Desired");
        JButton addBlockedBtn = new JButton("➖ Block");
        JCheckBox filterRestChk = new JCheckBox("Filter Out All Other Capes");

        JComboBox<String> rankFilter = new JComboBox<>(
                Stream.concat(Stream.of("Don't Filter"), Stream.of(FilterByHypixelRank.Rank.values()).map(Enum::name))
                        .toArray(String[]::new)
        );

        JCheckBox outputJsonChk = new JCheckBox("Output as JSON");

        JTextField nameChangeInput = new JTextField(20);
        nameChangeInput.setBorder(BorderFactory.createTitledBorder("NCs (e.g., 1-3,5,7)"));

        String[] sites = {"NameMC.com", "Laby.net", "Livzmc", "Crafty.gg"};
        JList<String> ncWebsite = new JList<>(sites);
        ncWebsite.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        ncWebsite.setVisibleRowCount(4);

        ncWebsite.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, false, false);
                if ("NameMC.com".equals(value)) {
                    label.setEnabled(false);
                    label.setForeground(Color.GRAY);
                    label.setText("<html><strike>" + value + "</strike></html>");
                } else if (isSelected) {
                    label.setBackground(list.getSelectionBackground());
                    label.setForeground(list.getSelectionForeground());
                }
                return label;
            }
        });
        ncWebsite.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && ncWebsite.getSelectedValuesList().contains("NameMC.com")) {
                ncWebsite.removeSelectionInterval(0, 0); // prevent selection
            }
        });


        JTextArea ignInputArea = new JTextArea(8, 40);
        ignInputArea.setBorder(BorderFactory.createTitledBorder("Manual IGN Input"));
        JScrollPane ignScroll = new JScrollPane(ignInputArea);

        JButton loadFileBtn = new JButton("📂 Load IGN List");
        JButton filterSpecificBtn = new JButton("🔍 Filter Specific List");
        JButton startBtn = new JButton("🚀 Start Scraping");
        JToggleButton themeToggle = new JToggleButton("🌑 Theme", true);

        consoleArea = createConsoleArea();

        setupCapeButtons(capeSelector, addDesiredBtn, addBlockedBtn, filterRestChk);
        startBtn.addActionListener(e -> startScraping(desiredModel, blockedModel, rankFilter, outputJsonChk, startBtn, nameChangeInput, ncWebsite));
        loadFileBtn.addActionListener(e -> loadIGNFile(ignInputArea));
        filterSpecificBtn.addActionListener(e -> filterFromSpecificList(ignInputArea, rankFilter, outputJsonChk, filterSpecificBtn));
        themeToggle.addActionListener(e -> toggleTheme(themeToggle));

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Capes", buildCapeTab(capeSelector, addDesiredBtn, addBlockedBtn, filterRestChk, desiredList, blockedList));
        tabbedPane.addTab("Hypixel Rank", buildRankTab(rankFilter));
        tabbedPane.addTab("Specific Filtering", buildSpecificTab(ignScroll, loadFileBtn, filterSpecificBtn));
        tabbedPane.addTab("Output Options", buildOutputTab(outputJsonChk));
        tabbedPane.addTab("Name History", buildNameHistoryTab(nameChangeInput, ncWebsite));

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actionPanel.add(startBtn);
        actionPanel.add(themeToggle);

        JPanel mainContent = new JPanel(new BorderLayout(10, 10));
        mainContent.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainContent.add(tabbedPane, BorderLayout.CENTER);
        mainContent.add(new JScrollPane(consoleArea), BorderLayout.SOUTH);

        JPanel fullContent = new JPanel(new BorderLayout());
        fullContent.add(actionPanel, BorderLayout.NORTH);
        fullContent.add(mainContent, BorderLayout.CENTER);

        frame.setContentPane(fullContent);
        frame.setVisible(true);
    }

    private void setupCapeButtons(JComboBox<CapeType> capeSelector, JButton addDesiredBtn, JButton addBlockedBtn, JCheckBox filterRestChk) {
        capeSelector.addActionListener(e -> updateAddButtons(capeSelector, addDesiredBtn, addBlockedBtn));
        filterRestChk.addActionListener(e -> {
            if (filterRestChk.isSelected()) autoFilterBlocked();
        });

        addDesiredBtn.addActionListener(e -> {
            CapeType selected = (CapeType) capeSelector.getSelectedItem();
            if (selected != null && !desiredModel.contains(selected) && !blockedModel.contains(selected)) {
                desiredModel.addElement(selected);
            }
            if (filterRestChk.isSelected()) autoFilterBlocked();
            updateAddButtons(capeSelector, addDesiredBtn, addBlockedBtn);
        });

        addBlockedBtn.addActionListener(e -> {
            CapeType selected = (CapeType) capeSelector.getSelectedItem();
            if (selected != null && !blockedModel.contains(selected) && !desiredModel.contains(selected)) {
                blockedModel.addElement(selected);
            }
            if (filterRestChk.isSelected()) autoFilterBlocked();
            updateAddButtons(capeSelector, addDesiredBtn, addBlockedBtn);
        });

        updateAddButtons(capeSelector, addDesiredBtn, addBlockedBtn);
    }

    private void updateAddButtons(JComboBox<CapeType> capeSelector, JButton addDesiredBtn, JButton addBlockedBtn) {
        CapeType selected = (CapeType) capeSelector.getSelectedItem();
        boolean canAddDesired = selected != null && !desiredModel.contains(selected) && !blockedModel.contains(selected);
        boolean canAddBlocked = selected != null && !blockedModel.contains(selected) && !desiredModel.contains(selected);
        addDesiredBtn.setEnabled(canAddDesired);
        addBlockedBtn.setEnabled(canAddBlocked);
    }

    private void autoFilterBlocked() {
        blockedModel.clear();
        for (CapeType cape : CapeType.values()) {
            if (!desiredModel.contains(cape)) blockedModel.addElement(cape);
        }
    }

    private void startScraping(DefaultListModel<CapeType> desiredModel, DefaultListModel<CapeType> blockedModel,
                               JComboBox<String> rankFilter, JCheckBox outputJsonChk,
                               JButton startBtn, JTextField nameChangeInput, JList<String> ncWebsite) {
        if (desiredModel.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Please select at least one desired cape.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        startBtn.setEnabled(false);
        clearConsole();

        filterContext.put("desiredCapes", toEnumSet(desiredModel));
        filterContext.put("blockedCapes", blockedModel.isEmpty() ? EnumSet.noneOf(CapeType.class) : toEnumSet(blockedModel));
        filterContext.put("desiredRank", rankFilter.getSelectedItem());
        filterContext.put("outputJson", outputJsonChk.isSelected());
        filterContext.put("ncs", parseNcInput(nameChangeInput.getText()));
        filterContext.put("ncWebsite", new HashSet<>(ncWebsite.getSelectedValuesList()));


        new Thread(() -> {
            log("Starting cape filtering...");
            List<String> results = Main.startFiltering(filterContext);

            if (results.isEmpty()) {
                log("No players found after filtering.");
                enableButtonLater(startBtn);
                return;
            }

            Main.writeResults(results, outputJsonChk.isSelected());
            log("\nAll steps complete.");

            SwingUtilities.invokeLater(() -> {
                desiredModel.clear();
                blockedModel.clear();
                rankFilter.setSelectedIndex(0);
                outputJsonChk.setSelected(false);
                startBtn.setEnabled(true);
            });
        }).start();
    }

    private void filterFromSpecificList(JTextArea ignInputArea, JComboBox<String> rankFilter,
                                        JCheckBox outputJsonChk, JButton filterSpecificBtn) {
        String rawText = ignInputArea.getText().trim();
        if (rawText.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Please enter or load a list of IGNs.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<String> igns = Arrays.stream(rawText.split("[,\n\r]+"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        if (desiredModel.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Please select at least one desired cape.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        EnumSet<CapeType> desired = toEnumSet(desiredModel);
        EnumSet<CapeType> blocked = blockedModel.isEmpty() ? EnumSet.noneOf(CapeType.class) : toEnumSet(blockedModel);

        filterContext.put("desiredCapes", desired);
        filterContext.put("blockedCapes", blocked);
        filterContext.put("desiredRank", rankFilter.getSelectedItem());
        filterContext.put("outputJson", outputJsonChk.isSelected());
        filterContext.put("specificPlayers", igns);

        filterSpecificBtn.setEnabled(false);
        clearConsole();

        new Thread(() -> {
            log("Starting filtering from specific list of " + igns.size() + " players...");
            List<String> filtered = Main.startFiltering(filterContext);

            if (filtered.isEmpty()) {
                log("No players matched the filtering criteria.");
                enableButtonLater(filterSpecificBtn);
                return;
            }

            Main.writeResults(filtered, outputJsonChk.isSelected());
            log("\nFiltering from specific list complete.");
            filterContext.remove("specificPlayers");
            enableButtonLater(filterSpecificBtn);
        }).start();
    }

    private void loadIGNFile(JTextArea ignInputArea) {
        JFileChooser chooser = new JFileChooser();
        int result = chooser.showOpenDialog(frame);
        if (result == JFileChooser.APPROVE_OPTION) {
            try (Scanner scanner = new Scanner(chooser.getSelectedFile())) {
                StringBuilder sb = new StringBuilder();
                while (scanner.hasNextLine()) {
                    sb.append(scanner.nextLine().trim()).append("\n");
                }
                ignInputArea.setText(sb.toString());
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(frame, "Failed to load file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void toggleTheme(JToggleButton themeToggle) {
        try {
            darkMode = !darkMode;
            UIManager.setLookAndFeel(darkMode ? new FlatDarkLaf() : new FlatLightLaf());
            SwingUtilities.updateComponentTreeUI(frame);
            themeToggle.setSelected(darkMode);
        } catch (Exception ignored) {
        }
    }

    private JTextArea createConsoleArea() {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setFont(new Font("Consolas", Font.PLAIN, 13));
        area.setBorder(BorderFactory.createTitledBorder("Console Output"));
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setRows(8);
        return area;
    }

    private JPanel buildCapeTab(JComboBox<CapeType> selector, JButton addDesired, JButton addBlocked, JCheckBox filterRest, JList<CapeType> desiredList, JList<CapeType> blockedList) {
        JPanel listPanel = new JPanel(new GridLayout(1, 3, 10, 10));
        listPanel.add(makePanel("Cape Selection", selector, addDesired, addBlocked, filterRest));
        listPanel.add(makePanel("✅ Desired Capes", new JScrollPane(desiredList)));
        listPanel.add(makePanel("❌ Blocked Capes", new JScrollPane(blockedList)));
        JPanel tab = new JPanel(new BorderLayout(10, 10));
        tab.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        tab.add(listPanel);
        return tab;
    }

    private JPanel buildRankTab(JComboBox<String> rankFilter) {
        JPanel rankPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        rankPanel.setBorder(BorderFactory.createTitledBorder("Hypixel Rank Filter"));
        rankPanel.add(new JLabel("Select Rank:"));
        rankPanel.add(rankFilter);
        JPanel tab = new JPanel(new BorderLayout(10, 10));
        tab.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        tab.add(rankPanel, BorderLayout.NORTH);
        return tab;
    }

    private JPanel buildSpecificTab(JScrollPane ignScroll, JButton loadFile, JButton filterBtn) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder("Filter From Specific Player List"));
        panel.add(ignScroll);
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        btnPanel.add(loadFile);
        btnPanel.add(filterBtn);
        panel.add(btnPanel);
        return panel;
    }

    private JPanel buildOutputTab(JCheckBox outputJsonChk) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(outputJsonChk);
        return panel;
    }

    private JPanel buildNameHistoryTab(JTextField ncInput, JList<String> ncWebsite) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(ncInput, BorderLayout.NORTH);

        JLabel websiteLabel = new JLabel("Select NC Website(s):");
        JScrollPane listScroll = new JScrollPane(ncWebsite);
        listScroll.setPreferredSize(new Dimension(200, 80));

        JLabel warning = new JLabel("⚠ NameMC implementation WIP");
        warning.setForeground(new Color(255, 165, 0));

        panel.add(inputPanel);
        panel.add(Box.createVerticalStrut(10));
        panel.add(websiteLabel);
        panel.add(listScroll);
        panel.add(Box.createVerticalStrut(5));
        panel.add(warning);

        return panel;
    }


    private static JPanel makePanel(String title, JComponent... components) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder(title));
        for (JComponent comp : components) {
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

    private Set<Integer> parseNcInput(String input) {
        Set<Integer> set = new HashSet<>();
        Matcher matcher = Pattern.compile("(\\d+)(?:-(\\d+))?").matcher(input);
        while (matcher.find()) {
            int start = Integer.parseInt(matcher.group(1));
            int end = matcher.group(2) != null ? Integer.parseInt(matcher.group(2)) : start;
            for (int i = start; i <= end; i++) set.add(i);
        }
        return set;
    }

    public static void log(String text) {
        SwingUtilities.invokeLater(() -> {
            consoleArea.append(text + "\n");
            consoleArea.setCaretPosition(consoleArea.getDocument().getLength());
        });
    }

    private static void clearConsole() {
        SwingUtilities.invokeLater(() -> consoleArea.setText(""));
    }

    private static void enableButtonLater(JButton button) {
        SwingUtilities.invokeLater(() -> button.setEnabled(true));
    }
}
