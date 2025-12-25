package com.sadiqstore.controller;

import com.sadiqstore.App; // <--- Import App to use setRoot
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import java.io.IOException; // <--- Import for exceptions

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Label errorLabel;

    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.equals("admin") && password.equals("admin")) {
            System.out.println("Login Successful!");
            
            try {
                // THIS LINE SWITCHES THE SCREEN TO DASHBOARD
                App.setRoot("Dashboard");
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else {
            errorLabel.setText("Invalid Username or Password");
            errorLabel.setStyle("-fx-text-fill: red;");
            errorLabel.setVisible(true);
        }
    }
}