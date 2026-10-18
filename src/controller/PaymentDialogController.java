package controller;

import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXRadioButton;
import io.github.palexdev.materialfx.controls.MFXTextField;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import model.Cycle;
import model.Group;
import model.User;
import model.enums.PaymentMethod;
import payment.PaymentResponse;
import service.GroupService;
import service.PaymentService;

import java.io.IOException;
import java.net.URL;

public class PaymentDialogController {

    @FXML private HBox header;
    @FXML private Label cycleLabel;
    @FXML private Label amountLabel;
    @FXML private Label groupLabel;
    @FXML private Label messageLabel;
    @FXML private HBox cardMpesa;
    @FXML private HBox cardEmola;
    @FXML private MFXRadioButton radioMpesa;
    @FXML private MFXRadioButton radioEmola;
    @FXML private MFXTextField txtTelefone;
    @FXML private MFXButton btnPagar;

    private final PaymentService paymentService = new PaymentService();
    private final GroupService groupService = new GroupService();

    private Group group;
    private User user;
    private Stage stage;
    private boolean paid = false;
    private double dragX, dragY;

    /** Abre o pop-up e só volta quando fecha. Devolve true se o pagamento foi feito. */
    public static boolean show(Window owner, Group group, User user) {
        if (owner == null || group == null || user == null) {
            throw new IllegalArgumentException("Janela, grupo e utilizador são obrigatórios para pagar.");
        }

        URL resource = PaymentDialogController.class.getResource("/view/fxml/payment-dialog.fxml");
        if (resource == null) {
            throw new IllegalStateException("Não foi possível encontrar o ecrã de pagamento.");
        }

        try {
            FXMLLoader loader = new FXMLLoader(resource);
            Parent root = loader.load();
            PaymentDialogController controller = loader.getController();

            Stage stage = new Stage();
            stage.initStyle(StageStyle.TRANSPARENT);
            stage.initOwner(owner);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Pagar contribuição");

            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            stage.setScene(scene);

            controller.init(stage, group, user);
            stage.showAndWait();
            return controller.paid;
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível abrir o ecrã de pagamento.", e);
        }
    }

    private void init(Stage stage, Group group, User user) {
        this.stage = stage;
        this.group = group;
        this.user = user;

        amountLabel.setText(util.UiFormatting.money(group.getAmountPerUser()));
        groupLabel.setText("Grupo: " + group.getGroupName());
        cycleLabel.setText(loadCycleText(group));
        txtTelefone.setText(user.getPhoneNumber() != null ? user.getPhoneNumber() : "");

        // Mover a janela arrastando o cabeçalho
        header.setOnMousePressed(e -> { dragX = e.getSceneX(); dragY = e.getSceneY(); });
        header.setOnMouseDragged(e -> {
            stage.setX(e.getScreenX() - dragX);
            stage.setY(e.getScreenY() - dragY);
        });

        // Troca o estilo do cartão conforme o método escolhido
        radioMpesa.selectedProperty().addListener((o, a, b) -> updateCards());
        radioEmola.selectedProperty().addListener((o, a, b) -> updateCards());
        cardMpesa.setOnMouseClicked(e -> radioMpesa.setSelected(true));
        cardEmola.setOnMouseClicked(e -> radioEmola.setSelected(true));
        updateCards();
    }

    private void updateCards() {
        setCardSelected(cardMpesa, radioMpesa.isSelected());
        setCardSelected(cardEmola, radioEmola.isSelected());
    }

    private void setCardSelected(HBox card, boolean selected) {
        card.getStyleClass().removeAll("pay-method-card", "pay-method-card-selected");
        card.getStyleClass().add(selected ? "pay-method-card-selected" : "pay-method-card");
    }

    private String loadCycleText(Group g) {
        Cycle cycle = groupService.findActiveCycle(g.getGroupId());
        return cycle == null ? "Sem ciclo" : "Ciclo #" + String.format("%02d", cycle.getNumero());
    }

    @FXML
    private void onConfirmPayment() {
        PaymentMethod method = radioEmola.isSelected() ? PaymentMethod.EMOLA : PaymentMethod.MPESA;
        String account = txtTelefone.getText();
        if (account == null || account.isBlank()) {
            showMessage("Indique o número de telefone.", "msg-error");
            return;
        }

        Task<PaymentResponse> task = new Task<>() {
            @Override protected PaymentResponse call() {
                return paymentService.payContribution(
                        group.getGroupId(), user.getId(), method, account);
            }
        };

        setBusy(true);
        showMessage("A processar pagamento...", "msg-info");

        task.setOnSucceeded(e -> {
            setBusy(false);
            PaymentResponse r = task.getValue();
            if (r.success()) {
                paid = true;
                showMessage("Pagamento aprovado. Referência: " + r.transactionRef(), "msg-success");
                btnPagar.setVisible(false);
                btnPagar.setManaged(false);
                cardMpesa.setDisable(true);
                cardEmola.setDisable(true);
                txtTelefone.setDisable(true);
            } else {
                showMessage(r.message() + " (" + r.providerCode() + ")", "msg-error");
            }
        });
        task.setOnFailed(e -> {
            setBusy(false);
            Throwable ex = task.getException();
            showMessage(ex != null && ex.getMessage() != null
                    ? ex.getMessage() : "Erro inesperado ao pagar.", "msg-error");
        });

        Thread t = new Thread(task, "pagamento");
        t.setDaemon(true);
        t.start();
    }

    @FXML
    private void onCancel() {
        stage.close();
    }

    private void setBusy(boolean busy) {
        btnPagar.setDisable(busy);
        btnPagar.setText(busy ? "A processar..." : "Simular Pagamento  →");
        cardMpesa.setDisable(busy);
        cardEmola.setDisable(busy);
        txtTelefone.setDisable(busy);
    }

    private void showMessage(String text, String styleClass) {
        messageLabel.getStyleClass().removeAll("msg-info", "msg-error", "msg-success");
        messageLabel.getStyleClass().add(styleClass);
        messageLabel.setText(text);
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);
        stage.sizeToScene();
    }
}