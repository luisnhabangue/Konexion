package controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Window;

import model.User;
import model.enums.Gender;
import service.AuthService;

import java.io.IOException;
import java.time.LocalDate;

public class RegisterController {

    private final AuthService authService = new AuthService();

    // Dados pessoais - Passo 1
    @FXML private TextField txtPrimeirosNomes;
    @FXML private TextField txtApelidos;
    @FXML private TextField txtDocumento;
    @FXML private ComboBox<Gender> cmbGenero;
    @FXML private DatePicker dpDataNascimento;
    @FXML private TextField txtTelefone;

    // Credenciais - Passo 2
    @FXML private TextField txtUsername;
    @FXML private TextField txtEmail;
    @FXML private PasswordField txtPassword;
    @FXML private PasswordField txtConfirmPassword;

    // Botões e navegação
    @FXML private javafx.scene.control.Button btnContinuar;
    @FXML private javafx.scene.control.Button btnCriarConta;
    @FXML private javafx.scene.control.Button btnVoltarPasso;
    @FXML private Hyperlink linkVoltarLogin;

    // Dados pessoais guardados durante a navegação
    private String firstName;
    private String lastName;
    private String documentId;
    private Gender gender;
    private LocalDate birthDate;
    private String phone;

    @FXML
    private void initialize() {

        // Inicializar apenas os campos existentes no ecrã atual
        if (cmbGenero != null) {
            cmbGenero.getItems().setAll(
                    Gender.M,
                    Gender.F,
                    Gender.NOT_SPECIFIED
            );
            cmbGenero.setValue(Gender.NOT_SPECIFIED);
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

    // PASSO 1: validar e avançar
    private void continueRegistration(Object source) {

        if (txtPrimeirosNomes.getText().trim().isEmpty()
                || txtApelidos.getText().trim().isEmpty()
                || txtDocumento.getText().trim().isEmpty()
                || txtTelefone.getText().trim().isEmpty()
                || cmbGenero.getValue() == null
                || dpDataNascimento.getValue() == null) {

            showAlert(Alert.AlertType.WARNING,
                    "Preencha todos os dados pessoais antes de continuar.");
            return;
        }

        firstName = txtPrimeirosNomes.getText().trim();
        lastName = txtApelidos.getText().trim();
        documentId = txtDocumento.getText().trim();
        gender = cmbGenero.getValue();
        birthDate = dpDataNascimento.getValue();
        phone = txtTelefone.getText().trim();

        showScreen(source, "/view/fxml/register_step2.fxml");
    }

    // PASSO 2: criar conta
    private void createAccount(Object source) {

        if (txtUsername.getText().trim().isEmpty()
                || txtEmail.getText().trim().isEmpty()
                || txtPassword.getText().isEmpty()
                || txtConfirmPassword.getText().isEmpty()) {

            showAlert(Alert.AlertType.WARNING,
                    "Preencha todos os campos das credenciais.");
            return;
        }

        if (!txtPassword.getText().equals(txtConfirmPassword.getText())) {
            showAlert(Alert.AlertType.ERROR,
                    "As senhas não coincidem.");
            return;
        }

        try {
            /*
             * IMPORTANTE:
             * Este construtor deve corresponder ao construtor real
             * da tua classe User.
             */
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
                    "A conta foi criada com sucesso. Entre com o seu username e senha.");

            showLogin(source);

        } catch (RuntimeException e) {
            showAlert(Alert.AlertType.ERROR, e.getMessage());
        }
    }

    // VOLTAR DO PASSO 2 PARA O PASSO 1
    private void showPreviousStep(Object source) {

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/view/fxml/register_step1.fxml")
            );

            Parent root = loader.load();

            RegisterController controller = loader.getController();

            // Restaurar os dados anteriormente preenchidos
            controller.txtPrimeirosNomes.setText(firstName);
            controller.txtApelidos.setText(lastName);
            controller.txtDocumento.setText(documentId);
            controller.cmbGenero.setValue(gender);
            controller.dpDataNascimento.setValue(birthDate);
            controller.txtTelefone.setText(phone);

            ((Node) source).getScene().setRoot(root);

        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR,
                    "Não foi possível voltar ao passo anterior: " + e.getMessage());
        }
    }

    // ABRIR O LOGIN
    private void showLogin(Object source) {
        showScreen(source, "/view/fxml/LoginView.fxml");
    }

    // MÉTODO AUXILIAR PARA MUDAR DE ECRÃ
    private void showScreen(Object source, String fxmlPath) {

        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource(fxmlPath)
            );

            ((Node) source).getScene().setRoot(root);

        } catch (IOException | NullPointerException e) {
            showAlert(Alert.AlertType.ERROR,
                    "Não foi possível abrir o ecrã: " + fxmlPath
                            + "\n" + e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String message) {
        new Alert(type, message == null ? "Ocorreu um erro inesperado." : message)
                .showAndWait();
    }
}
