package controller;

import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import model.AdminUserSummary;
import model.LeadershipRequest;
import model.User;
import model.UserReport;
import service.AdminService;
import util.SessionContext;
import util.StyledDialogs;
import util.UiFormatting;

import java.io.ByteArrayInputStream;
import java.util.List;

/**
 * Controlador JavaFX do painel de administração da plataforma.
 * <p>
 * Oferece funcionalidades de monitorização de métricas globais do sistema,
 * aprovação/rejeição de processos KYC, resolução de denúncias, nomeação de chefes de grupo
 * e gestão/desativação de contas de utilizador.
 * </p>

 * @since 1.0
 */
public class AdminDashboardController {

    /** Serviço responsável por operações administrativas. */
    private final AdminService adminService = new AdminService();

    /** Identificador do administrador atualmente autenticado na sessão. */
    private Long administratorId;

    @FXML private Label lblUsers;
    @FXML private Label lblVerified;
    @FXML private Label lblGroups;
    @FXML private Label lblPaid;
    @FXML private Label lblReports;
    @FXML private Label lblContributed;
    @FXML private PieChart verificationChart;
    @FXML private BarChart<String, Number> activityChart;
    @FXML private VBox kycRows;
    @FXML private VBox reportRows;
    @FXML private VBox userRows;
    @FXML private VBox leadershipRows;

    /**
     * Inicializa o controlador obtendo o ID do administrador ativo e recarregando os dados do painel.
     */
    @FXML
    private void initialize() {
        administratorId = SessionContext.getCurrentUser().getId();
        refresh();
    }

    /**
     * Recarrega e atualiza todas as métricas, gráficos e tabelas do painel administrativo.
     */
    private void refresh() {
        try {
            AdminService.Metrics metrics = adminService.getMetrics(administratorId);
            lblUsers.setText(Long.toString(metrics.users()));
            lblVerified.setText(metrics.verifiedUsers() + " verificados de " + metrics.users());
            lblGroups.setText(metrics.groups() + " grupos · " + metrics.publicGroups() + " públicos");
            lblPaid.setText(Long.toString(metrics.paidContributions()));
            lblReports.setText(Long.toString(metrics.openReports()));
            lblContributed.setText(UiFormatting.money(metrics.contributed()));
            renderCharts(metrics);
            lblVerified.setText(metrics.verifiedUsers() + " verificados · "
                    + metrics.activeUsers() + " contas ativas");
            renderKyc(adminService.getPendingKyc(administratorId));
            renderLeadershipRequests(adminService.getLeadershipRequests(administratorId));
            renderReports(adminService.getOpenReports(administratorId));
            renderUsers(adminService.getUsers(administratorId));
        } catch (RuntimeException exception) {
            StyledDialogs.showAndWait(new Alert(Alert.AlertType.ERROR, exception.getMessage()));
        }
    }

    /**
     * Renderiza a lista de pedidos de liderança de grupo pendentes.
     *
     * @param requests Lista de pedidos de liderança a apresentar.
     */
    private void renderLeadershipRequests(List<LeadershipRequest> requests) {
        leadershipRows.getChildren().clear();
        for (LeadershipRequest request : requests) {
            VBox row = new VBox(6);
            row.getStyleClass().add("admin-row");
            Label title = new Label(request.getRequester().getFirstName() + " "
                    + request.getRequester().getLastName() + " · @" + request.getRequester().getUsername());
            title.getStyleClass().add("t-name");
            Label reason = new Label(request.getReason());
            reason.setWrapText(true);
            HBox actions = new HBox(8);
            Button approve = new Button("Nomear chefe");
            approve.getStyleClass().addAll("btn", "btn-primary");
            approve.setOnAction(event -> resolveLeadershipRequest(request, true));
            Button reject = new Button("Recusar pedido");
            reject.getStyleClass().addAll("btn", "btn-secondary");
            reject.setOnAction(event -> resolveLeadershipRequest(request, false));
            actions.getChildren().addAll(approve, reject);
            row.getChildren().addAll(title, reason, actions);
            leadershipRows.getChildren().add(row);
        }
        addEmptyMessage(leadershipRows, requests.isEmpty(), "Não existem pedidos pendentes.");
    }

    /**
     * Resolve um pedido de liderança aprovando-o ou recusando-o.
     *
     * @param request  Pedido de liderança.
     * @param approved {@code true} para aprovar, {@code false} para recusar.
     */
    private void resolveLeadershipRequest(LeadershipRequest request, boolean approved) {
        try {
            adminService.resolveLeadershipRequest(administratorId, request.getId(), approved);
            refresh();
        } catch (RuntimeException exception) {
            StyledDialogs.showAndWait(new Alert(Alert.AlertType.ERROR, exception.getMessage()));
        }
    }

    /**
     * Renderiza os gráficos de estado de verificação (tarte) e atividade geral do sistema (barras).
     *
     * @param metrics Objeto com as métricas calculadas do sistema.
     */
    private void renderCharts(AdminService.Metrics metrics) {
        verificationChart.getData().setAll(
                new PieChart.Data("Verificados", metrics.verifiedUsers()),
                new PieChart.Data("Por verificar", metrics.users() - metrics.verifiedUsers()));
        activityChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Sistema");
        series.getData().add(new XYChart.Data<>("Utilizadores", metrics.users()));
        series.getData().add(new XYChart.Data<>("Grupos", metrics.groups()));
        series.getData().add(new XYChart.Data<>("Pagamentos", metrics.paidContributions()));
        series.getData().add(new XYChart.Data<>("Denúncias", metrics.openReports()));
        activityChart.getData().add(series);
    }

    /**
     * Renderiza os registos de verificação KYC pendentes para análise do administrador.
     *
     * @param users Lista de utilizadores com verificação pendente.
     */
    private void renderKyc(List<User> users) {
        kycRows.getChildren().clear();
        for (User user : users) {
            HBox row = new HBox(10);
            row.getStyleClass().add("admin-row");
            Label identity = new Label(user.getFirstName() + " " + user.getLastName()
                    + " · " + user.getDocumentId());
            identity.getStyleClass().add("t-name");
            HBox.setHgrow(identity, javafx.scene.layout.Priority.ALWAYS);
            Button preview = new Button("Ver fotos");
            preview.getStyleClass().addAll("btn", "btn-secondary");
            preview.setOnAction(event -> showKycImages(user));
            Button approve = new Button("Aprovar");
            approve.getStyleClass().addAll("btn", "btn-primary");
            approve.setOnAction(event -> reviewKyc(user, true));
            Button reject = new Button("Recusar");
            reject.getStyleClass().addAll("btn", "btn-danger");
            reject.setOnAction(event -> reviewKyc(user, false));
            row.getChildren().addAll(identity, preview, approve, reject);
            kycRows.getChildren().add(row);
        }
        addEmptyMessage(kycRows, users.isEmpty(), "Não existem verificações KYC pendentes.");
    }

    /**
     * Renderiza as denúncias de utilizadores enviadas para análise.
     *
     * @param reports Lista de denúncias em aberto.
     */
    private void renderReports(List<UserReport> reports) {
        reportRows.getChildren().clear();
        for (UserReport report : reports) {
            VBox row = new VBox(5);
            row.getStyleClass().add("admin-row");
            row.getChildren().add(new Label(report.getReportedUser().getFirstName() + " "
                    + report.getReportedUser().getLastName() + " · Grupo: "
                    + report.getGroup().getGroupName()));
            Label reason = new Label(report.getReason());
            reason.setWrapText(true);
            reason.getStyleClass().add("section-sub");
            Button strike = new Button("Aplicar strike");
            strike.getStyleClass().addAll("btn", "btn-danger");
            strike.setOnAction(event -> processReport(report, true));
            Button resolve = new Button("Resolver sem strike");
            resolve.getStyleClass().addAll("btn", "btn-secondary");
            resolve.setOnAction(event -> processReport(report, false));
            row.getChildren().addAll(reason, new HBox(8, strike, resolve));
            reportRows.getChildren().add(row);
        }
        addEmptyMessage(reportRows, reports.isEmpty(), "Não existem denúncias por analisar.");
    }

    /**
     * Renderiza a listagem sumária de todos os utilizadores registados na plataforma.
     *
     * @param users Lista com os resumos dos utilizadores.
     */
    private void renderUsers(List<AdminUserSummary> users) {
        userRows.getChildren().clear();
        for (AdminUserSummary user : users) {
            boolean administrator = user.userRole() != null && user.userRole().isAdministrator();
            HBox row = new HBox(10);
            row.getStyleClass().add("admin-row");
            Label identity = new Label(user.firstName() + " " + user.lastName()
                    + " · @" + user.username() + " · strikes: " + user.strikeCount()
                    + " · pontos: " + user.punctualityPoints());
            identity.getStyleClass().add("t-name");
            HBox.setHgrow(identity, javafx.scene.layout.Priority.ALWAYS);
            row.getChildren().add(identity);
            if (!administrator && user.active()) {
                Button disable = new Button("Desativar");
                disable.getStyleClass().addAll("btn", "btn-danger");
                disable.setOnAction(event -> disableUser(user.id(), user.username()));
                row.getChildren().add(disable);
            }
            userRows.getChildren().add(row);
        }
    }

    /**
     * Abre um diálogo modal exibindo as fotografias do documento de identificação e da selfie KYC.
     *
     * @param user Utilizador em análise KYC.
     */
    private void showKycImages(User user) {
        try {
            ImageView document = image(user.getIdentityDocumentImage());
            ImageView selfie = image(user.getIdentitySelfieImage());
            HBox images = new HBox(14, document, selfie);
            images.setStyle("-fx-padding: 18; -fx-alignment: center;");
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(kycRows.getScene().getWindow());
            dialog.setTitle("Documentos KYC · " + user.getUsername());
            Scene scene = new Scene(images, 860, 480);
            scene.getStylesheets().add(getClass().getResource("/view/fxml/app.css").toExternalForm());
            dialog.setScene(scene);
            dialog.showAndWait();
        } catch (RuntimeException exception) {
            StyledDialogs.showAndWait(new Alert(Alert.AlertType.ERROR,
                    "Não foi possível apresentar as fotografias: " + exception.getMessage()));
        }
    }

    /**
     * Converte um array de bytes numa instância de {@link ImageView} formatada.
     *
     * @param bytes Array de bytes da imagem.
     * @return Componente {@link ImageView} configurado.
     * @throws IllegalArgumentException Se os bytes da imagem forem nulos ou vazios.
     */
    private ImageView image(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("Fotografia KYC ausente.");
        }
        ImageView image = new ImageView(new Image(new ByteArrayInputStream(bytes)));
        image.setFitWidth(410);
        image.setFitHeight(440);
        image.setPreserveRatio(true);
        return image;
    }

    /**
     * Finaliza a revisão do processo KYC de um utilizador.
     *
     * @param user     Utilizador avaliado.
     * @param approved {@code true} para aprovar KYC, {@code false} para rejeitar.
     */
    private void reviewKyc(User user, boolean approved) {
        try {
            adminService.resolveKyc(administratorId, user.getId(), approved);
            refresh();
        } catch (RuntimeException exception) {
            StyledDialogs.showAndWait(new Alert(Alert.AlertType.ERROR, exception.getMessage()));
        }
    }

    /**
     * Processa uma denúncia de utilizador, podendo aplicar uma penalização (strike).
     *
     * @param report Denúncia em causa.
     * @param strike {@code true} para aplicar penalização ao utilizador denunciado.
     */
    private void processReport(UserReport report, boolean strike) {
        try {
            if (strike) {
                adminService.strikeUser(administratorId, report.getReportedUser().getId());
            }
            adminService.resolveReport(administratorId, report.getId());
            refresh();
        } catch (RuntimeException exception) {
            StyledDialogs.showAndWait(new Alert(Alert.AlertType.ERROR, exception.getMessage()));
        }
    }

    /**
     * Desativa a conta de um determinado utilizador após confirmação.
     *
     * @param userId   ID do utilizador a desativar.
     * @param username Nome de utilizador (username) a ser exibido no aviso.
     */
    private void disableUser(Long userId, String username) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                "Desativar @" + username + "? A conta deixará de poder iniciar sessão.");
        if (StyledDialogs.showAndWait(confirmation).orElse(null) != javafx.scene.control.ButtonType.OK) {
            return;
        }
        try {
            adminService.disableUser(administratorId, userId);
            refresh();
        } catch (RuntimeException exception) {
            StyledDialogs.showAndWait(new Alert(Alert.AlertType.ERROR, exception.getMessage()));
        }
    }

    /**
     * Adiciona um rótulo indicando ausência de dados caso o contentor esteja vazio.
     *
     * @param parent  Contentor vertical VBox.
     * @param empty   Indica se a lista está vazia.
     * @param message Mensagem explicativa a exibir.
     */
    private void addEmptyMessage(VBox parent, boolean empty, String message) {
        if (empty) {
            Label label = new Label(message);
            label.getStyleClass().add("section-sub");
            parent.getChildren().add(label);
        }
    }
}