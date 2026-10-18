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
import util.UiFormatting;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class PaymentController {
    private static final Map<String, PaymentMethod> METHODS = Map.of(
            "M-Pesa", PaymentMethod.MPESA,
            "E-Mola", PaymentMethod.EMOLA,
            "Transferência bancária", PaymentMethod.BIM,
            "Dinheiro em espécie", PaymentMethod.CASH
    );
    private final GroupService groupService = new GroupService();
    private Long groupId;
    private Stage dialog;
    private Runnable afterSave;

    @FXML private ComboBox<UserGroup> cmbMember;
    @FXML private TextField txtCycle;
    @FXML private TextField txtAmount;
    @FXML private ComboBox<String> cmbMethod;
    @FXML private DatePicker dpPaymentDate;
    @FXML private Button btnClose;
    @FXML private Button btnCancel;
    @FXML private Button btnConfirm;

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

    private void showError(String message) {
        new Alert(Alert.AlertType.ERROR, message).showAndWait();
    }

    private void close() {
        if (dialog != null) {
            dialog.close();
        }
    }
}
