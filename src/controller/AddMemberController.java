
package controller;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import model.User;
import service.GroupService;
import util.SessionContext;
import util.StyledDialogs;
import util.UiFormatting;

import java.util.Locale;

/**
 * Controlador JavaFX responsável pela janela modal de adição de novos membros a um grupo de Xitique.
 * <p>
 * Permite pesquisar utilizadores ativos por username ou documento de identificação,
 * verificar a elegibilidade do utilizador e enviar um convite para integrar o grupo.
 * </p>

 * @since 1.0
 */
public class AddMemberController {

    /** Serviço com a lógica de negócio associada a grupos e membros. */
    private final GroupService groupService = new GroupService();

    /** Identificador único do grupo ao qual se pretende adicionar um membro. */
    private Long groupId;

    /** Instância da janela (Stage) modal atual. */
    private Stage dialog;

    /** Ação a ser executada após o convite ser gravado com sucesso. */
    private Runnable afterSave;

    /** Utilizador selecionado nos resultados da pesquisa. */
    private User selectedUser;

    /** Campo de texto para pesquisa do utilizador. */
    @FXML private TextField txtSearchUser;

    /** Contentor horizontal que exibe o resultado da pesquisa. */
    @FXML private HBox searchResult;

    /** Rótulo para apresentação do nome do utilizador encontrado. */
    @FXML private Label lblResultName;

    /** Rótulo para apresentação do username e contacto do utilizador encontrado. */
    @FXML private Label lblResultInfo;

    /** Rótulo que apresenta a pontuação de assiduidade do utilizador encontrado. */
    @FXML private Label lblResultScore;

    /** Rótulo que exibe a posição em que o novo membro ficará na fila do grupo. */
    @FXML private Label lblPosition;

    /** Botão de fecho do diálogo na barra de título. */
    @FXML private Button btnClose;

    /** Botão de cancelamento da operação. */
    @FXML private Button btnCancel;

    /** Botão de confirmação para o envio do convite. */
    @FXML private Button btnConfirm;

    /**
     * Inicializa o controlador após o carregamento da vista FXML.
     * Oculta a secção de resultados e associa os ouvintes de eventos e listeners de texto.
     */
    @FXML
    private void initialize() {
        searchResult.setVisible(false);
        searchResult.setManaged(false);
        txtSearchUser.textProperty().addListener((observable, oldValue, value) -> findUser(value));
        btnClose.setOnAction(event -> close());
        btnCancel.setOnAction(event -> close());
        btnConfirm.setOnAction(event -> addMember());
    }

    /**
     * Configura os parâmetros necessários para o funcionamento da janela modal.
     *
     * @param groupId   Identificador do grupo.
     * @param dialog    Instância da janela {@link Stage} modal.
     * @param afterSave Callback a ser executado após a conclusão com sucesso da operação.
     */
    public void configure(Long groupId, Stage dialog, Runnable afterSave) {
        this.groupId = groupId;
        this.dialog = dialog;
        this.afterSave = afterSave;
        lblPosition.setText((groupService.findMembers(groupId).size() + 1) + "ª posição");
    }

    /**
     * Pesquisa um utilizador ativo com base na consulta introduzida.
     *
     * @param query Termo de pesquisa (username ou documento com pelo menos 3 caracteres).
     */
    private void findUser(String query) {
        selectedUser = null;
        searchResult.setVisible(false);
        searchResult.setManaged(false);
        if (query == null || query.trim().length() < 3) {
            return;
        }
        String identifier = query.trim().toLowerCase(Locale.ROOT);
        selectedUser = groupService.findActiveUserByIdentifier(identifier);
        if (selectedUser != null && groupService.findMembers(groupId).stream()
                .anyMatch(member -> member.getUser().getId().equals(selectedUser.getId()))) {
            selectedUser = null;
        }
        if (selectedUser == null) {
            lblResultName.setText("Nenhum utilizador disponível com esse username ou documento.");
            lblResultInfo.setText("Confirme os dados e tente novamente.");
            lblResultScore.setText("");
        } else {
            lblResultName.setText(UiFormatting.fullName(selectedUser));
            lblResultInfo.setText("@" + selectedUser.getUsername() + " · " + selectedUser.getPhoneNumber());
            lblResultScore.setText("Pontuação: " + selectedUser.getPunctualityPoints() + " pontos");
        }
        searchResult.setVisible(true);
        searchResult.setManaged(true);
    }

    /**
     * Envia o convite ao utilizador selecionado e fecha o diálogo caso a operação seja bem-sucedida.
     */
    private void addMember() {
        if (selectedUser == null) {
            showError("Pesquise um utilizador ativo que ainda não pertença ao grupo.");
            return;
        }
        try {
            groupService.inviteMember(groupId, SessionContext.getCurrentUser().getId(), selectedUser.getId());
            StyledDialogs.showAndWait(new Alert(Alert.AlertType.INFORMATION,
                    "Convite enviado. O utilizador poderá aceitá-lo ou recusá-lo nas notificações."));
            afterSave.run();
            close();
        } catch (RuntimeException e) {
            showError(e.getMessage());
        }
    }

    /**
     * Exibe uma mensagem de erro numa caixa de diálogo estilizada.
     *
     * @param message Mensagem de erro a apresentar.
     */
    private void showError(String message) {
        StyledDialogs.showAndWait(new Alert(Alert.AlertType.ERROR, message));
    }

    /**
     * Fecha a janela modal atual.
     */
    private void close() {
        if (dialog != null) {
            dialog.close();
        }
    }
}