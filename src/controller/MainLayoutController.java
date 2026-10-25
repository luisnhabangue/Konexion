package controller;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Screen;
import javafx.geometry.Rectangle2D;
import javafx.util.Duration;
import model.MembershipInvitation;
import model.User;
import model.UserNotification;
import model.enums.Page;
import service.ContributionDeadlineService;
import service.AuthService;
import service.GroupService;
import util.ModalDialogs;
import util.Navigator;
import util.SessionContext;
import util.StyledDialogs;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Controlador JavaFX responsável pela estrutura principal (Main Layout / Shell) da aplicação[cite: 110].
 * <p>
 * Gere o menu de navegação lateral, o cabeçalho do utilizador, a central de notificações e convites
 * pendentes, o fecho de sessão e o contentor dinâmico de vistas ({@link StackPane})[cite: 110].
 * </p>
 *
 * @author Sistema Xitique
 * @version 1.0
 * @since 1.0
 */
public class MainLayoutController {
    private static final KeyCodeCombination SEARCH_SHORTCUT =
            new KeyCodeCombination(KeyCode.K, KeyCombination.SHORTCUT_DOWN);

    /** Contentor de pilha onde as páginas da aplicação são carregadas dinamicamente[cite: 110]. */
    @FXML private StackPane contentArea;

    /** Botão de navegação para a vista de Painel/Visão Geral ou Painel de Administração[cite: 110]. */
    @FXML private Button navDashboard;

    /** Botão de navegação para a vista de Grupos[cite: 110]. */
    @FXML private Button navGroups;

    /** Botão de navegação para a vista de Histórico de Contribuições[cite: 110]. */
    @FXML private Button navHistory;

    /** Botão de navegação para a vista do Perfil do utilizador[cite: 110]. */
    @FXML private Button navProfile;

    /** Botão para fechar a sessão ativa do utilizador[cite: 110]. */
    @FXML private Button btnLogout;

    /** Botão para abrir o diálogo de criação de novo grupo ou verificação de conta[cite: 110]. */
    @FXML private Button btnCreateGroup;

    /** Botão para abrir a lista de notificações/convites pendentes[cite: 110]. */
    @FXML private Button btnNotifications;

    /** Campo de pesquisa global por grupos[cite: 110]. */
    @FXML private TextField searchField;

    /** Rótulo com o primeiro nome do utilizador autenticado[cite: 110]. */
    @FXML private Label lblUserName;

    /** Rótulo com o tipo de utilizador autenticado[cite: 110]. */
    @FXML private Label lblUserRole;

    /** Rótulo com as iniciais do utilizador para exibição no avatar[cite: 110]. */
    @FXML private Label lblUserInitials;

    /** Rótulo para exibição do contador de notificações pendentes[cite: 110]. */
    @FXML private Label lblNotificationCount;

    /** Mapeamento entre as páginas da aplicação e os respetivos botões de navegação[cite: 110]. */
    private final Map<Page, Button> navigation = new EnumMap<>(Page.class);

    /** Identificador da página atualmente apresentada[cite: 110]. */
    private Page currentPage = Page.DASHBOARD;

    /** Serviço responsável pelas operações de grupos e gestão de convites[cite: 110]. */
    private final GroupService groupService = new GroupService();
    private final AuthService authService = new AuthService();
    private final ContributionDeadlineService deadlineService = new ContributionDeadlineService();
    private Timeline deadlineMonitor;
    private Scene shortcutScene;
    private Popup notificationPopup;

    /**
     * Inicializa os botões de navegação, ajusta o papel do utilizador (administrador vs regular)
     * e configura os ouvintes de eventos e notificações pendentes[cite: 110].
     */
    @FXML
    private void initialize() {
        refreshCurrentUser();
        navigation.put(Page.DASHBOARD, navDashboard);
        navigation.put(Page.ADMIN, navDashboard);
        navigation.put(Page.GROUPS, navGroups);
        navigation.put(Page.HISTORY, navHistory);
        navigation.put(Page.PROFILE, navProfile);

        boolean administrator = SessionContext.getCurrentUser().getUserRole() != null
                && SessionContext.getCurrentUser().getUserRole().isAdministrator();
        navDashboard.setText(administrator ? "Painel de administração" : "Visão geral");
        if (administrator) {
            navGroups.setVisible(false);
            navGroups.setManaged(false);
            navHistory.setVisible(false);
            navHistory.setManaged(false);
            btnCreateGroup.setVisible(false);
            btnCreateGroup.setManaged(false);
            btnNotifications.setVisible(false);
            btnNotifications.setManaged(false);
            lblNotificationCount.setVisible(false);
            lblNotificationCount.setManaged(false);
            searchField.setPromptText("Pesquisar utilizadores e grupos...");
        }
        navDashboard.setOnAction(event -> navigate(administrator ? Page.ADMIN : Page.DASHBOARD));
        navGroups.setOnAction(event -> navigate(Page.GROUPS));
        navHistory.setOnAction(event -> navigate(Page.HISTORY));
        navProfile.setOnAction(event -> navigate(Page.PROFILE));
        btnCreateGroup.setOnAction(event -> openCreateGroup());
        btnNotifications.setOnAction(event -> showNotifications());
        btnLogout.setOnAction(event -> logout());
        searchField.setOnAction(event -> {
            if (administrator) {
                AdminDashboardController admin = navigate(Page.ADMIN);
                admin.search(searchField.getText());
            } else {
                GroupsController groups = navigate(Page.GROUPS);
                groups.search(searchField.getText());
            }
        });
        contentArea.sceneProperty().addListener((observable, oldScene, newScene) ->
                registerSearchShortcut(newScene));

        updateUserInfo();
        lblNotificationCount.setText("0");
        lblNotificationCount.setVisible(false);
        refreshInvitationCount();
        Navigator.init(contentArea, this::updateNavigation);
        navigate(administrator ? Page.ADMIN : Page.DASHBOARD);
        deadlineMonitor = new Timeline(new KeyFrame(Duration.minutes(15),
                event -> refreshNotificationCount()));
        deadlineMonitor.setCycleCount(Animation.INDEFINITE);
        deadlineMonitor.play();
    }

    /**
     * Navega para a página especificada e guarda o estado da página ativa[cite: 110].
     *
     * @param <T> O tipo do controlador associado à página.
     * @param page A página {@link Page} para a qual se pretende navegar.
     * @return A instância do controlador da página carregada.
     */
    private <T> T navigate(Page page) {
        currentPage = page;
        return Navigator.go(page);
    }

    private void registerSearchShortcut(Scene scene) {
        if (shortcutScene != null) {
            shortcutScene.getAccelerators().remove(SEARCH_SHORTCUT);
        }
        shortcutScene = scene;
        if (shortcutScene != null) {
            shortcutScene.getAccelerators().put(SEARCH_SHORTCUT, searchField::requestFocus);
        }
    }

    /**
     * Atualiza a indicação visual do menu de navegação, atribuindo a classe ativa ao botão selecionado[cite: 110].
     *
     * @param page A página {@link Page} em exibição.
     */
    private void updateNavigation(Page page) {
        updateUserInfo();
        navigation.values().forEach(button -> button.getStyleClass().remove("active"));
        Button selected = navigation.get(page);
        if (selected != null && !selected.getStyleClass().contains("active")) {
            selected.getStyleClass().add("active");
        }
    }

    private void updateUserInfo() {
        User currentUser = SessionContext.getCurrentUser();
        lblUserName.setText(currentUser.getFirstName());
        lblUserInitials.setText(util.UiFormatting.initials(currentUser));
        lblUserRole.setText(currentUser.getUserRole() != null
                && currentUser.getUserRole().isAdministrator() ? "Administrador" : "");
    }

    /**
     * Abre a caixa de diálogo modal para criação de um novo grupo de Xitique[cite: 110].
     */
    private void openCreateGroup() {
        refreshCurrentUser();
        User currentUser = SessionContext.getCurrentUser();
        boolean administrator = currentUser.getUserRole() != null
                && currentUser.getUserRole().isAdministrator();
        if (administrator) {
            throw new SecurityException("Administradores não podem criar nem participar em grupos.");
        }
        if (!currentUser.isVerified() && !administrator) {
            btnCreateGroup.setText("Verificar conta");
            navigate(Page.PROFILE);
            return;
        }
        btnCreateGroup.setText("Criar grupo");
        ModalDialogs.showCreateGroup(contentArea.getScene().getWindow(), () -> navigate(currentPage));
    }

    private void refreshCurrentUser() {
        var refreshedUser = authService.findById(SessionContext.getCurrentUser().getId());
        if (refreshedUser != null) {
            SessionContext.setCurrentUser(refreshedUser);
            boolean administrator = refreshedUser.getUserRole() != null
                    && refreshedUser.getUserRole().isAdministrator();
            btnCreateGroup.setVisible(!administrator);
            btnCreateGroup.setManaged(!administrator);
            btnCreateGroup.setText(refreshedUser.isVerified() ? "Criar grupo" : "Verificar conta");
        }
    }

    /**
     * Encerra a sessão atual do utilizador, limpa o contexto da sessão e redireciona para o ecrã de login[cite: 110].
     */
    private void logout() {
        if (deadlineMonitor != null) {
            deadlineMonitor.stop();
        }
        registerSearchShortcut(null);
        try {
            SessionContext.clear();
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/view/fxml/LoginView.fxml"));
            Parent login = loader.load();
            contentArea.getScene().setRoot(login);
        } catch (java.io.IOException e) {
            StyledDialogs.showAndWait(new Alert(Alert.AlertType.ERROR,
                    "Não foi possível terminar a sessão: " + e.getMessage()));
        }
    }

    private void showNotifications() {
        if (notificationPopup != null && notificationPopup.isShowing()) {
            notificationPopup.hide();
            return;
        }

        try {
            Long userId = SessionContext.getCurrentUser().getId();
            VBox cards = new VBox(10);
            cards.getStyleClass().add("notification-list");

            VBox panel = new VBox(12);
            panel.getStyleClass().add("notification-panel");
            panel.getStylesheets().add(
                    getClass().getResource("/view/fxml/app.css").toExternalForm());
            Label title = new Label("Notificações");
            title.getStyleClass().add("notification-panel-title");
            ScrollPane scrollPane = new ScrollPane(cards);
            scrollPane.setFitToWidth(true);
            scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            scrollPane.getStyleClass().add("notification-scroll");
            panel.getChildren().addAll(title, scrollPane);

            notificationPopup = new Popup();
            notificationPopup.setAutoFix(true);
            notificationPopup.setAutoHide(true);
            notificationPopup.setHideOnEscape(true);
            notificationPopup.getContent().add(panel);

            refreshNotificationCards(cards, userId);

            Bounds buttonBounds = btnNotifications.localToScreen(btnNotifications.getBoundsInLocal());
            if (buttonBounds == null) {
                return;
            }
            double popupWidth = 380;
            double x = buttonBounds.getMaxX() - popupWidth;
            double y = buttonBounds.getMaxY() + 8;
            Rectangle2D screenBounds = Screen.getScreensForRectangle(
                    buttonBounds.getMinX(), buttonBounds.getMinY(),
                    buttonBounds.getWidth(), buttonBounds.getHeight()).stream()
                    .findFirst().orElse(Screen.getPrimary()).getVisualBounds();
            x = Math.max(screenBounds.getMinX(),
                    Math.min(x, screenBounds.getMaxX() - popupWidth));
            y = Math.max(screenBounds.getMinY(),
                    Math.min(y, screenBounds.getMaxY() - 460));
            notificationPopup.show(btnNotifications.getScene().getWindow(), x, y);
        } catch (RuntimeException exception) {
            StyledDialogs.showAndWait(new Alert(Alert.AlertType.ERROR, exception.getMessage()));
        }
    }

    private void refreshNotificationCards(VBox cards, Long userId) {
        refreshNotificationCount();
        List<UserNotification> notifications = deadlineService.findUnread(userId);
        List<MembershipInvitation> invitations = groupService.findPendingInvitations(userId);
        cards.getChildren().clear();

        for (MembershipInvitation invitation : invitations) {
            cards.getChildren().add(createInvitationCard(invitation, cards, userId));
        }
        for (UserNotification notification : notifications) {
            cards.getChildren().add(createNotificationCard(notification, cards, userId));
        }
        if (cards.getChildren().isEmpty()) {
            Label empty = new Label("Não tem notificações nem convites pendentes.");
            empty.getStyleClass().add("notification-empty");
            empty.setWrapText(true);
            cards.getChildren().add(empty);
        }
    }

    private VBox createInvitationCard(MembershipInvitation invitation, VBox cards, Long userId) {
        VBox card = createNotificationCardBase();
        Label type = new Label("Convite para grupo");
        type.getStyleClass().add("notification-type");
        Label group = new Label(invitation.getGroup().getGroupName());
        group.getStyleClass().add("notification-card-title");
        Label message = new Label(invitation.getInviter().getFirstName()
                + " convidou-o para participar neste grupo.");
        message.getStyleClass().add("notification-message");
        message.setWrapText(true);

        Button accept = new Button("Aceitar");
        accept.getStyleClass().addAll("btn", "btn-primary", "notification-action");
        accept.setOnAction(event -> respondToInvitation(invitation, userId, true, cards));
        Button decline = new Button("Recusar");
        decline.getStyleClass().addAll("btn", "btn-secondary", "notification-action");
        decline.setOnAction(event -> respondToInvitation(invitation, userId, false, cards));

        HBox actions = new HBox(8, accept, decline);
        actions.setAlignment(Pos.CENTER_RIGHT);
        card.getChildren().addAll(type, group, message, actions);
        return card;
    }

    private VBox createNotificationCard(UserNotification notification, VBox cards, Long userId) {
        VBox card = createNotificationCardBase();
        Label type = new Label(notificationTitle(notification.getType()));
        type.getStyleClass().add("notification-type");
        Label group = new Label(notification.getUserGroup().getGroup().getGroupName()
                + " · Ciclo " + notification.getCycle().getNumero());
        group.getStyleClass().add("notification-card-title");
        Label message = new Label(notification.getMessage());
        message.getStyleClass().add("notification-message");
        message.setWrapText(true);

        Button openGroup = new Button(notificationAction(notification.getType()));
        openGroup.getStyleClass().addAll("btn", "btn-primary", "notification-action");
        openGroup.setOnAction(event -> handleNotificationAction(notification, userId, true, cards));
        Button markRead = new Button("Marcar como lida");
        markRead.getStyleClass().addAll("btn", "btn-secondary", "notification-action");
        markRead.setOnAction(event -> handleNotificationAction(notification, userId, false, cards));

        HBox actions = new HBox(8, markRead, openGroup);
        actions.setAlignment(Pos.CENTER_RIGHT);
        card.getChildren().addAll(type, group, message, actions);
        return card;
    }

    private VBox createNotificationCardBase() {
        VBox card = new VBox(7);
        card.getStyleClass().add("notification-card");
        card.setPadding(new Insets(13));
        return card;
    }

    private String notificationTitle(String type) {
        return switch (type) {
            case "OVERDUE_MEMBER" -> "Contribuição em atraso";
            case "OVERDUE_LEADER" -> "Contribuição de membro em atraso";
            case "CYCLE_PAYOUT" -> "Transferência recebida";
            default -> "Notificação";
        };
    }

    private String notificationAction(String type) {
        return "OVERDUE_MEMBER".equals(type) ? "Pagar contribuição" : "Ver grupo";
    }

    private void handleNotificationAction(UserNotification notification, Long userId,
                                          boolean openGroup, VBox cards) {
        try {
            if (openGroup) {
                notificationPopup.hide();
                Navigator.setSelectedGroupId(notification.getUserGroup().getGroup().getGroupId());
                navigate(Page.GROUP_DETAIL);
            } else {
                deadlineService.markRead(userId, notification.getId());
                refreshNotificationCards(cards, userId);
            }
        } catch (RuntimeException exception) {
            showNotificationError(exception);
        }
    }

    private void respondToInvitation(MembershipInvitation invitation, Long userId,
                                     boolean accepted, VBox cards) {
        try {
            groupService.respondToInvitation(invitation.getId(), userId, accepted);
            refreshNotificationCards(cards, userId);
        } catch (RuntimeException exception) {
            showNotificationError(exception);
        }
    }

    private void showNotificationError(RuntimeException exception) {
        StyledDialogs.showAndWait(new Alert(Alert.AlertType.ERROR, exception.getMessage()));
    }

    /**
     * Atualiza o contador de convites pendentes e altera a visibilidade do badge de notificação[cite: 110].
     */
    private void refreshInvitationCount() {
        refreshNotificationCount();
    }

    private void refreshNotificationCount() {
        try {
            Long userId = SessionContext.getCurrentUser().getId();
            deadlineService.processDeadlines();
            int pending = groupService.findPendingInvitations(userId).size()
                    + deadlineService.countUnread(userId);
            lblNotificationCount.setText(Integer.toString(pending));
            lblNotificationCount.setVisible(pending > 0);
        } catch (RuntimeException exception) {
            StyledDialogs.showAndWait(new Alert(Alert.AlertType.ERROR,
                    "Não foi possível verificar notificações de prazos: " + exception.getMessage()));
        }
    }
}
