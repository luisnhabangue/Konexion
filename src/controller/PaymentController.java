package controller;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import model.UserGroup;
import model.enums.PaymentMethod;
import service.GroupService;
import util.SessionContext;
import util.StyledDialogs;
import util.UiFormatting;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Controlador JavaFX para o registo manual de pagamentos de contribuições dos membros[cite: 111].
 * <p>
 * Permite selecionar o membro, o valor, a data e o método de pagamento (M-Pesa, E-Mola, BIM, Dinheiro)
 * para registo no ciclo ativo de um grupo[cite: 111].
 * </p>
 *
 * @author Sistema Xitique
 * @version 1.0
 * @since 1.0
 */
public class PaymentController {

    /** Mapeamento de descrições dos métodos de pagamento para os respetivos enums {@link PaymentMethod}[cite: 111]. */
    private static final Map<String, PaymentMethod> METHODS = Map.of(
            "M-Pesa", PaymentMethod.MPESA,
            "E-Mola", PaymentMethod.EMOLA,
            "Transferência bancária", PaymentMethod.BIM,
            "Dinheiro em espécie", PaymentMethod.CASH
    );

    /** Serviço responsável pelas operações de grupos e pagamentos[cite: 111]. */
    private final GroupService groupService = new GroupService();

    /** Identificador único do grupo[cite: 111]. */
    private Long groupId;

    /** Instância do palco (Stage) da janela modal[cite: 111]. */
    private Stage dialog;

    /** Callback executado após a gravação com sucesso do pagamento[cite: 111]. */
    private Runnable afterSave;

    /** Seletor do membro a registar o pagamento[cite: 111]. */
    @FXML private ComboBox<UserGroup> cmbMember;

    /** Campo de texto informativo com o número do ciclo ativo[cite: 111]. */
    @FXML private TextField txtCycle;

    /** Campo de texto para introdução do valor da contribuição[cite: 111]. */
    @FXML private TextField txtAmount;

    /** Seletor do método de pagamento utilizado[cite: 111]. */
    @FXML private ComboBox<String> cmbMethod;

    /** Seletor de data para o registo da data de pagamento[cite: 111]. */
    @FXML private DatePicker dpPaymentDate;

    /** Botão de fecho da janela no cabeçalho[cite: 111]. */
    @FXML private Button btnClose;

    /** Botão de cancelamento da operação[cite: 111]. */
    @FXML private Button btnCancel;

    /** Botão de confirmação e gravação do pagamento[cite: 111]. */
    @FXML private Button btnConfirm;

    /**
     * Inicializa os componentes e seletores da interface, configurando o formato do nome dos membros
     * e desativando a seleção de datas futuras[cite: 111].
     */
    @FXML
    private void initialize() {
        cmbMember.setConverter(new StringConverter<>() {
            @Override
            public String toString(UserGroup member) {
                return member == null ? "" : UiFormatting.fullName(member.getUser());
            }

            @Override
            public UserGroup fromString(String value) {
                return null;
            }
        });
        dpPaymentDate.setValue(LocalDate.now());
        dpPaymentDate.setDayCellFactory(picker -> new javafx.scene.control.DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isAfter(LocalDate.now()));
            }
        });
        btnClose.setOnAction(event -> close());
        btnCancel.setOnAction(event -> close());
        btnConfirm.setOnAction(event -> savePayment());
    }

    /**
     * Configura o contexto do grupo, a janela modal e os dados predefinidos do pagamento[cite: 111].
     *
     * @param groupId   Identificador do grupo.
     * @param dialog    Instância da janela modal {@link Stage}.
     * @param afterSave Ação a executar após guardar o pagamento.
     */
    public void configure(Long groupId, Stage dialog, Runnable afterSave) {
        this.groupId = groupId;
        this.dialog = dialog;
        this.afterSave = afterSave;
        List<UserGroup> members = groupService.findMembers(groupId);
        cmbMember.getItems().setAll(members);
        members.stream()
                .filter(member -> member.getUser().getId().equals(SessionContext.getCurrentUser().getId()))
                .findFirst()
                .ifPresent(cmbMember::setValue);
        model.Cycle activeCycle = groupService.findActiveCycle(groupId);
        txtCycle.setText(activeCycle == null ? "Sem ciclo ativo" : "Ciclo nº " + activeCycle.getNumero());
        model.Group group = groupService.findByIdWithMembers(groupId);
        txtAmount.setText(group.getAmountPerUser().toPlainString());
        if (activeCycle == null) {
            btnConfirm.setDisable(true);
        }
    }

    /**
     * Valida os campos preenchidos e regista a nova contribuição financeira no ciclo ativo[cite: 111].
     */
    private void savePayment() {
        try {
            UserGroup member = cmbMember.getValue();
            PaymentMethod method = METHODS.get(cmbMethod.getValue());
            if (member == null || method == null || dpPaymentDate.getValue() == null) {
                throw new IllegalArgumentException("Selecione o membro, método e data do pagamento.");
            }
            java.math.BigDecimal amount = UiFormatting.parseMoney(txtAmount.getText());
            groupService.addContributionToCycle(groupId, member.getUser().getId(), amount, method, dpPaymentDate.getValue());
            afterSave.run();
            close();
        } catch (NumberFormatException e) {
            showError("Introduza um valor válido.");
        } catch (RuntimeException e) {
            showError(e.getMessage());
        }
    }

    /**
     * Exibe um diálogo estilizado com a mensagem de erro recebida[cite: 111].
     *
     * @param message Mensagem de erro a apresentar.
     */
    private void showError(String message) {
        StyledDialogs.showAndWait(new Alert(Alert.AlertType.ERROR, message));
    }

    /**
     * Fecha a janela modal atual[cite: 111].
     */
    private void close() {
        if (dialog != null) {
            dialog.close();
        }
    }
}