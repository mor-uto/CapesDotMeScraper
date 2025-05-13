package lol.moruto.scraper;

import javafx.application.Application;
import javafx.collections.*;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.EnumSet;
import java.util.Set;

public class Main extends Application {

    private final ObservableList<CapeType> desiredCapes = FXCollections.observableArrayList();
    private final ObservableList<CapeType> blockedCapes = FXCollections.observableArrayList();
    private final ObservableList<CapeType> autoFilteredCapes = FXCollections.observableArrayList();

    private TextArea consoleArea;
    private boolean isDarkMode = true;

    @Override
    public void start(Stage stage) {
        stage.setTitle("capes.me Scraper");

        ComboBox<CapeType> capeSelector = new ComboBox<>(FXCollections.observableArrayList(CapeType.values()));
        capeSelector.setPromptText("Select a Cape");

        Button addToDesired = new Button("Add to Desired");
        Button addToBlocked = new Button("Add to Filter Out");

        ListView<CapeType> desiredList = new ListView<>(desiredCapes);
        ListView<CapeType> blockedList = new ListView<>(blockedCapes);

        Label desiredLabel = new Label("✅ Desired Capes");
        Label blockedLabel = new Label("🚫 Filtered Out Capes");

        desiredList.setPlaceholder(new Label("No desired capes"));
        blockedList.setPlaceholder(new Label("No filtered out capes"));

        CheckBox filterOutRestCheckbox = new CheckBox("Filter Out All Other Capes");
        filterOutRestCheckbox.setOnAction(e -> updateAutoFilteredCapes());
        desiredCapes.addListener((ListChangeListener<CapeType>) c -> {
            if (filterOutRestCheckbox.isSelected()) updateAutoFilteredCapes();
        });

        consoleArea = new TextArea();
        consoleArea.setEditable(false);
        consoleArea.setWrapText(true);
        consoleArea.setPrefHeight(300);
        consoleArea.setStyle("-fx-font-family: 'Consolas';");

        addToDesired.setOnAction(e -> {
            CapeType selected = capeSelector.getValue();
            if (selected != null && !desiredCapes.contains(selected) && !blockedCapes.contains(selected)) {
                desiredCapes.add(selected);
            }
            updateButtons(capeSelector, addToDesired, addToBlocked);
        });

        addToBlocked.setOnAction(e -> {
            CapeType selected = capeSelector.getValue();
            if (selected != null && !blockedCapes.contains(selected) && !desiredCapes.contains(selected)) {
                blockedCapes.add(selected);
            }
            updateButtons(capeSelector, addToDesired, addToBlocked);
        });

        capeSelector.setOnAction(e -> updateButtons(capeSelector, addToDesired, addToBlocked));

        Button startButton = new Button("🚀 Start Scraping");
        startButton.setOnAction(e -> {
            if (desiredCapes.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Please select at least one desired cape.");
                return;
            }

            Set<CapeType> desiredSet = EnumSet.copyOf(desiredCapes);
            Set<CapeType> blockedSet = blockedCapes.isEmpty() ? EnumSet.noneOf(CapeType.class) : EnumSet.copyOf(blockedCapes);

            new Thread(() -> new ListCapes(desiredSet, blockedSet, consoleArea)).start();
        });

        ToggleButton themeToggleButton = new ToggleButton("🌙 Light/Dark Mode");
        themeToggleButton.setOnAction(e -> {
            isDarkMode = !isDarkMode;
            if (isDarkMode) {
                stage.getScene().getStylesheets().clear();
                stage.getScene().getStylesheets().add("dark-theme.css");
            } else {
                stage.getScene().getStylesheets().clear();
                stage.getScene().getStylesheets().add("light-theme.css");
            }
        });

        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(15);
        grid.setPadding(new Insets(15));

        VBox selectBox = new VBox(10, capeSelector, addToDesired, addToBlocked, filterOutRestCheckbox);
        VBox desiredBox = new VBox(10, desiredLabel, desiredList);
        VBox blockedBox = new VBox(10, blockedLabel, blockedList);

        selectBox.getStyleClass().add("section");
        desiredBox.getStyleClass().add("section-blue");
        blockedBox.getStyleClass().add("section-red");

        grid.add(selectBox, 0, 0);
        grid.add(desiredBox, 1, 0);
        grid.add(blockedBox, 2, 0);
        grid.add(startButton, 0, 1);
        grid.add(consoleArea, 0, 2, 3, 1);
        grid.add(themeToggleButton, 2, 1);

        Scene scene = new Scene(grid, 800, 600);
        scene.getStylesheets().add("dark-theme.css");
        stage.setScene(scene);
        stage.show();
    }

    private void updateAutoFilteredCapes() {
        blockedCapes.removeAll(autoFilteredCapes);
        autoFilteredCapes.clear();

        if (!desiredCapes.isEmpty()) {
            for (CapeType cape : CapeType.values()) {
                if (!desiredCapes.contains(cape) && !blockedCapes.contains(cape)) {
                    blockedCapes.add(cape);
                    autoFilteredCapes.add(cape);
                }
            }
        }
    }

    private void updateButtons(ComboBox<CapeType> capeSelector, Button addToDesired, Button addToBlocked) {
        CapeType selected = capeSelector.getValue();
        if (selected == null) return;

        addToDesired.setDisable(desiredCapes.contains(selected) || blockedCapes.contains(selected));
        addToBlocked.setDisable(blockedCapes.contains(selected) || desiredCapes.contains(selected));
    }

    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type, message, ButtonType.OK);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
