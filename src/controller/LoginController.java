package controller;


import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import service.AuthService;


public class LoginController {
    AuthService as;
    @FXML
    private TextField tfUsername;
    @FXML
    private TextField pfPassword;


    public LoginController() {
      //  this.as = new AuthService();

    }

    @FXML
    public void btnEntrar(ActionEvent actionEvent) {

        String username = tfUsername.getText();
        String password = pfPassword.getText();

        as.login(username,password);
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
