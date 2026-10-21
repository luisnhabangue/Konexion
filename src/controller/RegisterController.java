package controller;

import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXPasswordField;
import io.github.palexdev.materialfx.controls.MFXTextField;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Hyperlink;
import model.User;
import model.enums.Gender;
import service.AuthService;
import util.StyledDialogs;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
/**
 * Controlador JavaFX responsável por apresentar e coordenar as interações da funcionalidade correspondente.
 *
 * @since 1.0
 */

public class RegisterController {

    private final AuthService authService = new AuthService();

    @FXML private MFXTextField txtPrimeirosNomes;
    @FXML private MFXTextField txtApelidos;
    @FXML private MFXTextField txtDocumento;
    @FXML private ComboBox<Gender> cmbGenero;
    @FXML private DatePicker dpDataNascimento;
    @FXML private MFXTextField txtTelefone;

    @FXML private MFXTextField txtUsername;
    @FXML private MFXTextField txtEmail;
    @FXML private MFXPasswordField txtPassword;
    @FXML private MFXPasswordField txtConfirmPassword;

    @FXML private MFXButton btnContinuar;
    @FXML private MFXButton btnCriarConta;
    @FXML private MFXButton btnVoltarPasso;
    @FXML private Hyperlink linkVoltarLogin;

    private String firstName;
    private String lastName;
    private String documentId;
    private Gender gender;
    private LocalDate birthDate;
    private String phone;

    @FXML
    private void initialize() {
        if (cmbGenero != null) {
            cmbGenero.getItems().setAll(
                    Gender.M,
                    Gender.F
            );
        }

        if (btnContinuar != null) {
            btnContinuar.setOnAction(event -> continueRegistration(event.getSource()));
        }

        if (btnCriarConta != null) {
            btnCriarConta.setOnAction(event -> createAccount(event.getSource()));
        }

        if (btnVoltarPasso != null) {
            btnVoltarPasso.setOnAction(event -> showPreviousStep(event.getSource()));
        }

        if (linkVoltarLogin != null) {
            linkVoltarLogin.setOnAction(event -> showLogin(event.getSource()));
        }
    }

    private void continueRegistration(Object source) {
        if (isBlank(txtPrimeirosNomes.getText())
                || isBlank(txtApelidos.getText())
                || isBlank(txtTelefone.getText())
                || cmbGenero.getValue() == null
                || dpDataNascimento.getValue() == null) {
            showAlert(Alert.AlertType.WARNING,
                    "Preencha os dados pessoais obrigatórios antes de continuar. O BI/NUIT pode ser enviado depois, na verificação KYC.");
            return;
        }

        firstName = txtPrimeirosNomes.getText().trim();
        lastName = txtApelidos.getText().trim();
        documentId = txtDocumento.getText() == null || txtDocumento.getText().isBlank()
                ? null : txtDocumento.getText().trim();
        gender = cmbGenero.getValue();
        birthDate = dpDataNascimento.getValue();
        phone = txtTelefone.getText().trim();

        showStep(source, "/view/fxml/register_step2.fxml");
    }

    private void createAccount(Object source) {
        if (isBlank(txtUsername.getText())
                || isBlank(txtEmail.getText())
                || isBlank(txtPassword.getText())
                || isBlank(txtConfirmPassword.getText())) {
            showAlert(Alert.AlertType.WARNING,
                    "Preencha todos os campos das credenciais.");
            return;
        }

        if (!txtPassword.getText().equals(txtConfirmPassword.getText())) {
            showAlert(Alert.AlertType.ERROR, "As senhas não coincidem.");
            return;
        }

        try {
            User user = new User(
                    null,
                    firstName,
                    lastName,
                    documentId,
                    gender,
                    txtUsername.getText().trim().toLowerCase(),
                    txtPassword.getText(),
                    txtEmail.getText().trim(),
                    phone
            );

            authService.register(user, txtConfirmPassword.getText());
            showAlert(Alert.AlertType.INFORMATION,
                    "A conta foi criada com sucesso e está por verificar. Pode participar em grupos; para criar grupos, submeta o BI/NUIT e as fotografias para aprovação do administrador.");
            showLogin(source);
        } catch (RuntimeException exception) {
            showAlert(Alert.AlertType.ERROR, exception.getMessage());
        }
    }

    private void showPreviousStep(Object source) {
        showStep(source, "/view/fxml/register_step1.fxml");
    }

    private void showLogin(Object source) {
        showStep(source, "/view/fxml/LoginView.fxml");
    }

    private void showStep(Object source, String fxmlPath) {
        URL fxmlUrl = getClass().getResource(fxmlPath);
        if (fxmlUrl == null) {
            showAlert(Alert.AlertType.ERROR, "Não foi possível encontrar o ecrã: " + fxmlPath);
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();
            Object loadedController = loader.getController();
            if (loadedController instanceof RegisterController target) {
                target.restorePersonalData(this);
            }
            ((Node) source).getScene().setRoot(root);
        } catch (IOException exception) {
            showAlert(Alert.AlertType.ERROR,
                    "Não foi possível abrir o ecrã: " + fxmlPath + "\n" + exception.getMessage());
        }
    }

    private void restorePersonalData(RegisterController source) {
        firstName = source.firstName;
        lastName = source.lastName;
        documentId = source.documentId;
        gender = source.gender;
        birthDate = source.birthDate;
        phone = source.phone;

        if (txtPrimeirosNomes != null && firstName != null) {
            txtPrimeirosNomes.setText(firstName);
            txtApelidos.setText(lastName);
            txtDocumento.setText(documentId);
            cmbGenero.setValue(gender);
            dpDataNascimento.setValue(birthDate);
            txtTelefone.setText(phone);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void showAlert(Alert.AlertType type, String message) {
        StyledDialogs.showAndWait(new Alert(type, message == null
                ? "Ocorreu um erro inesperado." : message));
    }
}
