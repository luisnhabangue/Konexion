package controller;


import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import io.github.palexdev.materialfx.controls.MFXPasswordField;
import io.github.palexdev.materialfx.controls.MFXTextField;
import service.AuthService;


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
            authService.login(username, password);
            showAlert(Alert.AlertType.INFORMATION, "Login realizado com sucesso.");
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
