package usace.hec.ui;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.util.List;
import java.util.Map;

public class ExpressionNodeExplorer extends Application {
    public static void main(String[] args) {
        // This line starts the JavaFX application
        Application.launch(ExpressionNodeExplorer.class, args);
    }
    @Override
    public void start(Stage primaryStage) {
        // 1. Discover nodes using the reflection scanner
        List<Map<String, String>> rawNodes = ExpressionNodeConsoleApp.discoverNodes();
        ObservableList<Map<String, String>> data = FXCollections.observableArrayList(rawNodes);
        FilteredList<Map<String, String>> filteredData = new FilteredList<>(data, p -> true);

        // 2. Build UI Controls
        TextField searchField = new TextField();
        searchField.setPromptText("Filter by name, operator, or category...");
        searchField.setPrefWidth(300);

        TableView<Map<String, String>> table = new TableView<>();
        table.setPrefHeight(400);

        // Table Columns
        TableColumn<Map<String, String>, String> catCol = new TableColumn<>("Category");
        catCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().get("category")));
        catCol.setPrefWidth(100);

        TableColumn<Map<String, String>, String> nameCol = new TableColumn<>("Class");
        nameCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().get("simpleName")));
        nameCol.setPrefWidth(160);

        TableColumn<Map<String, String>, String> opCol = new TableColumn<>("Operator");
        opCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().get("opName")));
        opCol.setPrefWidth(120);

        TableColumn<Map<String, String>, String> infixCol = new TableColumn<>("Infix");
        infixCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().get("infixName")));
        infixCol.setPrefWidth(80);

        TableColumn<Map<String, String>, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().get("type")));
        typeCol.setPrefWidth(100);

        table.getColumns().addAll(catCol, nameCol, opCol, infixCol, typeCol);
        table.setItems(filteredData);

        // Search Filtering
        searchField.textProperty().addListener((obs, old, newVal) -> {
            String lower = newVal.toLowerCase();
            filteredData.setPredicate(node -> 
                node.get("simpleName").toLowerCase().contains(lower) ||
                node.get("opName").toLowerCase().contains(lower) ||
                node.get("category").toLowerCase().contains(lower) ||
                node.get("infixName").toLowerCase().contains(lower)
            );
        });

        // 3. Layout
        VBox controls = new VBox(10, new Label("ExpressionNode Explorer"), searchField);
        controls.setPadding(new Insets(10));
        controls.setStyle("-fx-background-color: #f8f9fa;");

        BorderPane root = new BorderPane();
        root.setTop(controls);
        root.setCenter(table);
        root.setPadding(new Insets(10));

        Scene scene = new Scene(root, 650, 450);
        primaryStage.setTitle("HEC ExpressionNode Explorer");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        // Launches the JavaFX stage on the FX Application Thread
        Application.launch(ExpressionNodeExplorer.class, args);
    }
}