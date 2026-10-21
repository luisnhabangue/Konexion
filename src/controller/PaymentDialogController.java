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

/**
 * Controlador JavaFX responsável pelo ecrã modal transparente de pagamento móvel (M-Pesa / E-Mola)[cite: 112].
 * <p>
 * Executa o processamento do pagamento de forma assíncrona utilizando {@link Task}[cite: 112].
 * </p>
 *
 * @author Sistema Xitique
 * @version 1.0
 * @since 1.0
 */
public class PaymentDialogController {

    /** Cabeçalho da janela utilizado para arrasto[cite: 112]. */
    @FXML private HBox header;

    /** Rótulo de identificação do ciclo[cite: 112]. */
    @FXML private Label cycleLabel;

    /** Rótulo com o valor da contribuição[cite: 112]. */
    @FXML private Label amountLabel;

    /** Rótulo com a denominação do grupo[cite: 112]. */
    @FXML private Label groupLabel;

    /** Rótulo de feedback visual e estado do pagamento[cite: 112]. */
    @FXML private Label messageLabel;

    /** Cartão visual referente à opção M-Pesa[cite: 112]. */
    @FXML private HBox cardMpesa;

    /** Cartão visual referente à opção E-Mola[cite: 112]. */
    @FXML private HBox cardEmola;

    /** Botão de rádio para seleção da opção M-Pesa[cite: 112]. */
    @FXML private MFXRadioButton radioMpesa;

    /** Botão de rádio para seleção da opção E-Mola[cite: 112]. */
    @FXML private MFXRadioButton radioEmola;

    /** Campo de texto para introdução do contacto telefónico pagador[cite: 112]. */
    @FXML private MFXTextField txtTelefone;

    /** Botão de confirmação de pagamento[cite: 112]. */
    @FXML private MFXButton btnPagar;

    /** Serviço responsável pela comunicação com o gateway de pagamento[cite: 112]. */
    private final PaymentService paymentService = new PaymentService();

    /** Serviço com dados de grupos e ciclos[cite: 112]. */
    private final GroupService groupService = new GroupService();

    /** Grupo para o qual a contribuição está a ser efetuada[cite: 112]. */
    private Group group;

    /** Utilizador a efetuar o pagamento[cite: 112]. */
    private User user;

    /** Instância da janela modal[cite: 112]. */
    private Stage stage;

    /** Indicador de sucesso na operação de pagamento[cite: 112]. */
    private boolean paid = false;

    /** Deslocamento horizontal durante o arrasto da janela[cite: 112]. */
    private double dragX;

    /** Deslocamento vertical durante o arrasto da janela[cite: 112]. */
    private double dragY;

    /**
     * Abre a caixa de diálogo transparente de pagamento e bloqueia até ser fechada[cite: 112].
     *
     * @param owner A janela proprietária (Window).
     * @param group O grupo ao qual se destina a contribuição.
     * @param user  O utilizador pagador.
     * @return {@code true} se o pagamento tiver sido aprovado com sucesso; {@code false} caso contrário.
     * @throws IllegalArgumentException Se owner, group ou user forem nulos.
     * @throws IllegalStateException Se o FXML não puder ser carregado.
     */
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

    /**
     * Inicializa os dados da janela modal e configura o evento de arrasto do cabeçalho[cite: 112].
     *
     * @param stage Janela modal {@link Stage}.
     * @param group Grupo de Xitique.
     * @param user  Utilizador ativo.
     */
    private void init(Stage stage, Group group, User user) {
        this.stage = stage;
        this.group = group;
        this.user = user;

        amountLabel.setText(util.UiFormatting.money(group.getAmountPerUser()));
        groupLabel.setText("Grupo: " + group.getGroupName());
        cycleLabel.setText(loadCycleText(group));
        txtTelefone.setText(user.getPhoneNumber() != null ? user.getPhoneNumber() : "");

        header.setOnMousePressed(e -> { dragX = e.getSceneX(); dragY = e.getSceneY(); });
        header.setOnMouseDragged(e -> {
            stage.setX(e.getScreenX() - dragX);
            stage.setY(e.getScreenY() - dragY);
        });

        radioMpesa.selectedProperty().addListener((o, a, b) -> updateCards());
        radioEmola.selectedProperty().addListener((o, a, b) -> updateCards());
        cardMpesa.setOnMouseClicked(e -> radioMpesa.setSelected(true));
        cardEmola.setOnMouseClicked(e -> radioEmola.setSelected(true));
        updateCards();
    }

    /**
     * Atualiza os estilos visuais dos cartões de métodos de pagamento[cite: 112].
     */
    private void updateCards() {
        setCardSelected(cardMpesa, radioMpesa.isSelected());
        setCardSelected(cardEmola, radioEmola.isSelected());
    }

    /**
     * Aplica ou remove as classes CSS de seleção ao cartão[cite: 112].
     *
     * @param card     Componente HBox do cartão.
     * @param selected {@code true} para aplicar o estilo de selecionado.
     */
    private void setCardSelected(HBox card, boolean selected) {
        card.getStyleClass().removeAll("pay-method-card", "pay-method-card-selected");
        card.getStyleClass().add(selected ? "pay-method-card-selected" : "pay-method-card");
    }

    /**
     * Carrega a designação do ciclo ativo do grupo[cite: 112].
     *
     * @param g O grupo a consultar.
     * @return O texto descritivo do ciclo.
     */
    private String loadCycleText(Group g) {
        Cycle cycle = groupService.findActiveCycle(g.getGroupId());
        return cycle == null ? "Sem ciclo" : "Ciclo #" + String.format("%02d", cycle.getNumero());
    }

    /**
     * Dispara a tarefa assíncrona para efetuar o pagamento via carteira móvel[cite: 112].
     */
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

    /**
     * Cancela o processo de pagamento e fecha a janela[cite: 112].
     */
    @FXML
    private void onCancel() {
        stage.close();
    }

    /**
     * Bloqueia ou desbloqueia a interface do utilizador durante o processamento da transação[cite: 112].
     *
     * @param busy {@code true} se uma operação assíncrona estiver a decorrer.
     */
    private void setBusy(boolean busy) {
        btnPagar.setDisable(busy);
        btnPagar.setText(busy ? "A processar..." : "Simular Pagamento  →");
        cardMpesa.setDisable(busy);
        cardEmola.setDisable(busy);
        txtTelefone.setDisable(busy);
    }

    /**
     * Exibe uma mensagem informativa, de sucesso ou de erro[cite: 112].
     *
     * @param text       O texto da mensagem.
     * @param styleClass A classe de estilo CSS da mensagem.
     */
    private void showMessage(String text, String styleClass) {
        messageLabel.getStyleClass().removeAll("msg-info", "msg-error", "msg-success");
        messageLabel.getStyleClass().add(styleClass);
        messageLabel.setText(text);
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);
        stage.sizeToScene();
    }
}