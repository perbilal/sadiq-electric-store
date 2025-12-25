package com.sadiqstore;

import com.sadiqstore.util.DatabaseHandler;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class App extends Application {

    private static Scene scene;

    @Override
    public void start(Stage stage) throws IOException {
        // 1. Initialize Database
        DatabaseHandler.initDB();

        // 2. Load the Login FXML
        scene = new Scene(loadFXML("Login"), 640, 480);
        
        stage.setScene(scene);
        stage.setTitle("Sadiq Electric Store - Login");
        stage.show();
    }

    // Helper method to switch views easily later
   public static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("/view/" + fxml + ".fxml"));
        return fxmlLoader.load();
    }

    public static void main(String[] args) {
        launch();
    }
}