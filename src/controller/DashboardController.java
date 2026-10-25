package controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.geometry.Pos;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.converter.NumberStringConverter;
import model.Contribution;
import model.Cycle;
import model.Group;
import model.enums.Page;
import service.GroupService;
import util.Navigator;
import util.SessionContext;
import util.UiFormatting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Controlador JavaFX da vista principal (Dashboard/Visão Geral) do utilizador.
 * <p>
 * Exibe resumos financeiros pessoais (total investido, estimativa de próximo recebimento),
 * cartões de acesso rápido aos grupos ativos e histórico recente de pagamentos.
 * </p>

 * @since 1.0
 */
public class DashboardController {

    /** Serviço responsável pelo acesso a dados de grupos e contribuições. */
    private final GroupService groupService = new GroupService();

    @FXML private Label lblGreeting;
    @FXML private Label lblToday;
    @FXML private Label lblTotalInvested;
    @FXML private Label lblNextPayout;
    @FXML private Label lblActiveGroups;
    @FXML private Button btnSeeAllGroups;
    @FXML private Button btnSeeAllActivity;
    @FXML private LineChart<String, Number> savingsChart;
    @FXML private NumberAxis savingsAmountAxis;
    @FXML private VBox cardGroup1;
    @FXML private HBox activityList;

    /**
     * Inicializa a vista do painel principal calculando os acumulados de pagamentos e prevendo recebimentos.
     */
    @FXML
    private void initialize() {
        model.User user = SessionContext.getCurrentUser();
        lblGreeting.setText("Olá, " + user.getFirstName());
        lblToday.setText("Resumo dos seus Xitiques para " + UiFormatting.date(LocalDate.now()) + ".");
        btnSeeAllGroups.setOnAction(event -> Navigator.go(Page.GROUPS));
        btnSeeAllActivity.setOnAction(event -> Navigator.go(Page.HISTORY));
        savingsAmountAxis.setTickLabelFormatter(
                new NumberStringConverter(Locale.forLanguageTag("pt-MZ"), "#,##0 'MT'"));

        List<Group> groups = groupService.findAllForUser(user.getId());
        List<Contribution> contributions = groupService.findContributionsForUser(user.getId());
        BigDecimal invested = contributions.stream()
                .filter(contribution -> contribution.getStatus() == model.enums.ContributionStatus.PAID)
                .map(Contribution::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        lblTotalInvested.setText(UiFormatting.money(invested));
        renderSavingsChart(contributions);
        lblActiveGroups.setText(groups.size() + (groups.size() == 1 ? " grupo" : " grupos"));
        BigDecimal expectedPayout = groups.stream()
                .filter(group -> {
                    Cycle activeCycle = groupService.findActiveCycle(group.getGroupId());
                    return activeCycle != null && activeCycle.getBeneficiary().getUser().getId().equals(user.getId());
                })
                .map(group -> group.getAmountPerUser().multiply(BigDecimal.valueOf(group.getMembers().size())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        lblNextPayout.setText(UiFormatting.money(expectedPayout));

        GridPane cards = (GridPane) cardGroup1.getParent();
        cards.getChildren().clear();
        int index = 0;
        for (Group group : groups.stream().limit(3).toList()) {
            List<Cycle> cycles = groupService.findCycles(group.getGroupId());
            Cycle latest = cycles.isEmpty() ? null : cycles.get(cycles.size() - 1);
            int paid = latest == null ? 0 : (int) groupService.findContributionsByCycle(latest.getId()).stream()
                    .filter(contribution -> contribution.getStatus() == model.enums.ContributionStatus.PAID).count();
            cards.add(GroupCards.create(group, group.getMembers().size(), latest == null ? 0 : latest.getNumero(),
                    paid, selected -> {
                        Navigator.setSelectedGroupId(selected.getGroupId());
                        Navigator.go(Page.GROUP_DETAIL);
                    }), index % 3, index / 3);
            index++;
        }
        if (groups.isEmpty()) {
            Label empty = new Label("Crie ou entre num grupo para acompanhar os seus Xitiques.");
            empty.getStyleClass().add("section-sub");
            cards.add(empty, 0, 0, 3, 1);
        }
        renderRecentActivity(contributions);
    }

    private void renderSavingsChart(List<Contribution> contributions) {
        YearMonth firstMonth = YearMonth.now().minusMonths(5);
        Map<YearMonth, BigDecimal> monthlyTotals = new TreeMap<>();
        for (int month = 0; month < 6; month++) {
            monthlyTotals.put(firstMonth.plusMonths(month), BigDecimal.ZERO);
        }

        BigDecimal totalBeforePeriod = BigDecimal.ZERO;
        for (Contribution contribution : contributions) {
            if (contribution.getStatus() != model.enums.ContributionStatus.PAID
                    || contribution.getPaymentDate() == null) {
                continue;
            }
            YearMonth paymentMonth = YearMonth.from(contribution.getPaymentDate());
            if (paymentMonth.isBefore(firstMonth)) {
                totalBeforePeriod = totalBeforePeriod.add(contribution.getAmount());
            } else if (monthlyTotals.containsKey(paymentMonth)) {
                monthlyTotals.merge(paymentMonth, contribution.getAmount(), BigDecimal::add);
            }
        }

        XYChart.Series<String, Number> savings = new XYChart.Series<>();
        savings.setName("Total poupado");
        BigDecimal cumulative = totalBeforePeriod;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM/yy", new Locale("pt", "MZ"));
        for (Map.Entry<YearMonth, BigDecimal> month : monthlyTotals.entrySet()) {
            cumulative = cumulative.add(month.getValue());
            savings.getData().add(new XYChart.Data<>(month.getKey().atDay(1).format(formatter), cumulative));
        }
        savingsChart.getData().setAll(savings);
    }

    /**
     * Renderiza a lista horizontal de atividades/contribuições recentes do utilizador.
     *
     * @param contributions Lista de contribuições do utilizador.
     */
    private void renderRecentActivity(List<Contribution> contributions) {
        activityList.getChildren().clear();
        VBox rows = new VBox();
        rows.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(rows, Priority.ALWAYS);
        for (Contribution contribution : contributions.stream().limit(4).toList()) {
            HBox row = new HBox(16);
            row.getStyleClass().add("t-row");
            Label date = new Label(UiFormatting.date(contribution.getPaymentDate()));
            date.getStyleClass().addAll("t-td", "c-w150");
            Label groupName = new Label(contribution.getUserGroup().getGroup().getGroupName());
            groupName.getStyleClass().add("t-name");
            groupName.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(groupName, Priority.ALWAYS);
            Label amount = new Label(UiFormatting.money(contribution.getAmount()));
            amount.getStyleClass().addAll("t-amount", "c-w130");
            Label status = new Label(contribution.getStatus().toString());
            status.getStyleClass().addAll("badge",
                    contribution.getStatus() == model.enums.ContributionStatus.PAID
                            ? "badge-success" : "badge-warning");
            HBox statusCell = new HBox(status);
            statusCell.getStyleClass().add("c-w110");
            statusCell.setAlignment(Pos.CENTER_LEFT);
            HBox actionCell = new HBox();
            actionCell.getStyleClass().add("c-w40");
            row.getChildren().addAll(date, groupName, amount, statusCell, actionCell);
            rows.getChildren().add(row);
        }
        if (contributions.isEmpty()) {
            Label empty = new Label("Ainda não existem contribuições registadas.");
            empty.getStyleClass().add("section-sub");
            rows.getChildren().add(empty);
        }
        activityList.getChildren().add(rows);
    }
}