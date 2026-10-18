package controller;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import model.User;
import service.AuthService;

public class ProfileEditDialogController {

    private final AuthService authService = new AuthService();

    @FXML private TextField txtFirstName;
    @FXML private TextField txtLastName;
    @FXML private TextField txtPhone;
    @FXML private Label lblEmail;
    @FXML private Label lblUsername;
    @FXML private Label lblDocument;
    @FXML private Button btnSave;
    @FXML private Button btnCancel;

    private User user;
    private Stage dialog;
    private Runnable onSaved;

    @FXML
    private void initialize() {
        btnSave.setOnAction(event -> save());
        btnCancel.setOnAction(event -> dialog.close());
    }

    public void configure(User user, Stage dialog, Runnable onSaved) {
        this.user = user;
        this.dialog = dialog;
        this.onSaved = onSaved;

        txtFirstName.setText(user.getFirstName());
        txtLastName.setText(user.getLastName());
        txtPhone.setText(user.getPhoneNumber());
        lblEmail.setText(user.getEmail());
        lblUsername.setText("@" + user.getUsername());
        lblDocument.setText(user.getDocumentId());
    }

    private void save() {
        try {
            authService.updateProfile(
                    user,
                    txtFirstName.getText(),
                    txtLastName.getText(),
                    user.getEmail(),
                    txtPhone.getText()
            );
            onSaved.run();
            dialog.close();
        } catch (RuntimeException exception) {
            new Alert(
                    Alert.AlertType.ERROR,
                    exception.getMessage() == null
                            ? "Não foi possível atualizar o perfil."
                            : exception.getMessage()
            ).showAndWait();
        }
    }
}
