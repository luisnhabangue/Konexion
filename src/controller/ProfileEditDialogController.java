package controller;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import model.User;
import service.AuthService;
import util.StyledDialogs;

/**
 * Controlador JavaFX para o diálogo modal de edição de dados pessoais do utilizador[cite: 114].
 * <p>
 * Permite atualizar o primeiro nome, apelido e número de telefone[cite: 114].
 * </p>
 *
 * @author Sistema Xitique
 * @version 1.0
 * @since 1.0
 */
public class ProfileEditDialogController {

    /** Serviço responsável por atualizar os dados do perfil do utilizador[cite: 114]. */
    private final AuthService authService = new AuthService();

    /** Campo de texto para o primeiro nome[cite: 114]. */
    @FXML private TextField txtFirstName;

    /** Campo de texto para o apelido[cite: 114]. */
    @FXML private TextField txtLastName;

    /** Campo de texto para o contacto telefónico[cite: 114]. */
    @FXML private TextField txtPhone;

    /** Rótulo informativo com o endereço de email do utilizador[cite: 114]. */
    @FXML private Label lblEmail;

    /** Rótulo informativo com o nome de utilizador (username)[cite: 114]. */
    @FXML private Label lblUsername;

    /** Rótulo informativo com o documento de identificação[cite: 114]. */
    @FXML private Label lblDocument;

    /** Botão para confirmar e guardar as alterações[cite: 114]. */
    @FXML private Button btnSave;

    /** Botão para cancelar as alterações e fechar o diálogo[cite: 114]. */
    @FXML private Button btnCancel;

    /** Instância do utilizador que está a ser modificado[cite: 114]. */
    private User user;

    /** Instância do palco (Stage) da janela modal[cite: 114]. */
    private Stage dialog;

    /** Callback a ser executado após os dados serem gravados com sucesso[cite: 114]. */
    private Runnable onSaved;

    /**
     * Inicializa os eventos dos botões de gravação e cancelamento[cite: 114].
     */
    @FXML
    private void initialize() {
        btnSave.setOnAction(event -> save());
        btnCancel.setOnAction(event -> dialog.close());
    }

    /**
     * Preenche os campos da interface com as informações atuais do utilizador e guarda as referências recebidas[cite: 114].
     *
     * @param user    O utilizador a ser editado.
     * @param dialog  O palco modal {@link Stage}.
     * @param onSaved Ação executada após o sucesso da gravação.
     */
    public void configure(User user, Stage dialog, Runnable onSaved) {
        this.user = user;
        this.dialog = dialog;
        this.onSaved = onSaved;

        txtFirstName.setText(user.getFirstName());
        txtLastName.setText(user.getLastName());
        txtPhone.setText(user.getPhoneNumber());
        lblEmail.setText(user.getEmail());
        lblUsername.setText("@" + user.getUsername());
        lblDocument.setText(user.getDocumentId() == null ? "Não submetido" : user.getDocumentId());
    }

    /**
     * Efetua a atualização do perfil através do serviço {@link AuthService} e fecha a caixa de diálogo[cite: 114].
     */
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
            StyledDialogs.showAndWait(new Alert(
                    Alert.AlertType.ERROR,
                    exception.getMessage() == null
                            ? "Não foi possível atualizar o perfil."
                            : exception.getMessage()
            ));
        }
    }
}