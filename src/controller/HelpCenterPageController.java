package controller;

import dao.SupportTicketDao;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import model.User;
import model.enums.Page;
import model.enums.SupportTicketStatus;
import service.SupportService;
import util.EntityManagerFactoryUtil;
import util.Navigator;
import util.SessionContext;
import util.StyledDialogs;

/**
 * Controlador JavaFX da página principal do Centro de Ajuda ({@code help-center.fxml}).
 * <p>
 * Apresenta os pontos de entrada da ajuda (guia, perguntas frequentes, suporte e pedidos do utilizador)
 * e permite enviar um novo pedido de suporte. A página é carregada na área principal através do
 * {@link util.Navigator} com {@link model.enums.Page#HELP_CENTER}.
 * </p>
 *
 * @since 1.0
 */
public class HelpCenterPageController {

    /** Número máximo de caracteres do assunto (igual ao limite validado em {@link SupportService}). */
    private static final int MAX_SUBJECT = 120;

    /** Número máximo de caracteres da mensagem, limitado na interface. */
    private static final int MAX_MESSAGE = 2000;

    /** Serviço responsável pelos pedidos de suporte. */
    private final SupportService supportService = new SupportService(
            new SupportTicketDao(EntityManagerFactoryUtil.getEntityManagerFactory()));

    @FXML private TextField txtHelpSearch;
    @FXML private VBox cardGuide;
    @FXML private VBox cardFaq;
    @FXML private VBox cardSupport;
    @FXML private VBox cardMyRequests;
    @FXML private Label lblOpenRequestsBadge;

    /** Construtor padrão exigido pelo {@code FXMLLoader}. */
    public HelpCenterPageController() {
    }

    /**
     * Ativa os cartões por teclado (Enter ou Espaço) e carrega o número de pedidos em aberto.
     */
    @FXML
    private void initialize() {
        enableKeyboardActivation(cardGuide, this::openGuide);
        enableKeyboardActivation(cardFaq, this::openFaq);
        enableKeyboardActivation(cardSupport, this::openSupportDialog);
        enableKeyboardActivation(cardMyRequests, this::openMyRequests);
        refreshOpenRequestsBadge();
    }

    // ------------------------------------------------------------------ Ações do FXML

    /**
     * Trata a pesquisa de dúvidas quando o utilizador carrega em Enter no campo de pesquisa.
     *
     * @param event Evento de ação do campo de texto.
     */
    @FXML
    private void onSearchHelp(ActionEvent event) {
        String query = txtHelpSearch.getText() == null ? "" : txtHelpSearch.getText().trim();
        if (query.isEmpty()) {
            return;
        }
        HelpFaqController faq = Navigator.go(Page.HELP_FAQ);
        faq.search(query);
    }

    /**
     * Abre o guia de utilização.
     *
     * @param event Clique no cartão.
     */
    @FXML
    private void onOpenGuide(MouseEvent event) {
        openGuide();
    }

    /**
     * Abre as perguntas frequentes.
     *
     * @param event Clique no cartão.
     */
    @FXML
    private void onOpenFaq(MouseEvent event) {
        openFaq();
    }

    /**
     * Abre o formulário para enviar um pedido ao suporte.
     *
     * @param event Clique no cartão.
     */
    @FXML
    private void onContactSupport(MouseEvent event) {
        openSupportDialog();
    }

    /**
     * Abre a lista dos pedidos enviados pelo utilizador.
     *
     * @param event Clique no cartão.
     */
    @FXML
    private void onOpenMyRequests(MouseEvent event) {
        openMyRequests();
    }

    // ------------------------------------------------------------------ Destinos

    private void openGuide() {
        Navigator.go(Page.HELP_GUIDE);
    }

    private void openFaq() {
        Navigator.go(Page.HELP_FAQ);
    }

    private void openMyRequests() {
        Navigator.go(Page.MY_REQUESTS);
    }

    /**
     * Mostra o formulário de pedido de suporte. Os erros de validação aparecem dentro do próprio
     * formulário, que só fecha quando o pedido é guardado ou quando o utilizador cancela.
     */
    private void openSupportDialog() {
        User user = SessionContext.getCurrentUser();
        if (user == null) {
            showError("Não foi possível identificar o utilizador. Inicie sessão novamente.");
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(window());
        dialog.setTitle("Contactar suporte");
        dialog.setHeaderText("Envie uma dúvida ou relate um problema");

        ButtonType send = new ButtonType("Enviar pedido", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(send, ButtonType.CANCEL);

        Label lblSubject = new Label("Assunto");
        lblSubject.getStyleClass().add("field-label");
        TextField tfSubject = new TextField();
        tfSubject.getStyleClass().add("field-input");
        tfSubject.setPromptText("Ex.: Não consigo abrir um ciclo");
        tfSubject.setTextFormatter(maxLength(MAX_SUBJECT));

        Label lblMessage = new Label("Mensagem");
        lblMessage.getStyleClass().add("field-label");
        TextArea taMessage = new TextArea();
        taMessage.getStyleClass().add("field-textarea");
        taMessage.setPromptText("Descreva o que aconteceu");
        taMessage.setWrapText(true);
        taMessage.setPrefRowCount(5);
        taMessage.setTextFormatter(maxLength(MAX_MESSAGE));

        Label lblError = new Label();
        lblError.getStyleClass().add("field-error");
        lblError.setWrapText(true);
        lblError.setVisible(false);
        lblError.setManaged(false);

        VBox form = new VBox(6, lblSubject, tfSubject, lblMessage, taMessage, lblError);
        form.setPrefWidth(420);
        dialog.getDialogPane().setContent(form);

        Node sendButton = dialog.getDialogPane().lookupButton(send);
        sendButton.addEventFilter(ActionEvent.ACTION, event -> {
            try {
                supportService.createTicket(user, tfSubject.getText(), taMessage.getText());
            } catch (IllegalArgumentException exception) {
                showInlineError(lblError, exception.getMessage());
                event.consume();
            } catch (RuntimeException exception) {
                showInlineError(lblError, "Não foi possível guardar o pedido. Tente novamente.");
                event.consume();
            }
        });

        boolean sent = StyledDialogs.showAndWait(dialog).filter(send::equals).isPresent();
        if (sent) {
            refreshOpenRequestsBadge();
            showInfo("Pedido enviado",
                    "O seu pedido foi guardado. Poderá acompanhar o estado em \"Os meus pedidos\".");
        }
    }

    // ------------------------------------------------------------------ Dados reais

    /**
     * Atualiza o selo do cartão "Os meus pedidos" com o número de pedidos ainda não resolvidos.
     * Se não houver pedidos em aberto, ou se a consulta falhar, o selo fica escondido.
     */
    private void refreshOpenRequestsBadge() {
        User user = SessionContext.getCurrentUser();
        if (user == null) {
            setBadgeVisible(false);
            return;
        }
        try {
            long open = supportService.getTicketsByUser(user.getId()).stream()
                    .filter(ticket -> ticket.getStatus() != SupportTicketStatus.RESOLVED)
                    .count();
            if (open > 0) {
                lblOpenRequestsBadge.setText(open + " em aberto");
            }
            setBadgeVisible(open > 0);
        } catch (RuntimeException exception) {
            setBadgeVisible(false);
        }
    }

    private void setBadgeVisible(boolean visible) {
        lblOpenRequestsBadge.setVisible(visible);
        lblOpenRequestsBadge.setManaged(visible);
    }

    // ------------------------------------------------------------------ Auxiliares

    /** Faz com que Enter ou Espaço num cartão com foco execute a mesma ação que o clique. */
    private void enableKeyboardActivation(Node card, Runnable action) {
        card.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE) {
                action.run();
                event.consume();
            }
        });
    }

    private static TextFormatter<String> maxLength(int max) {
        return new TextFormatter<>(change -> change.getControlNewText().length() <= max ? change : null);
    }

    private void showInlineError(Label label, String message) {
        label.setText(message);
        label.setVisible(true);
        label.setManaged(true);
    }

    private Window window() {
        return txtHelpSearch.getScene() == null ? null : txtHelpSearch.getScene().getWindow();
    }

    private void showInfo(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.initOwner(window());
        alert.setTitle("Centro de Ajuda");
        alert.setHeaderText(header);
        StyledDialogs.showAndWait(alert);
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.initOwner(window());
        alert.setTitle("Centro de Ajuda");
        alert.setHeaderText("Não foi possível continuar");
        StyledDialogs.showAndWait(alert);
    }
}