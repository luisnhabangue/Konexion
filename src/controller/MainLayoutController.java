package controller;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import model.MembershipInvitation;
import model.enums.Page;
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

    /**
     * Inicializa os botões de navegação, ajusta o papel do utilizador (administrador vs regular)
     * e configura os ouvintes de eventos e notificações pendentes[cite: 110].
     */
    @FXML
    private void initialize() {
        navigation.put(Page.DASHBOARD, navDashboard);
        navigation.put(Page.ADMIN, navDashboard);
        navigation.put(Page.GROUPS, navGroups);
        navigation.put(Page.HISTORY, navHistory);
        navigation.put(Page.PROFILE, navProfile);

        boolean administrator = SessionContext.getCurrentUser().getUserRole() != null
                && SessionContext.getCurrentUser().getUserRole().isAdministrator();
        navDashboard.setText(administrator ? "Painel de administração" : "Visão geral");
        navDashboard.setOnAction(event -> navigate(administrator ? Page.ADMIN : Page.DASHBOARD));
        navGroups.setOnAction(event -> navigate(Page.GROUPS));
        navHistory.setOnAction(event -> navigate(Page.HISTORY));
        navProfile.setOnAction(event -> navigate(Page.PROFILE));
        btnCreateGroup.setOnAction(event -> openCreateGroup());
        btnNotifications.setOnAction(event -> showInvitations());
        btnLogout.setOnAction(event -> logout());
        searchField.setOnAction(event -> {
            GroupsController groups = navigate(Page.GROUPS);
            groups.search(searchField.getText());
        });

        lblUserName.setText(SessionContext.getCurrentUser().getFirstName());
        lblUserInitials.setText(util.UiFormatting.initials(SessionContext.getCurrentUser()));
        lblNotificationCount.setText("0");
        lblNotificationCount.setVisible(false);
        refreshInvitationCount();
        Navigator.init(contentArea, this::updateNavigation);
        navigate(administrator ? Page.ADMIN : Page.DASHBOARD);
        if (!SessionContext.getCurrentUser().isVerified() && !administrator) {
            btnCreateGroup.setText("Verificar conta");
            btnCreateGroup.setOnAction(event -> navigate(Page.PROFILE));
        }
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

    /**
     * Atualiza a indicação visual do menu de navegação, atribuindo a classe ativa ao botão selecionado[cite: 110].
     *
     * @param page A página {@link Page} em exibição.
     */
    private void updateNavigation(Page page) {
        lblUserName.setText(SessionContext.getCurrentUser().getFirstName());
        lblUserInitials.setText(util.UiFormatting.initials(SessionContext.getCurrentUser()));
        navigation.values().forEach(button -> button.getStyleClass().remove("active"));
        Button selected = navigation.get(page);
        if (selected != null && !selected.getStyleClass().contains("active")) {
            selected.getStyleClass().add("active");
        }
    }

    /**
     * Abre a caixa de diálogo modal para criação de um novo grupo de Xitique[cite: 110].
     */
    private void openCreateGroup() {
        ModalDialogs.showCreateGroup(contentArea.getScene().getWindow(), () -> navigate(currentPage));
    }

    /**
     * Encerra a sessão atual do utilizador, limpa o contexto da sessão e redireciona para o ecrã de login[cite: 110].
     */
    private void logout() {
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

    /**
     * Exibe sequencialmente em caixas de diálogo os convites de grupos pendentes, permitindo aceitá-los ou recusá-los[cite: 110].
     */
    private void showInvitations() {
        try {
            List<MembershipInvitation> invitations = groupService.findPendingInvitations(
                    SessionContext.getCurrentUser().getId());
            if (invitations.isEmpty()) {
                StyledDialogs.showAndWait(new Alert(Alert.AlertType.INFORMATION,
                        "Não tem convites pendentes."));
                return;
            }
            for (MembershipInvitation invitation : invitations) {
                Alert dialog = new Alert(Alert.AlertType.CONFIRMATION);
                dialog.setTitle("Convite para grupo");
                dialog.setHeaderText(invitation.getGroup().getGroupName());
                dialog.setContentText(invitation.getInviter().getFirstName()
                        + " convidou-o para participar neste grupo.");
                ButtonType accept = new ButtonType("Aceitar");
                ButtonType decline = new ButtonType("Recusar");
                dialog.getButtonTypes().setAll(accept, decline);
                var response = StyledDialogs.showAndWait(dialog);
                if (response.isPresent()) {
                    groupService.respondToInvitation(invitation.getId(),
                            SessionContext.getCurrentUser().getId(), response.get() == accept);
                }
            }
            refreshInvitationCount();
        } catch (RuntimeException exception) {
            StyledDialogs.showAndWait(new Alert(Alert.AlertType.ERROR, exception.getMessage()));
        }
    }

    /**
     * Atualiza o contador de convites pendentes e altera a visibilidade do badge de notificação[cite: 110].
     */
    private void refreshInvitationCount() {
        int pending = groupService.findPendingInvitations(SessionContext.getCurrentUser().getId()).size();
        lblNotificationCount.setText(Integer.toString(pending));
        lblNotificationCount.setVisible(pending > 0);
    }
}