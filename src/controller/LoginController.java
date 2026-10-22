package controller;

import io.github.palexdev.materialfx.controls.MFXPasswordField;
import io.github.palexdev.materialfx.controls.MFXTextField;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import model.User;
import service.AuthService;
import service.PasswordRecoveryService;
import util.SessionContext;
import util.StyledDialogs;

import java.io.IOException;
import java.util.function.Function;

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
    private final PasswordRecoveryService passwordRecoveryService = new PasswordRecoveryService();

    @FXML private MFXTextField tfUsername;
    @FXML private MFXPasswordField pfPassword;
    @FXML private javafx.scene.control.Hyperlink linkCreateAccount;
    @FXML private javafx.scene.control.Hyperlink linkForgotPassword;
    @FXML private javafx.scene.control.Button btnMinimize;
    @FXML private javafx.scene.control.Button btnExit;
    @FXML private Label lblUsernameError;
    @FXML private Label lblPasswordError;

    /** Construtor padrão da classe do controlador. */
    public LoginController() {
    }

    /** Inicializa o evento do link para criar uma nova conta. */
    @FXML
    private void initialize() {
        bindValidation(tfUsername, lblUsernameError,
                value -> value == null || value.isBlank() ? "Introduza o username ou e-mail." : null);
        bindValidation(pfPassword, lblPasswordError,
                value -> value == null || value.isBlank() ? "Introduza a palavra-passe." : null);
        linkCreateAccount.setOnAction(this::createAccount);
        linkForgotPassword.setOnAction(event -> showPasswordRecoveryInfo());
        btnMinimize.setOnAction(event -> windowFrom(event.getSource()).setIconified(true));
        btnExit.setOnAction(event -> windowFrom(event.getSource()).close());
    }

    /** Mostra a informação de recuperação usando o tema centralizado dos diálogos. */
    private void showPasswordRecoveryInfo() {
        TextInputDialog emailDialog = new TextInputDialog(
                tfUsername.getText() != null && tfUsername.getText().contains("@") ? tfUsername.getText() : "");
        emailDialog.setTitle("Recuperar senha");
        emailDialog.setHeaderText("Receber código por email");
        emailDialog.setContentText("Email cadastrado:");
        StyledDialogs.showAndWait(emailDialog).ifPresent(email -> {
            try {
                passwordRecoveryService.sendCode(email);
                TextInputDialog codeDialog = new TextInputDialog();
                codeDialog.setTitle("Verificar código");
                codeDialog.setHeaderText("Enviámos um código de 6 dígitos para o seu email");
                codeDialog.setContentText("Código:");
                StyledDialogs.showAndWait(codeDialog).ifPresent(code -> promptNewPassword(email, code));
            } catch (RuntimeException exception) {
                showAlert(Alert.AlertType.ERROR, exception.getMessage());
            }
        });
    }

    private void promptNewPassword(String email, String code) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Criar nova senha");
        dialog.setHeaderText("Código recebido. Defina a sua nova senha.");
        ButtonType save = new ButtonType("Alterar senha", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);
        PasswordField password = new PasswordField();
        PasswordField confirmation = new PasswordField();
        password.setPromptText("Mínimo de 8 caracteres");
        confirmation.setPromptText("Repita a nova senha");
        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.addRow(0, new Label("Nova senha:"), password);
        form.addRow(1, new Label("Confirmar:"), confirmation);
        dialog.getDialogPane().setContent(form);
        StyledDialogs.showAndWait(dialog).filter(save::equals).ifPresent(result -> {
            if (!password.getText().equals(confirmation.getText())) {
                showAlert(Alert.AlertType.ERROR, "As senhas não coincidem.");
                return;
            }
            try {
                passwordRecoveryService.resetPassword(email, code, password.getText());
                showAlert(Alert.AlertType.INFORMATION, "Senha alterada com sucesso. Já pode iniciar sessão.");
            } catch (RuntimeException exception) {
                showAlert(Alert.AlertType.ERROR, exception.getMessage());
            }
        });
    }

    private Stage windowFrom(Object source) {
        return (Stage) ((Node) source).getScene().getWindow();
    }

    /**
     * Autentica o utilizador com o username e palavra-passe fornecidos.
     *
     * @param actionEvent Evento desencadeado pelo botão de login.
     */
    @FXML
    public void btnEntrar(ActionEvent actionEvent) {
        boolean usernameValid = validateField(tfUsername, lblUsernameError, value ->
                value == null || value.isBlank() ? "Introduza o username ou e-mail." : null, true);
        boolean passwordValid = validateField(pfPassword, lblPasswordError, value ->
                value == null || value.isBlank() ? "Introduza a palavra-passe." : null, true);
        if (!usernameValid || !passwordValid) {
            return;
        }
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

    private void bindValidation(TextInputControl field, Label errorLabel,
                                Function<String, String> validator) {
        if (field == null || errorLabel == null) {
            return;
        }
        field.textProperty().addListener((observable, oldValue, newValue) -> {
            field.getProperties().put("validationTouched", true);
            validateField(field, errorLabel, validator, false);
        });
    }

    private boolean validateField(TextInputControl field, Label errorLabel,
                                  Function<String, String> validator, boolean force) {
        if (force) {
            field.getProperties().put("validationTouched", true);
        }
        if (!force && !Boolean.TRUE.equals(field.getProperties().get("validationTouched"))) {
            return true;
        }
        String error = validator.apply(field.getText());
        boolean valid = error == null;
        errorLabel.setText(valid ? "" : error);
        errorLabel.setVisible(!valid);
        errorLabel.setManaged(!valid);
        field.getStyleClass().remove("field-invalid");
        if (!valid) {
            field.getStyleClass().add("field-invalid");
        }
        return valid;
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
