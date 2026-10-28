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
import javafx.scene.control.DateCell;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputControl;
import model.User;
import model.enums.Gender;
import service.AuthService;
import util.StyledDialogs;
import validation.UserValidator;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.util.Locale;
import java.util.function.Function;
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
    @FXML private CheckBox chkAcceptTerms;
    @FXML private Hyperlink linkTerms;
    @FXML private Hyperlink linkVoltarLogin;
    @FXML private Label lblFirstNameError;
    @FXML private Label lblLastNameError;
    @FXML private Label lblDocumentError;
    @FXML private Label lblBirthDateError;
    @FXML private Label lblPhoneError;
    @FXML private Label lblUsernameError;
    @FXML private Label lblEmailError;
    @FXML private Label lblNewPasswordError;
    @FXML private Label lblConfirmPasswordError;

    private String firstName;
    private String lastName;
    private String documentId;
    private Gender gender;
    private LocalDate birthDate;
    private String phone;

    @FXML
    private void initialize() {
        bindValidation(txtPrimeirosNomes, lblFirstNameError, this::validateName);
        bindValidation(txtApelidos, lblLastNameError, this::validateLastName);
        bindValidation(txtDocumento, lblDocumentError, this::validateDocument);
        bindValidation(txtTelefone, lblPhoneError, this::validatePhone);
        bindValidation(txtUsername, lblUsernameError, this::validateUsername);
        bindValidation(txtEmail, lblEmailError, this::validateEmail);
        bindValidation(txtPassword, lblNewPasswordError, this::validatePassword);
        bindValidation(txtConfirmPassword, lblConfirmPasswordError, this::validatePasswordConfirmation);
        if (txtPassword != null && txtConfirmPassword != null && lblConfirmPasswordError != null) {
            txtPassword.textProperty().addListener((observable, oldValue, newValue) -> {
                if (Boolean.TRUE.equals(txtConfirmPassword.getProperties().get("validationTouched"))) {
                    validateField(txtConfirmPassword, lblConfirmPasswordError,
                            this::validatePasswordConfirmation, false);
                }
            });
        }

        if (cmbGenero != null) {
            cmbGenero.getItems().setAll(
                    Gender.M,
                    Gender.F
            );
        }
        if (dpDataNascimento != null) {
            dpDataNascimento.setDayCellFactory(picker -> new DateCell() {
                @Override
                public void updateItem(LocalDate date, boolean empty) {
                    super.updateItem(date, empty);
                    LocalDate today = LocalDate.now();
                    setDisable(empty || date.isBefore(today.minusYears(90))
                            || date.isAfter(today.minusYears(15)));
                }
            });
            dpDataNascimento.valueProperty().addListener((observable, oldValue, newValue) ->
                    validateBirthDate(false));
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

        if (linkTerms != null) {
            linkTerms.setOnAction(event -> showTerms());
        }

        if (linkVoltarLogin != null) {
            linkVoltarLogin.setOnAction(event -> showLogin(event.getSource()));
        }
    }

    private void continueRegistration(Object source) {
        boolean textFieldsValid =
                validateField(txtPrimeirosNomes, lblFirstNameError, this::validateName, true)
                & validateField(txtApelidos, lblLastNameError, this::validateLastName, true)
                & validateField(txtTelefone, lblPhoneError, this::validatePhone, true)
                & validateField(txtDocumento, lblDocumentError, this::validateDocument, true);
        if (!textFieldsValid) {
            return;
        }
        if (isBlank(txtPrimeirosNomes.getText())
                || isBlank(txtApelidos.getText())
                || isBlank(txtTelefone.getText())
                || cmbGenero.getValue() == null
                || dpDataNascimento.getValue() == null) {
            showAlert(Alert.AlertType.WARNING,
                    "Preencha os dados pessoais obrigatórios antes de continuar. O BI/NUIT pode ser enviado depois, na verificação KYC.");
            return;
        }
        if (!validateBirthDate(true)) {
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
        boolean fieldsValid =
                validateField(txtUsername, lblUsernameError, this::validateUsername, true)
                & validateField(txtEmail, lblEmailError, this::validateEmail, true)
                & validateField(txtPassword, lblNewPasswordError, this::validatePassword, true)
                & validateField(txtConfirmPassword, lblConfirmPasswordError,
                        this::validatePasswordConfirmation, true);
        if (!fieldsValid) {
            return;
        }
        if (chkAcceptTerms == null || !chkAcceptTerms.isSelected()) {
            showAlert(Alert.AlertType.WARNING,
                    "Leia e aceite os Termos e Condições antes de criar a conta.");
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

            authService.register(user, txtConfirmPassword.getText(), chkAcceptTerms.isSelected());
            showAlert(Alert.AlertType.INFORMATION,
                    "A conta foi criada com sucesso e está por verificar. Pode participar em grupos; para criar grupos, submeta o BI/NUIT e as fotografias para aprovação do administrador.");
            showLogin(source);
        } catch (RuntimeException exception) {
            showAlert(Alert.AlertType.ERROR, exception.getMessage());
        }
    }

    private void showTerms() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Termos e Condições");
        alert.setHeaderText("Participação em grupos e incumprimento de pagamentos");
        alert.setContentText(
                "1. Cada membro é responsável pelos compromissos que assume perante os outros membros, " +
                "incluindo contribuições e pagamentos acordados no grupo.\n\n" +
                "2. O Konexion disponibiliza ferramentas para organizar grupos, mas não garante que os " +
                "membros cumpram os seus compromissos, não cobre valores em falta e não reembolsa perdas " +
                "causadas pelo incumprimento de outro membro. Isto não exclui responsabilidades da " +
                "plataforma que não possam ser afastadas por lei.\n\n" +
                "3. O utilizador deve avaliar os riscos, confirmar os acordos do grupo e guardar os " +
                "comprovativos das suas operações. A existência de um registo na aplicação não substitui " +
                "um acordo entre os membros nem garante a recuperação de valores.\n\n" +
                "4. A violação das regras da plataforma ou do grupo pode resultar, conforme a gravidade, " +
                "em advertência, restrição de funcionalidades, suspensão ou encerramento da conta, " +
                "respeitando a lei aplicável. Essas medidas não anulam pagamentos ou outras obrigações " +
                "assumidas pelo utilizador.\n\n" +
                "Ao marcar a caixa, confirma que leu e aceita estas condições."
        );
        StyledDialogs.showAndWait(alert);
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

    private boolean validateBirthDate(boolean force) {
        if (lblBirthDateError == null || dpDataNascimento == null) {
            return true;
        }
        LocalDate birthDate = dpDataNascimento.getValue();
        if (!force && birthDate == null) {
            lblBirthDateError.setText("");
            lblBirthDateError.setVisible(false);
            lblBirthDateError.setManaged(false);
            return true;
        }
        String error = null;
        try {
            UserValidator.validateBirthDate(birthDate);
        } catch (IllegalArgumentException exception) {
            error = exception.getMessage();
        }
        boolean valid = error == null;
        lblBirthDateError.setText(valid ? "" : error);
        lblBirthDateError.setVisible(!valid);
        lblBirthDateError.setManaged(!valid);
        return valid;
    }

    private String validateName(String value) {
        String name = value == null ? "" : value.trim();
        if (name.length() < 2 || name.length() > 60
                || !name.matches("^[\\p{L}]+(?:[ '-][\\p{L}]+)*$")) {
            return "Use 2 a 60 letras; espaços, apóstrofos e hífenes são permitidos.";
        }
        return null;
    }

    private String validateLastName(String value) {
        return validateName(value);
    }

    private String validateDocument(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String document = value.trim().toUpperCase(Locale.ROOT);
        if (!document.matches("^(?:[0-9]{12}[A-Z]|[0-9]{9})$")) {
            return "Introduza um BI com 12 dígitos e uma letra, ou um NUIT com 9 dígitos.";
        }
        return null;
    }

    private String validatePhone(String value) {
        String phoneNumber = value == null ? "" : value.replaceAll("\\s+", "");
        if (!phoneNumber.matches("^(?:\\+258)?(?:82|83|84|85|86|87)[0-9]{7}$")) {
            return "Introduza um número móvel moçambicano válido.";
        }
        return null;
    }

    private String validateUsername(String value) {
        String username = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (username.length() < 4 || username.length() > 20
                || !username.matches("^[a-z][a-z0-9._]{2,18}[a-z0-9_]$")
                || username.contains("..")) {
            return "Use 4 a 20 caracteres; comece por uma letra e use letras, números, pontos ou underscores.";
        }
        return null;
    }

    private String validateEmail(String value) {
        String email = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (email.length() > 254
                || !email.matches("(?=.{1,254}$)[a-z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-z0-9-]+(?:\\.[a-z0-9-]+)+")) {
            return "Introduza um endereço de e-mail válido.";
        }
        return null;
    }

    private String validatePassword(String value) {
        if (value == null || value.length() < 8 || value.length() > 64
                || !value.matches(".*[A-Z].*") || !value.matches(".*[a-z].*")
                || !value.matches(".*[0-9].*") || !value.matches(".*[^a-zA-Z0-9].*")) {
            return "Use 8 a 64 caracteres, com maiúscula, minúscula, número e símbolo.";
        }
        return null;
    }

    private String validatePasswordConfirmation(String value) {
        if (value == null || value.isBlank()) {
            return "Confirme a palavra-passe.";
        }
        if (txtPassword == null || !value.equals(txtPassword.getText())) {
            return "As palavras-passe não coincidem.";
        }
        return null;
    }

    private void showAlert(Alert.AlertType type, String message) {
        StyledDialogs.showAndWait(new Alert(type, message == null
                ? "Ocorreu um erro inesperado." : message));
    }
}
