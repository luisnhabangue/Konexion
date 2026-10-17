package controller;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import model.enums.Page;
import util.ModalDialogs;
import util.Navigator;
import util.SessionContext;

import java.util.EnumMap;
import java.util.Map;

public class MainLayoutController {
    @FXML private StackPane contentArea;
    @FXML private Button navDashboard;
    @FXML private Button navGroups;
    @FXML private Button navHistory;
    @FXML private Button navProfile;
    @FXML private Button navSettings;
    @FXML private Button btnLogout;
    @FXML private Button btnCreateGroup;
    @FXML private Button btnNotifications;
    @FXML private TextField searchField;
    @FXML private Label lblUserName;
    @FXML private Label lblUserInitials;
    @FXML private Label lblNotificationCount;

    private final Map<Page, Button> navigation = new EnumMap<>(Page.class);
    private Page currentPage = Page.DASHBOARD;

    @FXML
    private void initialize() {
        navigation.put(Page.DASHBOARD, navDashboard);
        navigation.put(Page.GROUPS, navGroups);
        navigation.put(Page.HISTORY, navHistory);
        navigation.put(Page.PROFILE, navProfile);

        navDashboard.setOnAction(event -> navigate(Page.DASHBOARD));
        navGroups.setOnAction(event -> navigate(Page.GROUPS));
        navHistory.setOnAction(event -> navigate(Page.HISTORY));
        navProfile.setOnAction(event -> navigate(Page.PROFILE));
        navSettings.setOnAction(event -> navigate(Page.PROFILE));
        btnCreateGroup.setOnAction(event -> openCreateGroup());
        btnNotifications.setOnAction(event -> new Alert(Alert.AlertType.INFORMATION,
                "Não existem notificações novas.").showAndWait());
        btnLogout.setOnAction(event -> logout());
        searchField.setOnAction(event -> {
            GroupsController groups = navigate(Page.GROUPS);
            groups.search(searchField.getText());
        });

        lblUserName.setText(SessionContext.getCurrentUser().getFirstName());
        lblUserInitials.setText(util.UiFormatting.initials(SessionContext.getCurrentUser()));
        lblNotificationCount.setText("0");
        lblNotificationCount.setVisible(false);
        Navigator.init(contentArea, this::updateNavigation);
        navigate(Page.DASHBOARD);
    }

    private <T> T navigate(Page page) {
        currentPage = page;
        return Navigator.go(page);
    }

    private void updateNavigation(Page page) {
        lblUserName.setText(SessionContext.getCurrentUser().getFirstName());
        lblUserInitials.setText(util.UiFormatting.initials(SessionContext.getCurrentUser()));
        navigation.values().forEach(button -> button.getStyleClass().remove("active"));
        Button selected = navigation.get(page);
        if (selected != null && !selected.getStyleClass().contains("active")) {
            selected.getStyleClass().add("active");
        }
    }

    private void openCreateGroup() {
        ModalDialogs.showCreateGroup(contentArea.getScene().getWindow(), () -> navigate(currentPage));
    }

    private void logout() {
        try {
            SessionContext.clear();
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/view/fxml/LoginView.fxml"));
            Parent login = loader.load();
            contentArea.getScene().setRoot(login);
        } catch (java.io.IOException e) {
            new Alert(Alert.AlertType.ERROR, "Não foi possível terminar a sessão: " + e.getMessage()).showAndWait();
        }
    }
}