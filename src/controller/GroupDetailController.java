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
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import model.Contribution;
import model.Cycle;
import model.Group;
import model.UserGroup;
import model.enums.ContributionStatus;
import model.enums.Page;
import model.enums.PaymentMethod;
import payment.PaymentResponse;
import service.GroupService;
import util.Navigator;
import util.SessionContext;
import util.StyledDialogs;
import util.UiFormatting;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Controlador JavaFX para a vista detalhada de um grupo de Xitique.
 * <p>
 * Gere as operações do grupo, tais como consulta da fila de ordem de recebimento,
 * abertura de novos ciclos, alteração da ordem dos membros, submissão de denúncias,
 * registo de pagamentos e remoção ou eliminação de grupos.
 * </p>

 * @since 1.0
 */
public class GroupDetailController {

    private final GroupService groupService = new GroupService();
    private Group group;
    private Cycle currentCycle;
    private boolean owner;
    private boolean manager;

    @FXML private Button btnBack;
    @FXML private Button btnAddMember;
    @FXML private Button btnNewCycle;
    @FXML private Button btnReorderTop;
    @FXML private Button btnDeleteGroup;
    @FXML private Button btnToggleVisibility;
    @FXML private Button btnReorder;
    @FXML private Button btnRegisterPayment;
    @FXML private Button btnTransferBalance;
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
    @FXML private Label lblGroupBalance;
    @FXML private Label lblPayoutDate;
    @FXML private Label lblPaidCount;
    @FXML private Label lblPendingCount;
    @FXML private ProgressBar potProgress;
    @FXML private VBox memberQueue;
    @FXML private VBox contributionRows;

    /**
     * Inicializa a vista obtendo o grupo selecionado na navegação e ajusta a visibilidade dos controlos
     * conforme as permissões do utilizador (criador do grupo ou administrador).
     */
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
            manager = owner || SessionContext.getCurrentUser().getUserRole() != null
                    && SessionContext.getCurrentUser().getUserRole().isAdministrator();
            btnAddMember.setVisible(manager);
            btnAddMember.setManaged(manager);
            btnNewCycle.setVisible(manager);
            btnNewCycle.setManaged(manager);
            btnReorderTop.setVisible(manager);
            btnReorderTop.setManaged(manager);
            btnReorder.setVisible(manager);
            btnReorder.setManaged(manager);
            boolean canManageGroup = manager;
            btnDeleteGroup.setVisible(canManageGroup);
            btnDeleteGroup.setManaged(canManageGroup);
            btnToggleVisibility.setVisible(canManageGroup);
            btnToggleVisibility.setManaged(canManageGroup);
            btnToggleVisibility.setText(group.isPrivateGroup() ? "Tornar público" : "Tornar privado");
            btnBack.setOnAction(event -> Navigator.go(Page.GROUPS));
            btnAddMember.setOnAction(event -> showMemberDialog());
            btnNewCycle.setOnAction(event -> startCycle());
            btnReorderTop.setOnAction(event -> reorder());
            btnReorder.setOnAction(event -> reorder());
            btnDeleteGroup.setOnAction(event -> deleteGroup());
            btnToggleVisibility.setOnAction(event -> toggleVisibility());
            btnRegisterPayment.setOnAction(event -> showPaymentDialog());
            btnTransferBalance.setOnAction(event -> transferCycleBalance());
            render();
        } catch (RuntimeException e) {
            showError(e.getMessage());
            Navigator.go(Page.GROUPS);
        }
    }

    /**
     * Atualiza todas as informações apresentadas no ecrã de detalhe do grupo.
     */
    private void render() {
        group = groupService.findByIdForUser(group.getGroupId(), SessionContext.getCurrentUser().getId());
        List<UserGroup> members = groupService.findMembers(group.getGroupId());
        List<Cycle> cycles = groupService.findCycles(group.getGroupId());
        currentCycle = groupService.findActiveCycle(group.getGroupId());
        lblGroupName.setText(group.getGroupName());
        lblGroupInitials.setText(UiFormatting.initialsFromName(group.getGroupName()));
        lblGroupDescription.setText(group.getGroupDescription());
        lblGroupMembers.setText(members.size() + (members.size() == 1 ? " membro" : " membros"));
        lblQuota.setText(UiFormatting.money(group.getAmountPerUser()));
        lblPayDay.setText("Dia " + group.getPayday());
        lblFrequency.setText(frequencyName(group.getFrequency()));
        renderQueue(members);
        renderCycle(members, cycles);
    }

    /**
     * Renderiza a fila ordenada de membros do grupo.
     *
     * @param members Lista de membros da associação grupo-utilizador.
     */
    private void renderQueue(List<UserGroup> members) {
        memberQueue.getChildren().clear();
        for (UserGroup member : members) {
            HBox row = new HBox(9);
            row.getStyleClass().add("member-row");
            Label order = styled(member.getUserReceivingOrder() + "º", "member-order");
            StackPane avatar = new StackPane(styled(UiFormatting.initials(member.getUser()), "avatar-initials"));
            avatar.getStyleClass().addAll("avatar", "avatar-sm");
            VBox identity = new VBox(4,
                    styled(UiFormatting.fullName(member.getUser()), "member-name"),
                    styled(member.getUser().getPhoneNumber() == null ? "—" : member.getUser().getPhoneNumber(), "member-phone"),
                    styled(member.getUser().isVerified() ? "Identidade verificada" : "Ainda não verificado",
                            "member-verification", member.getUser().isVerified() ? "verified" : "unverified"));
            HBox.setHgrow(identity, javafx.scene.layout.Priority.ALWAYS);
            String status = member.isReceived() ? "Já recebeu" : isBeneficiary(member) ? "Nesta rodada" : "Aguardando";
            String badgeStyle = member.isReceived() ? "badge-success" : isBeneficiary(member) ? "badge-brand" : "badge-neutral";
            row.getChildren().addAll(order, avatar, identity, styled(status, "badge", badgeStyle));
            if (manager && !member.getUser().getId().equals(SessionContext.getCurrentUser().getId())
                    && groupService.findCycles(group.getGroupId()).isEmpty()) {
                Button remove = new Button("Remover");
                remove.setOnAction(event -> removeMember(member));
                row.getChildren().add(remove);
            }
            if (!member.getUser().getId().equals(SessionContext.getCurrentUser().getId())) {
                Button report = new Button("Denunciar");
                report.getStyleClass().addAll("btn", "btn-ghost", "member-action");
                report.setOnAction(event -> reportMember(member));
                row.getChildren().add(report);
            }
            memberQueue.getChildren().add(row);
        }
        if (members.isEmpty()) {
            memberQueue.getChildren().add(styled("Ainda não há membros neste grupo.", "section-sub"));
        }
    }

    /**
     * Renderiza os dados do ciclo em curso ou concluído e a barra de progresso do fundo coletado.
     *
     * @param members Lista de membros do grupo.
     * @param cycles  Lista de ciclos registados.
     */
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
        BigDecimal groupBalance = groupService.getGroupBalance(group.getGroupId());
        lblGroupBalance.setText(UiFormatting.money(groupBalance));
        boolean canTransfer = manager && displayedCycle != null && groupBalance.signum() > 0;
        btnTransferBalance.setVisible(canTransfer);
        btnTransferBalance.setManaged(canTransfer);
        lblPayoutDate.setText(displayedCycle == null ? "—" : UiFormatting.date(displayedCycle.getEndDate()));
        lblCountdown.setText(displayedCycle == null ? "—" : currentCycle == null
                ? "Ciclo encerrado" : countdown(displayedCycle.getEndDate()));
        potProgress.setProgress(target.signum() == 0 ? 0 : Math.min(1, total.doubleValue() / target.doubleValue()));
        lblPaidCount.setText(paid + " de " + members.size() + " membros contribuíram");
        lblPendingCount.setText((members.size() - paid) + " pendentes");
        renderContributions(members, contributions);
    }

    private void transferCycleBalance() {
        List<Cycle> cycles = currentCycle == null ? groupService.findCycles(group.getGroupId()) : List.of();
        Cycle payoutCycle = currentCycle != null ? currentCycle
                : cycles.isEmpty() ? null : cycles.get(cycles.size() - 1);
        if (payoutCycle == null) {
            showError("Abra um ciclo antes de transferir o saldo.");
            return;
        }

        UserGroup beneficiary = payoutCycle.getBeneficiary();
        String beneficiaryPhone = beneficiary.getUser().getPhoneNumber();
        ChoiceDialog<String> methodDialog = new ChoiceDialog<>("M-Pesa", List.of("M-Pesa", "E-Mola"));
        methodDialog.setTitle("Transferir saldo do ciclo");
        methodDialog.setHeaderText("Beneficiário: " + UiFormatting.fullName(beneficiary.getUser()));
        methodDialog.setContentText("Método de pagamento:");
        StyledDialogs.showAndWait(methodDialog).ifPresent(methodName -> {
            TextInputDialog accountDialog = new TextInputDialog(beneficiaryPhone == null ? "" : beneficiaryPhone);
            accountDialog.setTitle("Número do beneficiário");
            accountDialog.setHeaderText("O saldo será enviado para o beneficiário do ciclo.");
            accountDialog.setContentText("Número M-Pesa/E-Mola:");
            StyledDialogs.showAndWait(accountDialog).ifPresent(account -> {
                try {
                    PaymentMethod method = "M-Pesa".equals(methodName)
                            ? PaymentMethod.MPESA : PaymentMethod.EMOLA;
                    PaymentResponse response = groupService.transferCycleBalanceToBeneficiary(
                            group.getGroupId(), SessionContext.getCurrentUser().getId(),
                            payoutCycle.getId(), method, account);
                    if (response.success()) {
                        StyledDialogs.showAndWait(new Alert(Alert.AlertType.INFORMATION,
                                "Transferência concluída para " + UiFormatting.fullName(beneficiary.getUser())
                                        + ". " + response.message()));
                        render();
                    } else {
                        showError("Não foi possível transferir o saldo: " + response.message());
                    }
                } catch (RuntimeException exception) {
                    showError(exception.getMessage());
                }
            });
        });
    }

    /**
     * Renderiza o estado das contribuições individuais dos membros para o ciclo ativo.
     *
     * @param members       Lista de membros do grupo.
     * @param contributions Contribuições já efetuadas.
     */
    private void renderContributions(List<UserGroup> members, List<Contribution> contributions) {
        contributionRows.getChildren().clear();
        for (UserGroup member : members) {
            Contribution contribution = contributions.stream()
                    .filter(item -> item.getUserGroup().getId().equals(member.getId()))
                    .findFirst().orElse(null);
            HBox row = new HBox();
            row.getStyleClass().add("t-row");
            StackPane avatar = new StackPane(styled(UiFormatting.initials(member.getUser()), "avatar-initials"));
            avatar.getStyleClass().addAll("avatar", "avatar-sm");
            HBox user = new HBox(9, avatar, styled(UiFormatting.fullName(member.getUser()), "t-name"));
            user.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(user, javafx.scene.layout.Priority.ALWAYS);
            HBox statusCell = new HBox(styled(contribution == null ? "PENDENTE" : "PAGO", "badge",
                    contribution == null ? "badge-warning" : "badge-success"));
            statusCell.getStyleClass().add("c-w110");
            statusCell.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            HBox actionCell = new HBox();
            actionCell.getStyleClass().add("c-w40");
            row.getChildren().addAll(
                    user,
                    styled(UiFormatting.money(contribution == null ? group.getAmountPerUser() : contribution.getAmount()), "t-amount", "c-w110"),
                    styled(contribution == null ? "—" : UiFormatting.date(contribution.getPaymentDate()), "t-td", "c-w110"),
                    styled(contribution == null ? "—" : paymentName(contribution), "t-td", "c-w90"),
                    statusCell,
                    actionCell
            );
            contributionRows.getChildren().add(row);
        }
        if (members.isEmpty()) {
            contributionRows.getChildren().add(styled("Adicione membros para iniciar as contribuições.", "section-sub"));
        }
    }

    /**
     * Inicia um novo ciclo de rotação permitindo a seleção do beneficiário elegível.
     */
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
        StyledDialogs.showAndWait(dialog).ifPresent(name -> {
            UserGroup beneficiary = choices.get(names.indexOf(name));
            try {
                groupService.addNewCycleToGroup(group.getGroupId(), SessionContext.getCurrentUser().getId(),
                        beneficiary.getUser().getId());
                render();
            } catch (RuntimeException e) {
                showError(e.getMessage());
            }
        });
    }

    /**
     * Reordena a fila de rotação, promovendo um membro selecionado para a primeira posição.
     */
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
        StyledDialogs.showAndWait(dialog).ifPresent(name -> {
            UserGroup target = members.get(names.indexOf(name));
            try {
                groupService.setFirstReceiver(group.getGroupId(), SessionContext.getCurrentUser().getId(),
                        target.getUser().getId());
                render();
            } catch (RuntimeException e) {
                showError(e.getMessage());
            }
        });
    }

    /**
     * Remove um determinado membro do grupo mediante confirmação.
     *
     * @param member Instância do membro a remover.
     */
    private void removeMember(UserGroup member) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                "Remover " + UiFormatting.fullName(member.getUser()) + " deste grupo?");
        confirmation.setHeaderText("Confirmar remoção");
        StyledDialogs.showAndWait(confirmation).filter(response -> response == javafx.scene.control.ButtonType.OK).ifPresent(response -> {
            try {
                groupService.removeMemberFromGroup(group.getGroupId(), SessionContext.getCurrentUser().getId(),
                        member.getUser().getId());
                render();
            } catch (RuntimeException e) {
                showError(e.getMessage());
            }
        });
    }

    /**
     * Elimina permanentemente o grupo de Xitique (apenas permitido caso não existam ciclos iniciados).
     */
    private void deleteGroup() {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                "O grupo será removido definitivamente. Esta ação só é permitida antes de iniciar ciclos.");
        confirmation.setHeaderText("Eliminar " + group.getGroupName() + "?");
        StyledDialogs.showAndWait(confirmation).filter(response -> response == javafx.scene.control.ButtonType.OK).ifPresent(response -> {
            try {
                groupService.deleteGroup(group.getGroupId(), SessionContext.getCurrentUser().getId());
                Navigator.go(Page.GROUPS);
            } catch (RuntimeException e) {
                showError(e.getMessage());
            }
        });
    }

    /**
     * Alterna a visibilidade do grupo entre público e privado.
     */
    private void toggleVisibility() {
        try {
            groupService.setGroupPrivate(group.getGroupId(), SessionContext.getCurrentUser().getId(),
                    !group.isPrivateGroup());
            group = groupService.findByIdForUser(group.getGroupId(), SessionContext.getCurrentUser().getId());
            btnToggleVisibility.setText(group.isPrivateGroup() ? "Tornar público" : "Tornar privado");
        } catch (RuntimeException exception) {
            showError(exception.getMessage());
        }
    }

    /**
     * Abre uma caixa de diálogo para que um membro do grupo possa ser denunciado à administração.
     *
     * @param member Membro a ser denunciado.
     */
    private void reportMember(UserGroup member) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Denunciar membro");
        dialog.setHeaderText("Denúncia sobre " + UiFormatting.fullName(member.getUser()));
        dialog.setContentText("Descreva a conduta (mínimo 10 caracteres):");
        dialog.getEditor().setPromptText("Motivo da denúncia");
        StyledDialogs.showAndWait(dialog).ifPresent(reason -> {
            try {
                groupService.reportMember(group.getGroupId(), SessionContext.getCurrentUser().getId(),
                        member.getUser().getId(), reason);
                StyledDialogs.showAndWait(new Alert(Alert.AlertType.INFORMATION,
                        "A denúncia foi enviada ao administrador."));
            } catch (RuntimeException exception) {
                showError(exception.getMessage());
            }
        });
    }

    /** Exibe o modal para convite de novos membros. */
    private void showMemberDialog() {
        showModal("/view/fxml/modal-member.fxml", (controller, stage) ->
                ((AddMemberController) controller).configure(group.getGroupId(), stage, this::render));
    }

    /** Exibe o modal de pagamento de quotas para o ciclo ativo. */
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

    /**
     * Carrega e abre uma janela modal FXML genérica.
     *
     * @param resource  Caminho do recurso FXML.
     * @param configure Consumer para configuração do controlador e do stage.
     */
    private void showModal(String resource, java.util.function.BiConsumer<Object, Stage> configure) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(resource));
            Parent content = loader.load();
            Stage dialog = new Stage();
            dialog.initStyle(StageStyle.UNDECORATED);
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(memberQueue.getScene().getWindow());
            dialog.setScene(new Scene(content));
            configure.accept(loader.getController(), dialog);
            dialog.showAndWait();
        } catch (IOException e) {
            showError("Não foi possível abrir a janela: " + e.getMessage());
        }
    }

    /**
     * Verifica se determinado membro é o beneficiário do ciclo em andamento.
     *
     * @param member Membro a avaliar.
     * @return {@code true} se for o beneficiário atual.
     */
    private boolean isBeneficiary(UserGroup member) {
        return currentCycle != null && currentCycle.getBeneficiary().getId().equals(member.getId());
    }

    /**
     * Traduz o método de pagamento para uma representação amigável.
     *
     * @param contribution Entidade da contribuição.
     * @return Nome legível do método de pagamento.
     */
    private String paymentName(Contribution contribution) {
        return switch (contribution.getPaymentMethod()) {
            case MPESA -> "M-Pesa";
            case EMOLA -> "E-Mola";
            case BIM -> "Banco";
            case CASH -> "Dinheiro";
        };
    }

    /**
     * Converte o intervalo de dias na designação da frequência.
     *
     * @param days Frequência em dias.
     * @return Nome da frequência.
     */
    private String frequencyName(int days) {
        return switch (days) {
            case 1 -> "Diário";
            case 7 -> "Semanal";
            case 15 -> "Quinzenal";
            case 30 -> "Mensal";
            default -> days + " dias";
        };
    }

    /**
     * Calcula o texto da contagem decrescente em dias até ao limite do pagamento.
     *
     * @param endDate Data limite.
     * @return Texto informativo dos dias restantes.
     */
    private String countdown(LocalDate endDate) {
        long days = ChronoUnit.DAYS.between(LocalDate.now(), endDate);
        return days < 0 ? "Atrasado " + Math.abs(days) + " dias" : days + " dias";
    }

    /**
     * Instancia um {@link Label} com estilos CSS aplicados.
     *
     * @param text    Texto a apresentar.
     * @param classes Nomes das classes CSS.
     * @return Rótulo configurado.
     */
    private Label styled(String text, String... classes) {
        Label label = new Label(text);
        label.getStyleClass().addAll(classes);
        return label;
    }

    /** Apresenta um alerta de erro estilizado. */
    private void showError(String message) {
        StyledDialogs.showAndWait(new Alert(Alert.AlertType.ERROR, message));
    }
}