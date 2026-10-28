package controller;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.converter.NumberStringConverter;
import model.AdminGroupSummary;
import model.AdminLateMemberSummary;
import model.AdminMemberSummary;
import model.AdminUserSummary;
import model.User;
import model.UserReport;
import model.enums.UserRole;
import model.enums.Page;
import service.AdminService;
import util.SessionContext;
import util.StyledDialogs;
import util.UiFormatting;
import util.Navigator;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.List;

/**
 * Controlador JavaFX do painel de administração da plataforma.
 * <p>
 * Oferece funcionalidades de monitorização de métricas globais do sistema,
 * aprovação/rejeição de processos KYC e resolução de denúncias
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
    @FXML private Label lblAtRisk;
    @FXML private Label lblRiskDetail;
    @FXML private PieChart verificationChart;
    @FXML private BarChart<String, Number> activityChart;
    @FXML private HBox activitySummary;
    @FXML private LineChart<String, Number> financeChart;
    @FXML private NumberAxis financeAmountAxis;
    @FXML private VBox kycRows;
    @FXML private VBox reportRows;
    @FXML private VBox userRows;
    @FXML private VBox groupRows;
    @FXML private VBox lateMemberRows;
    @FXML private javafx.scene.control.TextField userSearchField;
    @FXML private javafx.scene.control.DatePicker reportFrom;
    @FXML private javafx.scene.control.DatePicker reportTo;
    @FXML private Button btnExportCsv;
    @FXML private Button btnExportPdf;
    @FXML private javafx.scene.control.TextField penaltyPointsField;
    @FXML private javafx.scene.control.TextField reminderDaysField;
    @FXML private CheckBox remindersEnabledCheck;
    @FXML private CheckBox overdueNotificationsCheck;
    @FXML private CheckBox latePaymentNotificationsCheck;
    @FXML private Button btnSaveSettings;
    private List<AdminUserSummary> allUsers = List.of();
    private List<AdminGroupSummary> allGroups = List.of();
    private Page page;

    /**
     * Inicializa o controlador obtendo o ID do administrador ativo e recarregando os dados do painel.
     */
    @FXML
    private void initialize() {
        administratorId = SessionContext.getCurrentUser().getId();
        page = Navigator.getCurrentPage();
        if (page == null) {
            page = Page.ADMIN;
        }
        if (financeAmountAxis != null) {
            financeAmountAxis.setTickLabelFormatter(
                    new NumberStringConverter(Locale.forLanguageTag("pt-MZ"), "#,##0 'MT'"));
        }
        if (reportFrom != null) {
            reportFrom.setValue(LocalDate.now().withDayOfMonth(1).minusMonths(2));
            reportTo.setValue(LocalDate.now());
            btnExportCsv.setOnAction(event -> exportFinancialCsv());
            btnExportPdf.setOnAction(event -> exportFinancialPdf());
        }
        if (btnSaveSettings != null) {
            btnSaveSettings.setOnAction(event -> saveGlobalSettings());
        }
        if (userSearchField != null) {
            userSearchField.textProperty().addListener((observable, oldValue, newValue) -> {
                if (page == Page.ADMIN_GROUPS) {
                    renderGroups(allGroups);
                } else {
                    filterUsers(newValue);
                }
            });
        }
        refresh();
    }

    /**
     * Recarrega e atualiza todas as métricas, gráficos e tabelas do painel administrativo.
     */
    private void refresh() {
        try {
            switch (page) {
                case ADMIN -> {
                    AdminService.Metrics metrics = adminService.getMetrics(administratorId);
                    lblUsers.setText(Long.toString(metrics.users()));
                    lblVerified.setText(metrics.verifiedUsers() + " verificados · "
                            + metrics.activeUsers() + " contas ativas");
                    lblGroups.setText(metrics.groups() + " grupos · " + metrics.publicGroups() + " públicos");
                    lblPaid.setText(metrics.paidContributions() + " pagamentos confirmados");
                    lblReports.setText(metrics.openReports() + " denúncias abertas");
                    lblContributed.setText(UiFormatting.money(metrics.contributed()));
                    lblAtRisk.setText(UiFormatting.money(metrics.pendingAmount().add(metrics.overdueAmount())));
                    lblRiskDetail.setText(UiFormatting.money(metrics.pendingAmount()) + " pendentes · "
                            + UiFormatting.money(metrics.overdueAmount()) + " atrasados");
                    renderCharts(metrics);
                }
                case ADMIN_REPORTS -> renderLateMembers(adminService.getLateMembers(administratorId));
                case ADMIN_RULES -> loadGlobalSettings(adminService.getGlobalSettings(administratorId));
                case ADMIN_COMPLAINTS -> renderReports(adminService.getOpenReports(administratorId));
                case ADMIN_KYC -> renderKyc(adminService.getPendingKyc(administratorId));
                case ADMIN_USERS -> {
                    allUsers = adminService.getUsers(administratorId);
                    filterUsers(userSearchField.getText());
                }
                case ADMIN_GROUPS -> {
                    allGroups = adminService.getGroups(administratorId);
                    renderGroups(allGroups);
                }
                default -> throw new IllegalStateException("Página administrativa inválida: " + page);
            }
        } catch (RuntimeException exception) {
            StyledDialogs.showAndWait(new Alert(Alert.AlertType.ERROR, exception.getMessage()));
        }
    }

    public void search(String query) {
        if (userSearchField == null) {
            return;
        }
        userSearchField.setText(query == null ? "" : query);
        if (page == Page.ADMIN_GROUPS) {
            renderGroups(allGroups);
        } else {
            filterUsers(userSearchField.getText());
        }
    }

    private void loadGlobalSettings(AdminService.GlobalSettings settings) {
        penaltyPointsField.setText(Integer.toString(settings.latePenaltyPoints()));
        reminderDaysField.setText(Integer.toString(settings.reminderDaysBefore()));
        remindersEnabledCheck.setSelected(settings.remindersEnabled());
        overdueNotificationsCheck.setSelected(settings.overdueNotificationsEnabled());
        latePaymentNotificationsCheck.setSelected(settings.latePaymentNotificationsEnabled());
    }

    private void saveGlobalSettings() {
        try {
            AdminService.GlobalSettings settings = new AdminService.GlobalSettings(
                    Integer.parseInt(penaltyPointsField.getText().trim()),
                    Integer.parseInt(reminderDaysField.getText().trim()),
                    remindersEnabledCheck.isSelected(),
                    overdueNotificationsCheck.isSelected(),
                    latePaymentNotificationsCheck.isSelected());
            adminService.updateGlobalSettings(administratorId, settings);
            StyledDialogs.showAndWait(new Alert(Alert.AlertType.INFORMATION,
                    "Regras globais actualizadas."));
        } catch (NumberFormatException exception) {
            StyledDialogs.showAndWait(new Alert(Alert.AlertType.ERROR,
                    "Introduza valores numéricos válidos para os pontos e dias."));
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
        financeChart.getData().clear();
        XYChart.Series<String, Number> finance = new XYChart.Series<>();
        finance.setName("Arrecadado");
        DateTimeFormatter monthLabel = DateTimeFormatter.ofPattern("MMM/yy", new Locale("pt", "MZ"));
        for (AdminService.MonthlyTotal total : adminService.getFinancialTrend(administratorId)) {
            finance.getData().add(new XYChart.Data<>(
                    total.month().atDay(1).format(monthLabel), total.amount()));
        }
        financeChart.getData().add(finance);
        activityChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        XYChart.Data<String, Number> users = new XYChart.Data<>("Utilizadores", metrics.users());
        XYChart.Data<String, Number> groups = new XYChart.Data<>("Grupos activos", metrics.groups());
        XYChart.Data<String, Number> payments = new XYChart.Data<>("Pagamentos", metrics.paidContributions());
        XYChart.Data<String, Number> reports = new XYChart.Data<>("Denúncias abertas", metrics.openReports());
        series.getData().addAll(users, groups, payments, reports);
        activityChart.getData().add(series);
        styleActivityBar(users, "activity-users");
        styleActivityBar(groups, "activity-groups");
        styleActivityBar(payments, "activity-payments");
        styleActivityBar(reports, "activity-reports");

        activitySummary.getChildren().clear();
        addActivityTotal("Utilizadores", metrics.users(), "activity-users");
        addActivityTotal("Grupos activos", metrics.groups(), "activity-groups");
        addActivityTotal("Pagamentos confirmados", metrics.paidContributions(), "activity-payments");
        addActivityTotal("Denúncias abertas", metrics.openReports(), "activity-reports");
    }

    private void styleActivityBar(XYChart.Data<String, Number> data, String colorClass) {
        data.nodeProperty().addListener((observable, oldNode, newNode) -> applyActivityBarColor(newNode, colorClass));
        applyActivityBarColor(data.getNode(), colorClass);
    }

    private void applyActivityBarColor(Node node, String colorClass) {
        if (node != null && !node.getStyleClass().contains(colorClass)) {
            node.getStyleClass().add(colorClass);
        }
    }

    private void addActivityTotal(String label, long total, String colorClass) {
        VBox item = new VBox(4);
        item.getStyleClass().add("admin-activity-total");
        Label value = new Label(Long.toString(total));
        value.getStyleClass().add("metric-value");
        Label description = new Label(label);
        description.getStyleClass().add("section-sub");
        description.setWrapText(true);
        Circle marker = new Circle(5);
        marker.getStyleClass().addAll("activity-legend-dot", colorClass);
        HBox legend = new HBox(6, marker, description);
        legend.setAlignment(Pos.CENTER_LEFT);
        item.getChildren().addAll(value, legend);
        HBox.setHgrow(item, javafx.scene.layout.Priority.ALWAYS);
        activitySummary.getChildren().add(item);
    }

    private void filterUsers(String query) {
        String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        renderUsers(allUsers.stream().filter(user -> normalized.isBlank()
                || (user.firstName() + " " + user.lastName()).toLowerCase(Locale.ROOT).contains(normalized)
                || user.username().toLowerCase(Locale.ROOT).contains(normalized)).toList());
    }

    private void renderGroups(List<AdminGroupSummary> groups) {
        groupRows.getChildren().clear();
        String query = userSearchField.getText() == null ? ""
                : userSearchField.getText().trim().toLowerCase(Locale.ROOT);
        List<AdminGroupSummary> matchingGroups = groups.stream()
                .filter(group -> query.isBlank()
                        || group.name().toLowerCase(Locale.ROOT).contains(query)
                        || group.managerName().toLowerCase(Locale.ROOT).contains(query))
                .toList();
        for (AdminGroupSummary group : matchingGroups) {
            HBox row = new HBox(10);
            row.getStyleClass().add("admin-row");
            VBox info = new VBox(3);
            Label name = new Label(group.name() + (group.archived() ? " · ARQUIVADO" : ""));
            name.getStyleClass().add("t-name");
            Label summary = new Label("Responsável: " + group.managerName() + " · "
                    + group.memberCount() + " membros · Ciclo " + group.latestCycle()
                    + " · " + UiFormatting.money(group.balance()));
            summary.getStyleClass().add("section-sub");
            info.getChildren().addAll(name, summary);
            info.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(info, javafx.scene.layout.Priority.ALWAYS);
            Button details = new Button("Ver detalhe");
            details.getStyleClass().addAll("btn", "btn-secondary");
            details.setOnAction(event -> showGroupDetails(group.id()));
            row.getChildren().addAll(info, details);
            if (!group.archived()) {
                Button withdrawal = new Button("Intervir em desistência");
                withdrawal.getStyleClass().addAll("btn", "btn-secondary");
                withdrawal.setDisable(group.latestCycle() != null && group.latestCycle() > 0);
                withdrawal.setOnAction(event -> interveneWithdrawal(group));
                row.getChildren().add(withdrawal);
            }
            if (!group.archived()) {
                Button archive = new Button("Arquivar");
                archive.getStyleClass().addAll("btn", "btn-danger");
                archive.setDisable(group.activeCycles() > 0);
                archive.setOnAction(event -> archiveGroup(group));
                row.getChildren().add(archive);
            }
            groupRows.getChildren().add(row);
        }
        addEmptyMessage(groupRows, matchingGroups.isEmpty(), query.isBlank()
                ? "Ainda não existem grupos na plataforma." : "Nenhum grupo corresponde à pesquisa.");
    }

    private void renderLateMembers(List<AdminLateMemberSummary> members) {
        lateMemberRows.getChildren().clear();
        for (AdminLateMemberSummary member : members) {
            Label row = new Label(member.fullName() + " · @" + member.username() + " · "
                    + String.format(Locale.ROOT, "%.1f%%", member.overdueRate()) + " de atrasos ("
                    + member.overdueCount() + "/" + member.totalContributions() + ") · "
                    + UiFormatting.money(member.overdueAmount()));
            row.getStyleClass().add("section-sub");
            row.setWrapText(true);
            lateMemberRows.getChildren().add(row);
        }
        addEmptyMessage(lateMemberRows, members.isEmpty(), "Sem contribuições em atraso.");
    }

    private void showGroupDetails(Long groupId) {
        try {
            Alert details = new Alert(Alert.AlertType.INFORMATION);
            details.setTitle("Detalhe do grupo");
            details.setHeaderText("Consulta administrativa");
            details.setContentText(adminService.getGroupDetails(administratorId, groupId));
            StyledDialogs.showAndWait(details);
        } catch (RuntimeException exception) {
            StyledDialogs.showAndWait(new Alert(Alert.AlertType.ERROR, exception.getMessage()));
        }
    }

    private void archiveGroup(AdminGroupSummary group) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                "Arquivar o grupo \"" + group.name() + "\"? O histórico será preservado.");
        confirmation.setHeaderText("Arquivar grupo inactivo");
        if (StyledDialogs.showAndWait(confirmation).orElse(null) != ButtonType.OK) {
            return;
        }
        try {
            adminService.archiveGroup(administratorId, group.id());
            refresh();
        } catch (RuntimeException exception) {
            StyledDialogs.showAndWait(new Alert(Alert.AlertType.ERROR, exception.getMessage()));
        }
    }

    private void interveneWithdrawal(AdminGroupSummary group) {
        try {
            List<AdminMemberSummary> eligibleMembers = adminService
                    .getGroupMembers(administratorId, group.id()).stream()
                    .filter(member -> !member.groupCreator()
                            && member.balance() != null
                            && member.balance().compareTo(java.math.BigDecimal.ZERO) <= 0)
                    .toList();
            if (eligibleMembers.isEmpty()) {
                StyledDialogs.showAndWait(new Alert(Alert.AlertType.INFORMATION,
                        "Não há membros elegíveis para saída. O responsável do grupo não pode ser removido, " +
                                "e membros com saldo por liquidar devem ser resolvidos primeiro."));
                return;
            }
            ChoiceDialog<AdminMemberSummary> choice = new ChoiceDialog<>(
                    eligibleMembers.getFirst(), eligibleMembers);
            choice.setTitle("Intervenção administrativa");
            choice.setHeaderText("Registar desistência de membro");
            choice.setContentText("Membro (disponível apenas antes do primeiro ciclo):");
            AdminMemberSummary member = StyledDialogs.showAndWait(choice).orElse(null);
            if (member == null) {
                return;
            }
            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                    "Remover " + member + " do grupo \"" + group.name()
                            + "\"? Esta operação só é permitida antes de iniciar ciclos.");
            if (StyledDialogs.showAndWait(confirmation).orElse(null) != ButtonType.OK) {
                return;
            }
            adminService.interveneMemberWithdrawal(administratorId, group.id(), member.id());
            refresh();
        } catch (RuntimeException exception) {
            StyledDialogs.showAndWait(new Alert(Alert.AlertType.ERROR, exception.getMessage()));
        }
    }

    private void exportFinancialCsv() {
        try {
            String csv = adminService.getFinancialCsv(administratorId, reportFrom.getValue(), reportTo.getValue());
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Exportar relatório financeiro");
            chooser.setInitialFileName("relatorio-financeiro-" + reportFrom.getValue()
                    + "-a-" + reportTo.getValue() + ".csv");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
            java.io.File file = chooser.showSaveDialog(userRows.getScene().getWindow());
            if (file != null) {
                Files.writeString(file.toPath(), csv, StandardCharsets.UTF_8);
                StyledDialogs.showAndWait(new Alert(Alert.AlertType.INFORMATION,
                        "Relatório exportado com sucesso."));
            }
        } catch (IOException | RuntimeException exception) {
            StyledDialogs.showAndWait(new Alert(Alert.AlertType.ERROR,
                    "Não foi possível exportar o relatório: " + exception.getMessage()));
        }
    }

    private void exportFinancialPdf() {
        try {
            byte[] pdf = adminService.getFinancialPdf(administratorId, reportFrom.getValue(), reportTo.getValue());
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Exportar relatório financeiro em PDF");
            chooser.setInitialFileName("relatorio-financeiro-" + reportFrom.getValue()
                    + "-a-" + reportTo.getValue() + ".pdf");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
            java.io.File file = chooser.showSaveDialog(userRows.getScene().getWindow());
            if (file != null) {
                Files.write(file.toPath(), pdf);
                StyledDialogs.showAndWait(new Alert(Alert.AlertType.INFORMATION,
                        "Relatório PDF exportado com sucesso."));
            }
        } catch (IOException | RuntimeException exception) {
            StyledDialogs.showAndWait(new Alert(Alert.AlertType.ERROR,
                    "Não foi possível exportar o relatório: " + exception.getMessage()));
        }
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
            row.getStyleClass().add("admin-user-row");

            VBox identity = new VBox(3);
            identity.getStyleClass().add("admin-user-name");
            Label name = new Label(user.firstName() + " " + user.lastName());
            name.getStyleClass().add("t-name");
            Label username = new Label("@" + user.username());
            username.getStyleClass().add("section-sub");
            identity.getChildren().addAll(name, username);
            identity.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(identity, javafx.scene.layout.Priority.ALWAYS);

            Label role = new Label(roleName(user.userRole()));
            role.getStyleClass().addAll("t-td", "admin-user-role");
            Label status = new Label(user.active() ? "ATIVA" : "INATIVA");
            status.getStyleClass().addAll("badge", user.active() ? "badge-success" : "badge-neutral",
                    "admin-user-state");
            Label strikes = new Label(Integer.toString(user.strikeCount()));
            strikes.getStyleClass().addAll("t-td", "admin-user-stat");
            Label points = new Label(Integer.toString(user.punctualityPoints()));
            points.getStyleClass().addAll("t-td", "admin-user-stat");

            HBox actions = new HBox(8);
            actions.getStyleClass().add("admin-user-actions");
            if (!administrator) {
                Button profile = new Button("Perfil");
                profile.getStyleClass().addAll("btn", "btn-secondary", "admin-action");
                profile.setOnAction(event -> showUserProfile(user));

                Button resetPassword = new Button("Redefinir password");
                resetPassword.getStyleClass().addAll("btn", "btn-secondary", "admin-action");
                resetPassword.setOnAction(event -> resetPassword(user));

                Button toggleActive = new Button(user.active() ? "Desativar" : "Reativar");
                toggleActive.getStyleClass().addAll("btn", user.active() ? "btn-danger" : "btn-secondary",
                        "admin-action");
                if (!user.active()) {
                    toggleActive.getStyleClass().add("admin-reactivate");
                }
                toggleActive.setOnAction(event -> {
                    if (user.active()) {
                        disableUser(user.id(), user.username());
                    } else {
                        reactivateUser(user.id(), user.username());
                    }
                });

                Button delete = new Button("Apagar definitivamente");
                delete.getStyleClass().addAll("btn", "btn-danger", "admin-action", "admin-delete");
                delete.setOnAction(event -> deleteUser(user.id(), user.username()));
                actions.getChildren().addAll(profile, resetPassword, toggleActive, delete);
            }

            row.getChildren().addAll(identity, role, status, strikes, points, actions);
            userRows.getChildren().add(row);
        }
        addEmptyMessage(userRows, users.isEmpty(), "Não existem utilizadores registados.");
    }

    private void showUserProfile(AdminUserSummary user) {
        try {
            Alert profile = new Alert(Alert.AlertType.INFORMATION);
            profile.setTitle("Perfil do utilizador");
            profile.setHeaderText(user.firstName() + " " + user.lastName());
            profile.setContentText(adminService.getUserProfile(administratorId, user.id()));
            StyledDialogs.showAndWait(profile);
        } catch (RuntimeException exception) {
            StyledDialogs.showAndWait(new Alert(Alert.AlertType.ERROR, exception.getMessage()));
        }
    }

    private String roleName(UserRole role) {
        return role != null && role.isAdministrator() ? "Administrador" : "Membro";
    }

    private void resetPassword(AdminUserSummary user) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                "Gerar uma palavra-passe temporária para @" + user.username()
                        + "? A palavra-passe actual deixará de funcionar.");
        confirmation.setHeaderText("Redefinir palavra-passe");
        if (StyledDialogs.showAndWait(confirmation).orElse(null) != ButtonType.OK) {
            return;
        }
        try {
            String temporaryPassword = adminService.resetUserPassword(administratorId, user.id());
            Alert result = new Alert(Alert.AlertType.INFORMATION);
            result.setHeaderText("Palavra-passe temporária — mostrar apenas uma vez");
            result.setContentText("Entregue este código ao utilizador por um canal seguro:\n\n"
                    + temporaryPassword);
            StyledDialogs.showAndWait(result);
        } catch (RuntimeException exception) {
            StyledDialogs.showAndWait(new Alert(Alert.AlertType.ERROR, exception.getMessage()));
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
            dialog.initStyle(StageStyle.UNDECORATED);
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

    private void reactivateUser(Long userId, String username) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                "Reativar @" + username + "? A conta poderá voltar a iniciar sessão.");
        confirmation.setHeaderText("Confirmar reativação");
        if (StyledDialogs.showAndWait(confirmation).orElse(null) != ButtonType.OK) {
            return;
        }
        try {
            adminService.reactivateUser(administratorId, userId);
            refresh();
        } catch (RuntimeException exception) {
            StyledDialogs.showAndWait(new Alert(Alert.AlertType.ERROR, exception.getMessage()));
        }
    }

    private void deleteUser(Long userId, String username) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                "Apagar @" + username + " permanentemente? Esta ação não pode ser desfeita. " +
                        "Contas associadas a grupos não podem ser apagadas para preservar o histórico financeiro.");
        confirmation.setHeaderText("Apagar utilizador definitivamente");
        if (StyledDialogs.showAndWait(confirmation).orElse(null) != ButtonType.OK) {
            return;
        }
        try {
            adminService.deleteUser(administratorId, userId);
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