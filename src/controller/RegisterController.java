package controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import model.User;
import model.enums.Gender;
import service.AuthService;

import java.io.IOException;

public class RegisterController {
    private final AuthService authService = new AuthService();

    @FXML private TextField txtFirstName;
    @FXML private TextField txtLastName;
    @FXML private TextField txtDocumentId;
    @FXML private ComboBox<Gender> cmbGender;
    @FXML private TextField txtUsername;
    @FXML private TextField txtEmail;
    @FXML private TextField txtPhone;
    @FXML private PasswordField txtPassword;
    @FXML private PasswordField txtConfirmPassword;
    @FXML private javafx.scene.control.Button btnRegister;
    @FXML private Hyperlink btnBack;

    @FXML
    private void initialize() {
        cmbGender.getItems().setAll(Gender.M, Gender.F, Gender.NOT_SPECIFIED);
        cmbGender.setValue(Gender.NOT_SPECIFIED);
        btnRegister.setOnAction(event -> register(event.getSource()));
        btnBack.setOnAction(event -> showLogin(event.getSource()));
    }

    private void register(Object source) {
        try {
            User user = new User(null, txtFirstName.getText().trim(), txtLastName.getText().trim(),
                    txtDocumentId.getText().trim(), cmbGender.getValue(),
                    txtUsername.getText().trim().toLowerCase(), txtPassword.getText(),
                    txtEmail.getText().trim(), txtPhone.getText().trim());
            authService.register(user, txtConfirmPassword.getText());
            new Alert(Alert.AlertType.INFORMATION,
                    "A conta foi criada. Entre com o username e a palavra-passe escolhida.").showAndWait();
            showLogin(source);
        } catch (RuntimeException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }

    private void showLogin(Object source) {
        try {
            Parent login = FXMLLoader.load(getClass().getResource("/view/fxml/LoginView.fxml"));
            ((Node) source).getScene().setRoot(login);
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "Não foi possível voltar ao login: " + e.getMessage()).showAndWait();
        }
    }
}
