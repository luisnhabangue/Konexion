package controller;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.VBox;
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
import service.GroupService;
import util.SessionContext;
import util.UiFormatting;

import java.io.IOException;
import java.net.URL;
import java.util.List;

public class ProfileController {
    private final GroupService groupService = new GroupService();
    private final AuthService authService = new AuthService();

    @FXML private Label lblProfileName;
    @FXML private Label lblUsername;
    @FXML private Label lblPhone;
    @FXML private Label lblEmail;
    @FXML private Label lblDocument;
    @FXML private Label lblProfileInitials;
    @FXML private Button btnEditProfile;
    @FXML private ToggleButton tabContributions;
    @FXML private ToggleButton tabFinished;
    @FXML private ToggleButton tabSettings;
    @FXML private VBox paneContributions;
    @FXML private VBox paneFinished;
    @FXML private VBox paneSettings;
    @FXML private VBox profileActivityRows;

    @FXML
    private void initialize() {
        User user = SessionContext.getCurrentUser();
        lblProfileName.setText(UiFormatting.fullName(user));
        lblProfileInitials.setText(UiFormatting.initials(user));
        lblUsername.setText("@" + user.getUsername());
        lblPhone.setText(user.getPhoneNumber());
        lblEmail.setText(user.getEmail());
        lblDocument.setText(user.getDocumentId());
        btnEditProfile.setOnAction(event -> editProfile(user));
        tabContributions.setOnAction(event -> showPane(paneContributions));
        tabFinished.setOnAction(event -> showPane(paneFinished));
        tabSettings.setOnAction(event -> showPane(paneSettings));
        renderContributions(groupService.findContributionsForUser(user.getId()));
        renderFinishedGroups(groupService.findAllForUser(user.getId()));
    }

    private void renderContributions(List<Contribution> contributions) {
        profileActivityRows.getChildren().clear();
        for (Contribution contribution : contributions) {
            Label item = new Label(UiFormatting.date(contribution.getPaymentDate()) + " · "
                    + contribution.getUserGroup().getGroup().getGroupName() + " · "
                    + UiFormatting.money(contribution.getAmount()) + " · " + contribution.getStatus());
            item.getStyleClass().add("t-row");
            profileActivityRows.getChildren().add(item);
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
        paneSettings.setVisible(selected == paneSettings);
        paneSettings.setManaged(selected == paneSettings);
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
            new Alert(Alert.AlertType.ERROR, e.getMessage() == null
                    ? "Não foi possível abrir a edição do perfil."
                    : e.getMessage()).showAndWait();
        }
    }
}
