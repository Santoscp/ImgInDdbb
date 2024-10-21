package application;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.nio.file.Files;
import java.sql.*;

public class Main extends Application {

    private ListView<String> listView = new ListView<>(); // Para mostrar los nombres de los productos
    private ImageView imageView = new ImageView(); // Para mostrar la imagen seleccionada
    private Button updateButton = new Button("Actualizar Imagen");

    // Conexión a la base de datos
    private static final String DB_URL = "jdbc:mysql://localhost:3306/inper"; // Cambia por tu base de datos
    private static final String USER = "root"; // Cambia por tu usuario
    private static final String PASSWORD = ""; // Cambia por tu contraseña

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Gestor de Imágenes");

        // Layout para la lista de productos
        VBox listLayout = new VBox(10, new Label("Productos en la Base de Datos"), listView);
        listLayout.setPadding(new Insets(10));
        listView.setPrefWidth(200);

        // Listener para mostrar la imagen seleccionada
        listView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                displayImage(newVal);
            }
        });

        // Layout para actualizar la imagen
        HBox updateLayout = new HBox(10, updateButton);
        updateLayout.setPadding(new Insets(10));
        
        // Evento del botón para actualizar imagen
        updateButton.setOnAction(e -> updateImage());

        // Layout principal
        VBox mainLayout = new VBox(10, listLayout, imageView, updateLayout);
        mainLayout.setPadding(new Insets(20));

        // Escena principal
        Scene scene = new Scene(mainLayout, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.show();

        // Cargar los productos desde la base de datos
        loadProducts();
    }

    // Método para cargar productos desde la base de datos
    private void loadProducts() {
        try (Connection connection = DriverManager.getConnection(DB_URL, USER, PASSWORD);
             Statement statement = connection.createStatement()) {
            String sql = "SELECT nombre FROM producto"; // Cambia a tu consulta según sea necesario
            ResultSet resultSet = statement.executeQuery(sql);

            listView.getItems().clear();
            while (resultSet.next()) {
                listView.getItems().add(resultSet.getString("nombre"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Método para mostrar la imagen seleccionada
    private void displayImage(String productName) {
        try (Connection connection = DriverManager.getConnection(DB_URL, USER, PASSWORD);
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT imagen FROM producto WHERE nombre = ?")) {
            preparedStatement.setString(1, productName);
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                byte[] imageBytes = resultSet.getBytes("imagen");
                if (imageBytes != null) {
                    Image image = new Image(new ByteArrayInputStream(imageBytes));
                    imageView.setImage(image);
                    imageView.setFitWidth(300);  // Ajusta el tamaño de la imagen
                    imageView.setPreserveRatio(true);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Método para actualizar la imagen en la base de datos
    private void updateImage() {
        String selectedProduct = listView.getSelectionModel().getSelectedItem();
        
        if (selectedProduct != null) {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
            File selectedFile = fileChooser.showOpenDialog(null);

            if (selectedFile != null) {
                try (Connection connection = DriverManager.getConnection(DB_URL, USER, PASSWORD);
                     PreparedStatement preparedStatement = connection.prepareStatement("UPDATE producto SET imagen = ? WHERE nombre = ?")) {
                    preparedStatement.setBytes(1, Files.readAllBytes(selectedFile.toPath()));
                    preparedStatement.setString(2, selectedProduct);
                    preparedStatement.executeUpdate();
                    displayImage(selectedProduct); // Actualiza la imagen mostrada
                } catch (SQLException | IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            showAlert("Error", "Por favor, selecciona un producto para actualizar la imagen.");
        }
    }

    // Método para mostrar alertas
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

