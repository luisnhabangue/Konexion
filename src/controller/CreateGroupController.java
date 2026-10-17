package controller;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import model.Group;
import service.GroupService;
import util.SessionContext;
import util.UiFormatting;

import java.time.LocalDate;
import java.util.Map;

public class CreateGroupController {
    private static final Map<String, Integer> FREQUENCIES = Map.of(
            "Semanal (7 dias)", 7,
            "Quinzenal (15 dias)", 15,
            "Mensal (30 dias)", 30
    );

    private final GroupService groupService = new GroupService();

    @FXML private TextField txtName;
    @FXML private TextArea txtDescription;
    @FXML private TextField txtAmount;
    @FXML private TextField txtPayDay;
    @FXML private ComboBox<String> cmbFrequency;
    @FXML private Button btnClose;
    @FXML private Button btnCancel;
    @FXML private Button btnConfirm;
    private Stage dialog;

    @FXML
    private void initialize() {
        btnClose.setOnAction(event -> close());
        btnCancel.setOnAction(event -> close());
        btnConfirm.setOnAction(event -> createGroup());
    }

    public void setDialog(Stage dialog) {
        this.dialog = dialog;
    }

    private void createGroup() {
        try {
            String frequencyLabel = cmbFrequency.getValue();
            Integer frequency = FREQUENCIES.get(frequencyLabel);
            if (frequency == null) {
                throw new IllegalArgumentException("Selecione a frequência do grupo.");
            }
            java.math.BigDecimal amount = UiFormatting.parseMoney(txtAmount.getText());
            int payday = Integer.parseInt(txtPayDay.getText().trim());

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
            groupService.createNewGroup(group);
            close();
        } catch (NumberFormatException e) {
            showError("Introduza um valor de contribuição e dia de pagamento válidos.");
        } catch (RuntimeException e) {
            showError(e.getMessage());
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setHeaderText("Não foi possível criar o grupo");
        alert.showAndWait();
    }

    private void close() {
        if (dialog != null) {
            dialog.close();
        }
    }
}
