package lol.moruto.scraper;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.*;
import java.util.EnumSet;
import java.util.Set;

public class Main extends Application {

    private final ObservableList<CapeType> desiredCapes = FXCollections.observableArrayList();
    private final ObservableList<CapeType> blockedCapes = FXCollections.observableArrayList();

    private TextArea consoleArea; // TextArea to display console logs

    @Override
    public void start(Stage stage) {
        stage.setTitle("capes.me Scraper");

        // Console log area
        consoleArea = new TextArea();
        consoleArea.setEditable(false);
        consoleArea.setWrapText(true);
        consoleArea.setStyle("-fx-font-family: Consolas, monospace; -fx-font-size: 14px;");
        consoleArea.setPrefHeight(150);

        // ComboBox for selecting capes
        ComboBox<CapeType> capeSelector = new ComboBox<>(FXCollections.observableArrayList(CapeType.values()));
        capeSelector.setPromptText("Select a Cape");
        capeSelector.setStyle("-fx-font-size: 14px;");

        // Buttons to add to desired or blocked lists
        Button addToDesired = new Button("Add to Desired");
        Button addToBlocked = new Button("Add to Filter Out");
        addToDesired.setStyle("-fx-font-size: 14px; -fx-padding: 8px 12px;");
        addToBlocked.setStyle("-fx-font-size: 14px; -fx-padding: 8px 12px;");

        // Tooltips for the buttons
        addToDesired.setTooltip(new Tooltip("Add selected cape to Desired list"));
        addToBlocked.setTooltip(new Tooltip("Add selected cape to Filter Out list"));

        // ListViews to display the desired and blocked capes
        ListView<CapeType> desiredList = new ListView<>(desiredCapes);
        ListView<CapeType> blockedList = new ListView<>(blockedCapes);
        desiredList.setStyle("-fx-font-size: 14px;");
        blockedList.setStyle("-fx-font-size: 14px;");

        // Set placeholders for the lists
        desiredList.setPlaceholder(new Label("No desired capes"));
        blockedList.setPlaceholder(new Label("No filtered out capes"));

        // Labels for each section
        Label desiredLabel = new Label("Desired Capes");
        desiredLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        Label blockedLabel = new Label("Filtered Out Capes");
        blockedLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        // Buttons for adding capes to the lists
        addToDesired.setOnAction(e -> {
            CapeType selected = capeSelector.getValue();
            if (selected != null) {
                if (blockedCapes.contains(selected)) {
                    showAlert(Alert.AlertType.WARNING, "Cape already added to Filter Out list.");
                } else if (!desiredCapes.contains(selected)) {
                    desiredCapes.add(selected);
                }
            }
            updateButtons(capeSelector, addToDesired, addToBlocked);
        });

        addToBlocked.setOnAction(e -> {
            CapeType selected = capeSelector.getValue();
            if (selected != null) {
                if (desiredCapes.contains(selected)) {
                    showAlert(Alert.AlertType.WARNING, "Cape already added to Desired list.");
                } else if (!blockedCapes.contains(selected)) {
                    blockedCapes.add(selected);
                }
            }
            updateButtons(capeSelector, addToDesired, addToBlocked);
        });

        // Disable buttons when necessary
        capeSelector.setOnAction(e -> updateButtons(capeSelector, addToDesired, addToBlocked));

        // Layout for adding capes and selecting
        VBox selectBox = new VBox(10, capeSelector, addToDesired, addToBlocked);
        selectBox.setPadding(new Insets(10));
        selectBox.setStyle("-fx-background-color: #f0f0f0; -fx-padding: 15px; -fx-border-radius: 8px;");

        // Layout for displaying desired and blocked lists
        VBox desiredBox = new VBox(5, desiredLabel, desiredList);
        VBox blockedBox = new VBox(5, blockedLabel, blockedList);
        desiredBox.setStyle("-fx-background-color: #e6f7ff; -fx-padding: 10px; -fx-border-radius: 8px;");
        blockedBox.setStyle("-fx-background-color: #f8d7da; -fx-padding: 10px; -fx-border-radius: 8px;");

        // Main layout with the lists
        HBox capeBoxes = new HBox(20, desiredBox, blockedBox);
        capeBoxes.setPadding(new Insets(10));

        // Start Scraping button
        Button startButton = new Button("Start Scraping");
        startButton.setStyle("-fx-font-size: 14px; -fx-padding: 10px;");
        startButton.setOnAction(e -> {
            if (desiredCapes.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Please add at least one desired cape.");
                return;
            }

            Set<CapeType> desiredSet = EnumSet.copyOf(desiredCapes);
            Set<CapeType> blockedSet = blockedCapes.isEmpty()
                    ? EnumSet.noneOf(CapeType.class)
                    : EnumSet.copyOf(blockedCapes);

            // Pass consoleArea to ListCapes to log the output to the GUI
            new Thread(() -> {
                showAlertAsync("Scraping started. Check console and 'results.txt'.");
                new ListCapes(desiredSet, blockedSet, consoleArea);
            }).start();
        });

        // Console area
        VBox consoleBox = new VBox(new Label("Console Output:"), consoleArea);
        consoleBox.setStyle("-fx-background-color: #eeeeee; -fx-padding: 10px; -fx-border-radius: 8px;");

        // Final layout
        VBox root = new VBox(20, selectBox, capeBoxes, startButton, consoleBox);
        root.setPadding(new Insets(15));
        root.setStyle("-fx-background-color: #ffffff;");

        Scene scene = new Scene(root, 600, 600);
        scene.getStylesheets().add("styles.css");  // Add external styles if needed
        stage.setScene(scene);
        stage.show();
    }

    private void updateButtons(ComboBox<CapeType> capeSelector, Button addToDesired, Button addToBlocked) {
        CapeType selected = capeSelector.getValue();
        if (selected == null) return;

        addToDesired.setDisable(blockedCapes.contains(selected) || desiredCapes.contains(selected));
        addToBlocked.setDisable(desiredCapes.contains(selected) || blockedCapes.contains(selected));
    }

    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type, message, ButtonType.OK);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private void showAlertAsync(String message) {
        javafx.application.Platform.runLater(() -> showAlert(Alert.AlertType.INFORMATION, message));
    }

    public static void main(String[] args) {
        launch(args);
    }
}
