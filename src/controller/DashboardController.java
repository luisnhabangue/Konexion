package controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
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
import java.util.List;

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

        List<Group> groups = groupService.findAllForUser(user.getId());
        BigDecimal invested = groupService.findContributionsForUser(user.getId()).stream()
                .filter(contribution -> contribution.getStatus() == model.enums.ContributionStatus.PAID)
                .map(Contribution::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        lblTotalInvested.setText(UiFormatting.money(invested));
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
        renderRecentActivity(groupService.findContributionsForUser(user.getId()));
    }

    /**
     * Renderiza a lista horizontal de atividades/contribuições recentes do utilizador.
     *
     * @param contributions Lista de contribuições do utilizador.
     */
    private void renderRecentActivity(List<Contribution> contributions) {
        activityList.getChildren().clear();
        for (Contribution contribution : contributions.stream().limit(4).toList()) {
            VBox item = new VBox(4);
            item.getStyleClass().add("activity-item");
            Label groupName = new Label(contribution.getUserGroup().getGroup().getGroupName());
            groupName.getStyleClass().add("t-name");
            Label details = new Label(UiFormatting.date(contribution.getPaymentDate()) + " · "
                    + UiFormatting.money(contribution.getAmount()));
            details.getStyleClass().add("t-td");
            item.getChildren().addAll(groupName, details);
            activityList.getChildren().add(item);
        }
        if (contributions.isEmpty()) {
            Label empty = new Label("Ainda não existem contribuições registadas.");
            empty.getStyleClass().add("section-sub");
            activityList.getChildren().add(empty);
        }
    }
}