package controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import model.Contribution;
import model.Cycle;
import model.Group;
import model.UserGroup;
import model.enums.ContributionStatus;
import model.enums.Page;
import service.GroupService;
import util.Navigator;
import util.SessionContext;
import util.UiFormatting;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class GroupDetailController {
    private final GroupService groupService = new GroupService();
    private Group group;
    private Cycle currentCycle;
    private boolean owner;

    @FXML private Button btnBack;
    @FXML private Button btnAddMember;
    @FXML private Button btnNewCycle;
    @FXML private Button btnReorderTop;
    @FXML private Button btnDeleteGroup;
    @FXML private Button btnReorder;
    @FXML private Button btnRegisterPayment;
    @FXML private Label lblGroupName;
    @FXML private Label lblGroupDescription;
    @FXML private Label lblGroupMembers;
    @FXML private Label lblQuota;
    @FXML private Label lblPayDay;
    @FXML private Label lblFrequency;
    @FXML private Label lblCycleTitle;
    @FXML private Label lblCycleStatus;
    @FXML private Label lblCountdown;
    @FXML private Label lblBeneficiary;
    @FXML private Label lblBeneficiaryPosition;
    @FXML private Label lblGroupInitials;
    @FXML private Label lblBeneficiaryInitials;
    @FXML private Label lblPotAmount;
    @FXML private Label lblPotTarget;
    @FXML private Label lblPayoutDate;
    @FXML private Label lblPaidCount;
    @FXML private Label lblPendingCount;
    @FXML private ProgressBar potProgress;
    @FXML private VBox memberQueue;
    @FXML private VBox contributionRows;

    @FXML
    private void initialize() {
        btnBack.setOnAction(event -> Navigator.go(Page.GROUPS));
        Long groupId = Navigator.getSelectedGroupId();
        if (groupId == null) {
            showError("Selecione um grupo para consultar os detalhes.");
            Navigator.go(Page.GROUPS);
            return;
        }
        try {
            group = groupService.findByIdForUser(groupId, SessionContext.getCurrentUser().getId());
            if (group == null) {
                throw new IllegalArgumentException("Grupo não encontrado.");
            }
            owner = group.getCreatedBy().getId().equals(SessionContext.getCurrentUser().getId());
            btnAddMember.setVisible(owner);
            btnAddMember.setManaged(owner);
            btnNewCycle.setVisible(owner);
            btnNewCycle.setManaged(owner);
            btnReorderTop.setVisible(owner);
            btnReorderTop.setManaged(owner);
            btnReorder.setVisible(owner);
            btnReorder.setManaged(owner);
            btnDeleteGroup.setVisible(owner);
            btnDeleteGroup.setManaged(owner);
            btnBack.setOnAction(event -> Navigator.go(Page.GROUPS));
            btnAddMember.setOnAction(event -> showMemberDialog());
            btnNewCycle.setOnAction(event -> startCycle());
            btnReorderTop.setOnAction(event -> reorder());
            btnReorder.setOnAction(event -> reorder());
            btnDeleteGroup.setOnAction(event -> deleteGroup());
            btnRegisterPayment.setOnAction(event -> showPaymentDialog());
            render();
        } catch (RuntimeException e) {
            showError(e.getMessage());
            Navigator.go(Page.GROUPS);
        }
    }

    private void render() {
        List<UserGroup> members = groupService.findMembers(group.getGroupId());
        List<Cycle> cycles = groupService.findCycles(group.getGroupId());
        currentCycle = groupService.findActiveCycle(group.getGroupId());
        lblGroupName.setText(group.getGroupName());
        lblGroupInitials.setText(group.getGroupName().substring(0, 1).toUpperCase());
        lblGroupDescription.setText(group.getGroupDescription());
        lblGroupMembers.setText(members.size() + (members.size() == 1 ? " membro" : " membros"));
        lblQuota.setText(UiFormatting.money(group.getAmountPerUser()));
        lblPayDay.setText("Dia " + group.getPayday());
        lblFrequency.setText(frequencyName(group.getFrequency()));
        renderQueue(members);
        renderCycle(members, cycles);
    }

    private void renderQueue(List<UserGroup> members) {
        memberQueue.getChildren().clear();
        for (UserGroup member : members) {
            HBox row = new HBox(9);
            row.getStyleClass().add("member-row");
            Label order = styled(member.getUserReceivingOrder() + "º", "member-order");
            Label avatar = styled(UiFormatting.initials(member.getUser()), "avatar", "avatar-sm");
            VBox identity = new VBox(4,
                    styled(UiFormatting.fullName(member.getUser()), "member-name"),
                    styled(member.getUser().getPhoneNumber() == null ? "—" : member.getUser().getPhoneNumber(), "member-phone"));
            HBox.setHgrow(identity, javafx.scene.layout.Priority.ALWAYS);
            String status = member.isReceived() ? "Já recebeu" : isBeneficiary(member) ? "Nesta rodada" : "Aguardando";
            String badgeStyle = member.isReceived() ? "badge-success" : isBeneficiary(member) ? "badge-brand" : "badge-neutral";
            row.getChildren().addAll(order, avatar, identity, styled(status, "badge", badgeStyle));
            if (owner && !member.getUser().getId().equals(SessionContext.getCurrentUser().getId())
                    && groupService.findCycles(group.getGroupId()).isEmpty()) {
                Button remove = new Button("Remover");
                remove.setOnAction(event -> removeMember(member));
                row.getChildren().add(remove);
            }
            memberQueue.getChildren().add(row);
        }
        if (members.isEmpty()) {
            memberQueue.getChildren().add(styled("Ainda não há membros neste grupo.", "section-sub"));
        }
    }

    private void renderCycle(List<UserGroup> members, List<Cycle> cycles) {
        Cycle displayedCycle = currentCycle == null && !cycles.isEmpty()
                ? cycles.get(cycles.size() - 1) : currentCycle;
        List<Contribution> contributions = displayedCycle == null
                ? List.of() : groupService.findContributionsByCycle(displayedCycle.getId());
        long paid = contributions.stream().filter(c -> c.getStatus() == ContributionStatus.PAID).count();
        BigDecimal total = contributions.stream().filter(c -> c.getStatus() == ContributionStatus.PAID)
                .map(Contribution::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal target = group.getAmountPerUser().multiply(BigDecimal.valueOf(members.size()));
        lblCycleTitle.setText(currentCycle == null
                ? cycles.isEmpty() ? "Sem ciclos iniciados" : "Último ciclo nº " + cycles.get(cycles.size() - 1).getNumero()
                : "Ciclo nº " + currentCycle.getNumero());
        lblCycleStatus.setText(currentCycle != null ? "EM ANDAMENTO"
                : displayedCycle != null && displayedCycle.getStatus() == model.enums.CycleStatus.CONCLUDED
                ? "CONCLUÍDO" : "SEM CICLO ATIVO");
        lblBeneficiary.setText(displayedCycle == null ? "—" : UiFormatting.fullName(displayedCycle.getBeneficiary().getUser()));
        lblBeneficiaryInitials.setText(displayedCycle == null ? "—"
                : UiFormatting.initials(displayedCycle.getBeneficiary().getUser()));
        lblBeneficiaryPosition.setText(displayedCycle == null ? "Sem beneficiário selecionado"
                : displayedCycle.getBeneficiary().getUserReceivingOrder() + "ª posição na fila");
        lblPotAmount.setText(UiFormatting.money(total));
        lblPotTarget.setText(" de " + UiFormatting.money(target));
        lblPayoutDate.setText(displayedCycle == null ? "—" : UiFormatting.date(displayedCycle.getEndDate()));
        lblCountdown.setText(displayedCycle == null ? "—" : currentCycle == null
                ? "Ciclo encerrado" : countdown(displayedCycle.getEndDate()));
        potProgress.setProgress(target.signum() == 0 ? 0 : Math.min(1, total.doubleValue() / target.doubleValue()));
        lblPaidCount.setText(paid + " de " + members.size() + " membros contribuíram");
        lblPendingCount.setText((members.size() - paid) + " pendentes");
        renderContributions(members, contributions);
    }

    private void renderContributions(List<UserGroup> members, List<Contribution> contributions) {
        contributionRows.getChildren().clear();
        for (UserGroup member : members) {
            Contribution contribution = contributions.stream()
                    .filter(item -> item.getUserGroup().getId().equals(member.getId()))
                    .findFirst().orElse(null);
            HBox row = new HBox();
            row.getStyleClass().add("t-row");
            HBox user = new HBox(9, styled(UiFormatting.initials(member.getUser()), "avatar", "avatar-sm"),
                    styled(UiFormatting.fullName(member.getUser()), "t-name"));
            HBox.setHgrow(user, javafx.scene.layout.Priority.ALWAYS);
            row.getChildren().addAll(
                    user,
                    styled(UiFormatting.money(contribution == null ? group.getAmountPerUser() : contribution.getAmount()), "t-amount", "c-w110"),
                    styled(contribution == null ? "—" : UiFormatting.date(contribution.getPaymentDate()), "t-td", "c-w110"),
                    styled(contribution == null ? "—" : paymentName(contribution), "t-td", "c-w90"),
                    styled(contribution == null ? "PENDENTE" : "PAGO", "badge", contribution == null ? "badge-warning" : "badge-success")
            );
            contributionRows.getChildren().add(row);
        }
        if (members.isEmpty()) {
            contributionRows.getChildren().add(styled("Adicione membros para iniciar as contribuições.", "section-sub"));
        }
    }

    private void startCycle() {
        List<UserGroup> members = groupService.findMembers(group.getGroupId());
        if (members.size() < 2) {
            showError("Adicione pelo menos dois membros antes de abrir um ciclo.");
            return;
        }
        List<UserGroup> eligible = members.stream().filter(member -> !member.isReceived()).toList();
        if (eligible.isEmpty()) {
            eligible = members;
        }
        List<UserGroup> choices = eligible;
        List<String> names = choices.stream().map(member -> member.getUserReceivingOrder() + "º — "
                + UiFormatting.fullName(member.getUser())).toList();
        ChoiceDialog<String> dialog = new ChoiceDialog<>(names.get(0), names);
        dialog.setTitle("Abrir ciclo");
        dialog.setHeaderText("Selecione o membro que receberá nesta rodada.");
        dialog.setContentText("Beneficiário:");
        dialog.showAndWait().ifPresent(name -> {
            UserGroup beneficiary = choices.get(names.indexOf(name));
            try {
                groupService.addNewCycleToGroup(group.getGroupId(), beneficiary.getUser().getId());
                render();
            } catch (RuntimeException e) {
                showError(e.getMessage());
            }
        });
    }

    private void reorder() {
        List<UserGroup> members = groupService.findMembers(group.getGroupId());
        List<String> names = members.stream().map(member -> member.getUserReceivingOrder() + "º — "
                + UiFormatting.fullName(member.getUser())).toList();
        if (names.isEmpty()) {
            return;
        }
        ChoiceDialog<String> dialog = new ChoiceDialog<>(names.get(0), names);
        dialog.setTitle("Ordem de recebimento");
        dialog.setHeaderText("Selecione o membro que ficará em primeiro na fila.");
        dialog.setContentText("Novo primeiro:");
        dialog.showAndWait().ifPresent(name -> {
            UserGroup target = members.get(names.indexOf(name));
            try {
                groupService.setFirstReceiver(group.getGroupId(), target.getUser().getId());
                render();
            } catch (RuntimeException e) {
                showError(e.getMessage());
            }
        });
    }

    private void removeMember(UserGroup member) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                "Remover " + UiFormatting.fullName(member.getUser()) + " deste grupo?");
        confirmation.setHeaderText("Confirmar remoção");
        confirmation.showAndWait().filter(response -> response == javafx.scene.control.ButtonType.OK).ifPresent(response -> {
            try {
                groupService.removeMemberFromGroup(group.getGroupId(), member.getUser().getId());
                render();
            } catch (RuntimeException e) {
                showError(e.getMessage());
            }
        });
    }

    private void deleteGroup() {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                "O grupo será removido definitivamente. Esta ação só é permitida antes de iniciar ciclos.");
        confirmation.setHeaderText("Eliminar " + group.getGroupName() + "?");
        confirmation.showAndWait().filter(response -> response == javafx.scene.control.ButtonType.OK).ifPresent(response -> {
            try {
                groupService.deleteGroup(group.getGroupId(), SessionContext.getCurrentUser().getId());
                Navigator.go(Page.GROUPS);
            } catch (RuntimeException e) {
                showError(e.getMessage());
            }
        });
    }

    private void showMemberDialog() {
        showModal("/view/fxml/modal-member.fxml", (controller, stage) ->
                ((AddMemberController) controller).configure(group.getGroupId(), stage, this::render));
    }

    private void showPaymentDialog() {
        if (currentCycle == null) {
            showError("Abra um ciclo antes de registrar pagamentos.");
            return;
        }
        try {
            boolean paid = PaymentDialogController.show(
                    memberQueue.getScene().getWindow(),
                    group,
                    SessionContext.getCurrentUser()
            );
            if (paid) {
                render();
            }
        } catch (RuntimeException e) {
            showError(e.getMessage());
        }
    }

    private void showModal(String resource, java.util.function.BiConsumer<Object, Stage> configure) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(resource));
            Parent content = loader.load();
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(memberQueue.getScene().getWindow());
            dialog.setScene(new Scene(content));
            configure.accept(loader.getController(), dialog);
            dialog.showAndWait();
        } catch (IOException e) {
            showError("Não foi possível abrir a janela: " + e.getMessage());
        }
    }

    private boolean isBeneficiary(UserGroup member) {
        return currentCycle != null && currentCycle.getBeneficiary().getId().equals(member.getId());
    }

    private String paymentName(Contribution contribution) {
        return switch (contribution.getPaymentMethod()) {
            case MPESA -> "M-Pesa";
            case EMOLA -> "E-Mola";
            case BIM -> "Banco";
            case CASH -> "Dinheiro";
        };
    }

    private String frequencyName(int days) {
        return switch (days) {
            case 7 -> "Semanal";
            case 15 -> "Quinzenal";
            case 30 -> "Mensal";
            default -> days + " dias";
        };
    }

    private String countdown(LocalDate endDate) {
        long days = ChronoUnit.DAYS.between(LocalDate.now(), endDate);
        return days < 0 ? "Atrasado " + Math.abs(days) + " dias" : days + " dias";
    }

    private Label styled(String text, String... classes) {
        Label label = new Label(text);
        label.getStyleClass().addAll(classes);
        return label;
    }

    private void showError(String message) {
        new Alert(Alert.AlertType.ERROR, message).showAndWait();
    }
}
