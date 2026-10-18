package controller;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import model.User;
import model.UserGroup;
import service.GroupService;
import util.StyledDialogs;
import util.SessionContext;
import util.UiFormatting;

import java.util.Locale;

public class AddMemberController {
    private final GroupService groupService = new GroupService();
    private Long groupId;
    private Stage dialog;
    private Runnable afterSave;
    private User selectedUser;

    @FXML private TextField txtSearchUser;
    @FXML private HBox searchResult;
    @FXML private Label lblResultName;
    @FXML private Label lblResultInfo;
    @FXML private Label lblPosition;
    @FXML private Button btnClose;
    @FXML private Button btnCancel;
    @FXML private Button btnConfirm;

    @FXML
    private void initialize() {
        searchResult.setVisible(false);
        searchResult.setManaged(false);
        txtSearchUser.textProperty().addListener((observable, oldValue, value) -> findUser(value));
        btnClose.setOnAction(event -> close());
        btnCancel.setOnAction(event -> close());
        btnConfirm.setOnAction(event -> addMember());
    }

    public void configure(Long groupId, Stage dialog, Runnable afterSave) {
        this.groupId = groupId;
        this.dialog = dialog;
        this.afterSave = afterSave;
        lblPosition.setText((groupService.findMembers(groupId).size() + 1) + "ª posição");
    }

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
        } else {
            lblResultName.setText(UiFormatting.fullName(selectedUser));
            lblResultInfo.setText("@" + selectedUser.getUsername() + " · " + selectedUser.getPhoneNumber());
        }
        searchResult.setVisible(true);
        searchResult.setManaged(true);
    }

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

    private void showError(String message) {
        StyledDialogs.showAndWait(new Alert(Alert.AlertType.ERROR, message));
    }

    private void close() {
        if (dialog != null) {
            dialog.close();
        }
    }
}
