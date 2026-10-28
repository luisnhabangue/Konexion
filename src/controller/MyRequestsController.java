package controller;

import dao.SupportTicketDao;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import model.SupportTicket;
import model.enums.Page;
import model.enums.SupportTicketStatus;
import service.SupportService;
import util.EntityManagerFactoryUtil;
import util.Navigator;
import util.SessionContext;
import util.StyledDialogs;
import util.UiFormatting;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Controlador JavaFX do ecrã "Os meus pedidos" ({@code my-requests.fxml}).
 * <p>
 * Lista os pedidos de suporte do utilizador autenticado, do mais recente para o mais antigo,
 * com o respetivo estado. Ao clicar num pedido, a mensagem enviada é mostrada ou escondida.
 * </p>
 *
 * @since 1.0
 */
public class MyRequestsController {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final SupportService supportService = new SupportService(
            new SupportTicketDao(EntityManagerFactoryUtil.getEntityManagerFactory()));

    @FXML private Button btnBack;
    @FXML private VBox tableCard;
    @FXML private VBox ticketRows;
    @FXML private VBox emptyState;

    /** Construtor padrão exigido pelo {@code FXMLLoader}. */
    public MyRequestsController() {
    }

    @FXML
    private void initialize() {
        btnBack.setOnAction(event -> Navigator.go(Page.HELP_CENTER));
        loadTickets();
    }

    private void loadTickets() {
        ticketRows.getChildren().clear();
        List<SupportTicket> tickets;
        try {
            tickets = supportService.getTicketsByUser(SessionContext.getCurrentUser().getId());
        } catch (RuntimeException exception) {
            ticketRows.getChildren().add(styled("Não foi possível carregar os pedidos. Tente novamente mais tarde.",
                    "section-sub"));
            return;
        }

        boolean empty = tickets.isEmpty();
        tableCard.setVisible(!empty);
        tableCard.setManaged(!empty);
        emptyState.setVisible(empty);
        emptyState.setManaged(empty);

        for (int i = 0; i < tickets.size(); i++) {
            ticketRows.getChildren().add(buildTicket(tickets.get(i), i == tickets.size() - 1));
        }
    }

    /** Cria a linha de um pedido, com a mensagem e a resposta escondidas até o utilizador clicar. */
    private VBox buildTicket(SupportTicket ticket, boolean last) {
        HBox row = new HBox(16);
        row.getStyleClass().add("t-row");
        if (last) {
            row.getStyleClass().add("t-row-last");
        }
        row.setCursor(Cursor.HAND);
        row.setAlignment(Pos.CENTER_LEFT);

        Label date = styled(formatDate(ticket.getCreatedAt()), "t-td", "c-w150");
        Label subject = styled(ticket.getSubject(), "t-name");
        subject.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(subject, Priority.ALWAYS);

        Label status = styled(statusText(ticket.getStatus()), "badge", statusStyle(ticket.getStatus()));
        HBox statusCell = new HBox(status);
        statusCell.getStyleClass().add("c-w110");
        statusCell.setAlignment(Pos.CENTER_LEFT);

        row.getChildren().addAll(date, subject, statusCell);

        Label message = styled(ticket.getMessage(), "t-td");
        message.setWrapText(true);
        Label responseTitle = styled("Resposta do suporte", "t-name");
        Label response = styled(
                ticket.getAdminResponse() == null || ticket.getAdminResponse().isBlank()
                        ? "Ainda não há resposta do suporte."
                        : ticket.getAdminResponse(),
                "t-td");
        response.setWrapText(true);
        VBox detail = new VBox(8,
                styled("A sua mensagem", "t-name"),
                message,
                responseTitle,
                response);
        detail.setPadding(new Insets(0, 16, 14, 16));
        detail.setVisible(false);
        detail.setManaged(false);

        row.setOnMouseClicked(event -> {
            boolean show = !detail.isVisible();
            detail.setVisible(show);
            detail.setManaged(show);
        });

        return new VBox(row, detail);
    }

    private static String formatDate(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "—";
        }
        return UiFormatting.date(dateTime.toLocalDate()) + ", " + dateTime.format(TIME);
    }

    private static String statusText(SupportTicketStatus status) {
        if (status == null) {
            return "—";
        }
        return switch (status) {
            case OPEN -> "ABERTO";
            case IN_PROGRESS -> "EM CURSO";
            case RESOLVED -> "RESOLVIDO";
        };
    }

    private static String statusStyle(SupportTicketStatus status) {
        if (status == null) {
            return "badge-neutral";
        }
        return switch (status) {
            case OPEN -> "badge-warning";
            case IN_PROGRESS -> "badge-neutral";
            case RESOLVED -> "badge-success";
        };
    }

    private Label styled(String text, String... classes) {
        Label label = new Label(text);
        label.getStyleClass().addAll(classes);
        return label;
    }
}
