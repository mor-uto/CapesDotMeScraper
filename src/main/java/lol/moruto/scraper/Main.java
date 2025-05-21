package lol.moruto.scraper;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.EnumSet;
import java.util.List;

public class Main {

    private final DefaultListModel<CapeType> desiredModel = new DefaultListModel<>();
    private final DefaultListModel<CapeType> blockedModel = new DefaultListModel<>();
    private final List<String> scrapedIGNs = new ArrayList<>();
    private boolean isDarkMode = true;

    static JTextArea consoleArea = new JTextArea();

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

        consoleArea.setEditable(false);
        consoleArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        consoleArea.setLineWrap(true);
        consoleArea.setWrapStyleWord(true);
        consoleArea.setMargin(new Insets(5, 5, 5, 5));

        JScrollPane consoleScroll = new JScrollPane(consoleArea);
        consoleScroll.setBorder(BorderFactory.createTitledBorder("Console Output"));
        consoleScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        consoleScroll.setPreferredSize(new Dimension(1050, 200));

        JComboBox<String> rankComboBox = new JComboBox<>();
        rankComboBox.addItem("Don't Filter");
        for (FilterByHypixelRank.Rank rank : FilterByHypixelRank.Rank.values()) {
            rankComboBox.addItem(rank.name());
        }

        JButton startButton = new JButton("🚀 Start Scraping");

        Runnable updateButtons = () -> {
            CapeType selected = (CapeType) capeSelector.getSelectedItem();
            boolean inDesired = desiredModel.contains(selected);
            boolean inBlocked = blockedModel.contains(selected);
            addToDesired.setEnabled(!inDesired && !inBlocked);
            addToBlocked.setEnabled(!inBlocked && !inDesired);
        };

        Runnable updateAutoFilteredCapes = () -> {
            blockedModel.clear();
            for (CapeType cape : CapeType.values()) {
                if (!desiredModel.contains(cape)) {
                    blockedModel.addElement(cape);
                }
            }
        };

        addToDesired.addActionListener(e -> {
            CapeType selected = (CapeType) capeSelector.getSelectedItem();
            if (selected != null && !desiredModel.contains(selected) && !blockedModel.contains(selected)) {
                desiredModel.addElement(selected);
                if (filterOutRest.isSelected()) {
                    updateAutoFilteredCapes.run();
                }
            }
            updateButtons.run();
        });

        addToBlocked.addActionListener(e -> {
            CapeType selected = (CapeType) capeSelector.getSelectedItem();
            if (selected != null && !blockedModel.contains(selected) && !desiredModel.contains(selected)) {
                blockedModel.addElement(selected);
                if (filterOutRest.isSelected()) {
                    updateAutoFilteredCapes.run();
                }
            }
            updateButtons.run();
        });

        filterOutRest.addActionListener(e -> updateAutoFilteredCapes.run());
        capeSelector.addActionListener(e -> updateButtons.run());

        startButton.addActionListener(e -> {
            if (desiredModel.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Please select at least one desired cape.", "Warning", JOptionPane.WARNING_MESSAGE);
                return;
            }

            startButton.setEnabled(false);
            scrapedIGNs.clear();
            consoleArea.setText("");

            EnumSet<CapeType> desiredSet = EnumSet.copyOf(toEnumSet(desiredModel));
            EnumSet<CapeType> blockedSet = blockedModel.isEmpty()
                    ? EnumSet.noneOf(CapeType.class)
                    : EnumSet.copyOf(toEnumSet(blockedModel));

            FilterByCapes filterByCapes = new FilterByCapes(desiredSet, blockedSet);
            FilterByHypixelRank filterByRank = new FilterByHypixelRank();

            new Thread(() -> {
                log("Starting cape filtering...");
                List<String> igns = filterByCapes.startScraping();

                if (igns.isEmpty()) {
                    log("No players found from cape filtering, aborting further steps.");
                    SwingUtilities.invokeLater(() -> startButton.setEnabled(true));
                    return;
                }

                scrapedIGNs.addAll(igns);

                String selectedRank = (String) rankComboBox.getSelectedItem();
                if (!"Don't Filter".equals(selectedRank)) {
                    filterByRank.startFiltering(scrapedIGNs, selectedRank);
                } else {
                    log("Skipped Hypixel Rank filtering.");
                }

                try (BufferedWriter writer = new BufferedWriter(new FileWriter("results.txt"))) {
                    for (String ign : scrapedIGNs) {
                        writer.write(ign);
                        writer.newLine();
                    }
                    log("Results saved to results.txt");
                } catch (IOException ioException) {
                    log("Failed to write results.txt: " + ioException.getMessage());
                }

                log("\nAll steps complete.");

                SwingUtilities.invokeLater(() -> {
                    desiredModel.clear();
                    blockedModel.clear();
                    rankComboBox.setSelectedIndex(0);
                    filterOutRest.setSelected(false);
                    startButton.setEnabled(true);
                });
            }).start();
        });

        JToggleButton themeToggle = new JToggleButton("🌑 Theme", true);
        themeToggle.addActionListener(e -> {
            isDarkMode = !isDarkMode;
            try {
                if (isDarkMode) {
                    UIManager.setLookAndFeel(new FlatDarkLaf());
                } else {
                    UIManager.setLookAndFeel(new FlatLightLaf());
                }
                SwingUtilities.updateComponentTreeUI(frame);
                updateConsoleColors();  // Update console colors on theme change
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        JPanel content = new JPanel(new BorderLayout(10, 10));
        content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel listPanel = new JPanel(new GridLayout(1, 3, 10, 10));
        listPanel.add(makeListPanel("Cape Selection", BoxLayout.Y_AXIS, capeSelector, addToDesired, addToBlocked, filterOutRest));
        listPanel.add(makeListPanel("✅ Desired Capes", desiredList));
        listPanel.add(makeListPanel("❌ Blocked Capes", blockedList));

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(listPanel, BorderLayout.CENTER);

        JPanel rankPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        rankPanel.setBorder(new TitledBorder("Hypixel Rank Filter (optional)"));
        rankPanel.add(new JLabel("Select Rank:"));
        rankPanel.add(rankComboBox);

        topPanel.add(rankPanel, BorderLayout.EAST);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actionPanel.add(startButton);
        actionPanel.add(themeToggle);

        content.add(topPanel, BorderLayout.CENTER);
        content.add(actionPanel, BorderLayout.NORTH);
        content.add(consoleScroll, BorderLayout.SOUTH);

        frame.setContentPane(content);
        updateButtons.run();

        updateConsoleColors(); // Initial console colors for dark theme

        frame.setVisible(true);
    }

    private static void updateConsoleColors() {
        consoleArea.setBackground(UIManager.getColor("TextArea.background"));
        consoleArea.setForeground(UIManager.getColor("TextArea.foreground"));
        consoleArea.setCaretColor(UIManager.getColor("TextArea.caretForeground"));
    }

    private static JPanel makeListPanel(String title, int layout, JComponent... components) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, layout));
        panel.setBorder(BorderFactory.createTitledBorder(title));
        for (JComponent c : components) {
            panel.add(c);
            panel.add(Box.createVerticalStrut(5));
        }
        return panel;
    }

    private static JPanel makeListPanel(String title, JList<?> list) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(title));
        panel.add(new JScrollPane(list), BorderLayout.CENTER);
        return panel;
    }

    private static EnumSet<CapeType> toEnumSet(DefaultListModel<CapeType> model) {
        EnumSet<CapeType> set = EnumSet.noneOf(CapeType.class);
        for (Enumeration<CapeType> e = model.elements(); e.hasMoreElements();) {
            set.add(e.nextElement());
        }
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
        } catch (Exception e) {
            System.err.println("Failed to initialize FlatLaf");
        }
        SwingUtilities.invokeLater(Main::new);
    }
}
