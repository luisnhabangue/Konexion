package controller;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import model.Group;
import model.enums.ReceivingOrderMode;
import service.GroupService;
import util.SessionContext;
import util.StyledDialogs;
import util.UiFormatting;

import java.time.LocalDate;
import java.util.Map;

/**
 * Controlador JavaFX do formulário modal de criação de novos grupos de Xitique.
 * <p>
 * Recolhe e valida a designação do grupo, descrição, valor da quota por membro,
 * dia limite de pagamento e frequência das rotações (diária, semanal, quinzenal ou mensal).
 * </p>

 * @since 1.0
 */
public class CreateGroupController {

    /** Mapeamento de descrições legíveis para o número correspondente de dias da frequência. */
    private static final Map<String, Integer> FREQUENCIES = Map.of(
            "Diário (1 dia)", 1,
            "Semanal (7 dias)", 7,
            "Quinzenal (15 dias)", 15,
            "Mensal (30 dias)", 30
    );
    private static final Map<String, ReceivingOrderMode> ORDER_MODES = Map.of(
            "Manual", ReceivingOrderMode.MANUAL,
            "Aleatória", ReceivingOrderMode.RANDOM
    );

    /** Serviço responsável pelas operações de gestão de grupos. */
    private final GroupService groupService = new GroupService();

    @FXML private TextField txtName;
    @FXML private TextArea txtDescription;
    @FXML private TextField txtAmount;
    @FXML private TextField txtPayDay;
    @FXML private ComboBox<String> cmbFrequency;
    @FXML private TextField txtMemberCount;
    @FXML private ComboBox<String> cmbOrderMode;
    @FXML private Label lblPayoutEstimate;
    @FXML private Button btnClose;
    @FXML private Button btnCancel;
    @FXML private Button btnConfirm;

    /** Instância da janela modal associada. */
    private Stage dialog;

    /**
     * Configura as ações dos botões aquando da inicialização da vista FXML.
     */
    @FXML
    private void initialize() {
        btnClose.setOnAction(event -> close());
        btnCancel.setOnAction(event -> close());
        btnConfirm.setOnAction(event -> createGroup());
        txtMemberCount.textProperty().addListener((observable, oldValue, value) -> updatePayoutEstimate());
        txtAmount.textProperty().addListener((observable, oldValue, value) -> updatePayoutEstimate());
        cmbOrderMode.valueProperty().addListener((observable, oldValue, value) -> updatePayoutEstimate());
        cmbOrderMode.setValue("Manual");
    }

    /**
     * Define a janela modal (Stage) vinculada a este formulário.
     *
     * @param dialog Instância da janela modal.
     */
    public void setDialog(Stage dialog) {
        this.dialog = dialog;
    }

    /**
     * Recolhe os dados dos campos da interface, valida os formatos e solicita a criação do novo grupo.
     */
    private void createGroup() {
        try {
            String frequencyLabel = cmbFrequency.getValue();
            Integer frequency = FREQUENCIES.get(frequencyLabel);
            if (frequency == null) {
                throw new IllegalArgumentException("Selecione a frequência do grupo.");
            }
            java.math.BigDecimal amount = UiFormatting.parseMoney(txtAmount.getText());
            int payday = Integer.parseInt(txtPayDay.getText().trim());
            int maxMembers = Integer.parseInt(txtMemberCount.getText().trim());
            ReceivingOrderMode orderMode = ORDER_MODES.get(cmbOrderMode.getValue());
            if (orderMode == null) {
                throw new IllegalArgumentException("Selecione a ordem de recebimento.");
            }

            Group group = new Group(
                    null,
                    txtName.getText(),
                    txtDescription.getText(),
                    amount,
                    payday,
                    LocalDate.now(),
                    SessionContext.getCurrentUser(),
                    frequency
            );
            group.setMaxMembers(maxMembers);
            group.setCapacityConfigured(true);
            group.setReceivingOrderMode(orderMode);
            groupService.createNewGroup(group);
            close();
        } catch (NumberFormatException e) {
            showError("Introduza valores válidos para a contribuição, o dia de pagamento e o total de membros.");
        } catch (RuntimeException e) {
            showError(e.getMessage());
        }
    }

    private void updatePayoutEstimate() {
        try {
            int members = Integer.parseInt(txtMemberCount.getText().trim());
            java.math.BigDecimal amount = UiFormatting.parseMoney(txtAmount.getText());
            String orderDescription = "Aleatória".equals(cmbOrderMode.getValue())
                    ? "A ordem será sorteada quando o grupo atingir a lotação."
                    : "A ordem segue a fila e pode ser reorganizada antes do primeiro ciclo.";
            java.math.BigDecimal cyclePot = amount.multiply(java.math.BigDecimal.valueOf(members));
            java.math.BigDecimal totalAcrossCycles = cyclePot.multiply(java.math.BigDecimal.valueOf(members));
            lblPayoutEstimate.setText("Cada ciclo arrecada " + UiFormatting.money(cyclePot)
                    + "; em " + members + " ciclos, o grupo movimentará "
                    + UiFormatting.money(totalAcrossCycles) + ". Cada membro recebe "
                    + UiFormatting.money(cyclePot) + " uma vez. " + orderDescription);
        } catch (RuntimeException exception) {
            lblPayoutEstimate.setText("O total por ciclo será a quota multiplicada pelo número de membros.");
        }
    }

    /**
     * Apresenta uma mensagem de erro estilizada ao utilizador.
     *
     * @param message Mensagem descritiva do erro.
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setHeaderText("Não foi possível criar o grupo");
        StyledDialogs.showAndWait(alert);
    }

    /**
     * Fecha a janela modal de criação de grupo.
     */
    private void close() {
        if (dialog != null) {
            dialog.close();
        }
    }
}