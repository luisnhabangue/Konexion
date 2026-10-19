package controller;

import io.github.palexdev.materialfx.controls.MFXPasswordField;
import io.github.palexdev.materialfx.controls.MFXTextField;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import model.User;
import service.AuthService;
import util.SessionContext;
import util.StyledDialogs;

import java.io.IOException;

/**
 * Controlador JavaFX do ecrã de início de sessão (Login) da aplicação.
 * <p>
 * Valida as credenciais do utilizador junto do {@link AuthService}, define a sessão ativa
 * e navega para o layout principal ou ecrã de registo.
 * </p>

 * @since 1.0
 */
public class LoginController {

    /** Serviço responsável pela autenticação e gestão de utilizadores. */
    private final AuthService authService = new AuthService();

    @FXML private MFXTextField tfUsername;
    @FXML private MFXPasswordField pfPassword;
    @FXML private javafx.scene.control.Hyperlink linkCreateAccount;

    /** Construtor padrão da classe do controlador. */
    public LoginController() {
    }

    /** Inicializa o evento do link para criar uma nova conta. */
    @FXML
    private void initialize() {
        linkCreateAccount.setOnAction(this::createAccount);
    }

    /**
     * Autentica o utilizador com o username e palavra-passe fornecidos.
     *
     * @param actionEvent Evento desencadeado pelo botão de login.
     */
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

    /** Apresenta um diálogo de alerta de erro ou aviso na autenticação. */
    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(type == Alert.AlertType.ERROR ? "Falha no login" : "Login");
        alert.setHeaderText(null);
        alert.setContentText(message);
        StyledDialogs.showAndWait(alert);
    }

    /**
     * Redireciona a vista para a primeira etapa do processo de registo de nova conta.
     *
     * @param actionEvent Evento acionado pelo hyperlink.
     */
    @FXML
    public void createAccount(ActionEvent actionEvent) {
        try {
            java.net.URL registerUrl = getClass().getResource("/view/fxml/register_step1.fxml");
            if (registerUrl == null) {
                throw new IOException("Não foi possível encontrar a tela de registo.");
            }
            Parent register = FXMLLoader.load(registerUrl);
            ((Node) actionEvent.getSource()).getScene().setRoot(register);
        } catch (IOException exception) {
            showAlert(Alert.AlertType.ERROR, "Não foi possível abrir o registo: " + exception.getMessage());
        }
    }

    /** Método utilitário alternativo para autenticação. */
    public void Logar(ActionEvent actionEvent) {
    }
}