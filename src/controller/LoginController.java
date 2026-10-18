package controller;


import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Node;
import io.github.palexdev.materialfx.controls.MFXPasswordField;
import io.github.palexdev.materialfx.controls.MFXTextField;
import model.User;
import service.AuthService;
import util.SessionContext;

import java.io.IOException;


public class LoginController {
    private final AuthService authService = new AuthService();
    @FXML
    private MFXTextField tfUsername;
    @FXML
    private MFXPasswordField pfPassword;


    public LoginController() {
      //  this.as = new AuthService();

    }

    @FXML
    public void btnEntrar(ActionEvent actionEvent) {

        String username = tfUsername.getText();
        String password = pfPassword.getText();

        try {
            User user = authService.login(username, password);
            SessionContext.setCurrentUser(user);
            Parent main = FXMLLoader.load(getClass().getResource("/view/fxml/main-layout.fxml"));
            ((Node) actionEvent.getSource()).getScene().setRoot(main);
        } catch (IOException exception) {
            showAlert(Alert.AlertType.ERROR, "Não foi possível abrir a aplicação: " + exception.getMessage());
        } catch (RuntimeException exception) {
            showAlert(Alert.AlertType.ERROR, exception.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(type == Alert.AlertType.ERROR ? "Falha no login" : "Login");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void createAccount(ActionEvent actionEvent) {
        try {
            Parent register = FXMLLoader.load(getClass().getResource("/view/fxml/register_step1.fxml"));
            ((Node) actionEvent.getSource()).getScene().setRoot(register);
        } catch (IOException exception) {
            showAlert(Alert.AlertType.ERROR, "Não foi possível abrir o registo: " + exception.getMessage());
        }
    }
//    txtUsername.textProperty().addListener(
//        (observable, valorAntigo, valorNovo) -> {
//
//        boolean valido =
//                valorNovo != null
//                        && valorNovo.matches("[a-zA-Z0-9_]{4,20}");
//
//        txtUsername.setStyle(
//                valido
//                        ? ""
//                        : "-fx-border-color: red;"
//        );
//    }
//)

    public void Logar(ActionEvent actionEvent) {

    }
}
