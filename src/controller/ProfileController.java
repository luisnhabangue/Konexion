package controller;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import model.Contribution;
import model.User;
import model.Cycle;
import model.Group;
import service.AuthService;
import service.AdminService;
import service.GroupService;
import util.SessionContext;
import util.StyledDialogs;
import util.UiFormatting;

import java.io.IOException;
import java.io.File;
import java.nio.file.Files;
import java.io.IOException;
import java.net.URL;
import java.util.List;

public class ProfileController {
    private final GroupService groupService = new GroupService();
    private final AuthService authService = new AuthService();
    private final AdminService adminService = new AdminService();
    private File documentPhoto;
    private File selfiePhoto;

    @FXML private Label lblProfileName;
    @FXML private Label lblUsername;
    @FXML private Label lblPhone;
    @FXML private Label lblEmail;
    @FXML private Label lblDocument;
    @FXML private Label lblProfileInitials;
    @FXML private Label lblVerificationStatus;
    @FXML private GridPane profileInfo;
    @FXML private VBox kycSection;
    @FXML private TextField txtKycDocument;
    @FXML private Button btnChooseDocumentPhoto;
    @FXML private Button btnChooseSelfie;
    @FXML private Button btnSubmitKyc;
    @FXML private Label lblDocumentPhoto;
    @FXML private Label lblSelfiePhoto;
    @FXML private VBox leadershipSection;
    @FXML private TextField txtLeadershipReason;
    @FXML private Button btnRequestLeadership;
    @FXML private Button btnEditProfile;
    @FXML private ToggleButton tabContributions;
    @FXML private ToggleButton tabFinished;
    @FXML private VBox paneContributions;
    @FXML private VBox paneFinished;
    @FXML private VBox profileActivityRows;

    @FXML
    private void initialize() {
        User user = SessionContext.getCurrentUser();
        lblProfileName.setText(UiFormatting.fullName(user));
        lblProfileInitials.setText(UiFormatting.initials(user));
        lblUsername.setText("@" + user.getUsername());
        lblPhone.setText(user.getPhoneNumber());
        lblEmail.setText(user.getEmail());
        btnEditProfile.setOnAction(event -> editProfile(user));
        tabContributions.setOnAction(event -> showPane(paneContributions));
        tabFinished.setOnAction(event -> showPane(paneFinished));
        lblDocument.setText(user.getDocumentId() == null ? "Não submetido" : user.getDocumentId());
        lblVerificationStatus.setText(user.isVerified() ? "VERIFICADO" : "POR VERIFICAR");
        if (user.isKycPending()) {
            lblVerificationStatus.setText("EM ANÁLISE");
        } else if ("REJECTED".equals(user.getKycStatus())) {
            lblVerificationStatus.setText("RECUSADO · REENVIE");
        }

        if (user.isVerified()) {
            lblVerificationStatus.getStyleClass().remove("badge-warning");
            lblVerificationStatus.getStyleClass().add("badge-success");
        }
        kycSection.setVisible(!user.isVerified());
        kycSection.setManaged(!user.isVerified());
        txtKycDocument.setText(user.getDocumentId() == null ? "" : user.getDocumentId());
        btnChooseDocumentPhoto.setOnAction(event -> choosePhoto(true));
        btnChooseSelfie.setOnAction(event -> choosePhoto(false));
        btnSubmitKyc.setOnAction(event -> submitKyc(user));
        boolean canRequestLeadership = user.isVerified() && (user.getUserRole() == null
                || !user.getUserRole().isAdministrator() && !user.getUserRole().isGroupLeader());
        leadershipSection.setVisible(canRequestLeadership);
        leadershipSection.setManaged(canRequestLeadership);
        btnRequestLeadership.setOnAction(event -> requestLeadership(user));
        configureProfileColumns();
        profileInfo.widthProperty().addListener((observable, oldWidth, newWidth) -> configureProfileColumns());
        renderContributions(groupService.findContributionsForUser(user.getId()));
        renderFinishedGroups(groupService.findAllForUser(user.getId()));
    }

    private void requestLeadership(User user) {
            try {
                adminService.requestGroupLeadership(user.getId(), txtLeadershipReason.getText());
                leadershipSection.setVisible(false);
                leadershipSection.setManaged(false);
                StyledDialogs.showAndWait(new Alert(Alert.AlertType.INFORMATION,
                        "O pedido foi enviado ao administrador para análise."));
            } catch (RuntimeException exception) {
                StyledDialogs.showAndWait(new Alert(Alert.AlertType.ERROR, exception.getMessage()));
            }
        }
    private void configureProfileColumns() {
        if (profileInfo == null || profileInfo.getChildren().isEmpty()) {
            return;
        }
        boolean compact = profileInfo.getWidth() < 700;
        profileInfo.getColumnConstraints().clear();
        int columnCount = compact ? 2 : 4;
        for (int index = 0; index < columnCount; index++) {
            javafx.scene.layout.ColumnConstraints column = new javafx.scene.layout.ColumnConstraints();
            column.setPercentWidth(100.0 / columnCount);
            column.setHgrow(javafx.scene.layout.Priority.ALWAYS);
            profileInfo.getColumnConstraints().add(column);
        }
        for (int index = 0; index < profileInfo.getChildren().size(); index++) {
            GridPane.setColumnIndex(profileInfo.getChildren().get(index), compact ? index % 2 : index);
            GridPane.setRowIndex(profileInfo.getChildren().get(index), compact ? index / 2 : 0);
        }
    }

    private void choosePhoto(boolean document) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(document ? "Selecionar fotografia do documento" : "Selecionar fotografia de confirmação");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "Imagens", "*.png", "*.jpg", "*.jpeg", "*.webp"));
        File selected = chooser.showOpenDialog(btnSubmitKyc.getScene().getWindow());
        if (selected == null) {
            return;
        }
        if (document) {
            documentPhoto = selected;
            lblDocumentPhoto.setText(selected.getAbsolutePath());
        } else {
            selfiePhoto = selected;
            lblSelfiePhoto.setText(selected.getAbsolutePath());
        }
    }

    private void submitKyc(User user) {
        try {
            if (documentPhoto == null || selfiePhoto == null) {
                throw new IllegalArgumentException("Selecione as duas fotografias antes de enviar.");
            }
            if (documentPhoto.length() > 8L * 1024 * 1024 || selfiePhoto.length() > 8L * 1024 * 1024) {
                throw new IllegalArgumentException("Cada fotografia deve ter no máximo 8 MB.");
            }
            byte[] documentBytes = Files.readAllBytes(documentPhoto.toPath());
            byte[] selfieBytes = Files.readAllBytes(selfiePhoto.toPath());
            authService.submitKyc(user.getId(), txtKycDocument.getText(), documentBytes, selfieBytes);
            user.setDocumentId(txtKycDocument.getText().trim().toUpperCase(java.util.Locale.ROOT));
            user.setIdentityDocumentImage(documentBytes);
            user.setIdentitySelfieImage(selfieBytes);
            lblDocument.setText(user.getDocumentId());
            lblVerificationStatus.setText("EM ANÁLISE");
            StyledDialogs.showAndWait(new Alert(Alert.AlertType.INFORMATION,
                    "Documentos enviados. A verificação ficará concluída após a aprovação do administrador."));
        } catch (IOException | RuntimeException exception) {
            StyledDialogs.showAndWait(new Alert(Alert.AlertType.ERROR, exception.getMessage()));
        }
    }

    private void renderContributions(List<Contribution> contributions) {
        profileActivityRows.getChildren().clear();
        for (Contribution contribution : contributions) {
            javafx.scene.layout.HBox row = new javafx.scene.layout.HBox(14);
            row.getStyleClass().add("t-row");
            Label date = new Label(UiFormatting.date(contribution.getPaymentDate()));
            date.getStyleClass().addAll("t-td", "c-w110");
            Label group = new Label(contribution.getUserGroup().getGroup().getGroupName());
            group.getStyleClass().add("t-name");
            group.setWrapText(true);
            javafx.scene.layout.HBox.setHgrow(group, javafx.scene.layout.Priority.ALWAYS);
            Label amount = new Label(UiFormatting.money(contribution.getAmount()));
            amount.getStyleClass().addAll("t-amount", "c-w130");
            Label status = new Label(contribution.getStatus().toString());
            status.getStyleClass().addAll("badge",
                    contribution.getStatus() == model.enums.ContributionStatus.PAID ? "badge-success" : "badge-warning");
            status.setMinWidth(90);
            row.getChildren().addAll(date, group, amount, status);
            profileActivityRows.getChildren().add(row);
        }
        if (contributions.isEmpty()) {
            Label empty = new Label("Ainda não há contribuições associadas a esta conta.");
            empty.getStyleClass().add("section-sub");
            profileActivityRows.getChildren().add(empty);
        }
    }

    private void showPane(VBox selected) {
        paneContributions.setVisible(selected == paneContributions);
        paneContributions.setManaged(selected == paneContributions);
        paneFinished.setVisible(selected == paneFinished);
        paneFinished.setManaged(selected == paneFinished);
    }

    private void renderFinishedGroups(List<Group> groups) {
        paneFinished.getChildren().clear();
        List<Group> finished = groups.stream().filter(group -> {
            List<Cycle> cycles = groupService.findCycles(group.getGroupId());
            return !cycles.isEmpty() && cycles.get(cycles.size() - 1).getStatus() == model.enums.CycleStatus.CONCLUDED;
        }).toList();
        if (finished.isEmpty()) {
            paneFinished.getChildren().add(new Label("Ainda não há grupos com um ciclo concluído."));
            return;
        }
        for (Group group : finished) {
            Label entry = new Label(group.getGroupName() + " · " + group.getMembers().size() + " membros");
            entry.getStyleClass().add("t-row");
            paneFinished.getChildren().add(entry);
        }
    }

    private void editProfile(User user) {
        try {
            URL editDialogUrl = getClass().getResource("/view/fxml/profile-edit-dialog.fxml");
            if (editDialogUrl == null) {
                throw new IOException("Não foi possível encontrar a tela de edição do perfil.");
            }
            FXMLLoader loader = new FXMLLoader(editDialogUrl);
            Parent content = loader.load();
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(btnEditProfile.getScene().getWindow());
            dialog.setTitle("Editar perfil");
            dialog.setScene(new Scene(content));

            ProfileEditDialogController controller = loader.getController();
            controller.configure(user, dialog, () -> {
                lblProfileName.setText(UiFormatting.fullName(user));
                lblProfileInitials.setText(UiFormatting.initials(user));
                lblEmail.setText(user.getEmail());
                lblPhone.setText(user.getPhoneNumber());
            });
            dialog.showAndWait();
        } catch (IOException | RuntimeException e) {
            StyledDialogs.showAndWait(new Alert(Alert.AlertType.ERROR, e.getMessage() == null
                    ? "Não foi possível abrir a edição do perfil."
                    : e.getMessage()));
        }
    }
}
