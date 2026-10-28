
package controller;

import dao.SupportTicketDao;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import model.SupportTicket;
import model.enums.Page;
import model.enums.SupportTicketStatus;
import service.SupportService;
import util.EntityManagerFactoryUtil;
import util.Navigator;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class AdminSupportController {

    private final SupportService supportService = new SupportService(
            new SupportTicketDao(
                    EntityManagerFactoryUtil.getEntityManagerFactory()
            )
    );

    @FXML private ListView<SupportTicket> ticketList;
    @FXML private Label lblRequester;
    @FXML private Label lblSubject;
    @FXML private Label lblDate;
    @FXML private Label lblMessage;
    @FXML private Label lblCurrentResponse;
    @FXML private TextArea txtAdminResponse;
    @FXML private ComboBox<SupportTicketStatus> cmbStatus;
    @FXML private Label lblEmpty;

    private SupportTicket selectedTicket;

    private final DateTimeFormatter dateFormatter =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    private void initialize() {
        cmbStatus.setItems(
                FXCollections.observableArrayList(
                        SupportTicketStatus.values()
                )
        );

        cmbStatus.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(
                    SupportTicketStatus status,
                    boolean empty) {
                super.updateItem(status, empty);

                setText(empty || status == null
                        ? null
                        : statusLabel(status));
            }
        });

        cmbStatus.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(
                    SupportTicketStatus status,
                    boolean empty) {
                super.updateItem(status, empty);

                setText(empty || status == null
                        ? null
                        : statusLabel(status));
            }
        });

        ticketList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(
                    SupportTicket ticket,
                    boolean empty) {
                super.updateItem(ticket, empty);

                if (empty || ticket == null) {
                    setText(null);
                } else {
                    setText(
                            ticket.getSubject()
                                    + "\n"
                                    + ticket.getUser().getFirstName()
                                    + " "
                                    + ticket.getUser().getLastName()
                                    + " · "
                                    + statusLabel(ticket.getStatus())
                    );
                }
            }
        });

        ticketList.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, oldTicket, newTicket) ->
                        showTicket(newTicket));

        loadTickets();
        clearDetails();
    }

    private void loadTickets() {
        List<SupportTicket> tickets = supportService.getAllTickets();
        ticketList.getItems().setAll(tickets);

        boolean empty = tickets.isEmpty();
        lblEmpty.setVisible(empty);
        lblEmpty.setManaged(empty);
    }

    private void showTicket(SupportTicket ticket) {
        selectedTicket = ticket;

        if (ticket == null) {
            clearDetails();
            return;
        }

        lblRequester.setText(
                ticket.getUser().getFirstName()
                        + " "
                        + ticket.getUser().getLastName()
                        + " (@"
                        + ticket.getUser().getUsername()
                        + ")"
        );

        lblSubject.setText(ticket.getSubject());
        lblDate.setText(ticket.getCreatedAt() == null
                ? "Data indisponível"
                : ticket.getCreatedAt().format(dateFormatter));

        lblMessage.setText(ticket.getMessage());

        String response = ticket.getAdminResponse();
        lblCurrentResponse.setText(
                response == null || response.isBlank()
                        ? "Ainda não foi enviada uma resposta."
                        : response
        );

        txtAdminResponse.setText(
                response == null ? "" : response
        );

        cmbStatus.setValue(ticket.getStatus());
    }


    @FXML
    private void saveResponse() {
        if (selectedTicket == null) {
            showAlert(
                    Alert.AlertType.WARNING,
                    "Seleciona primeiro um pedido de suporte."
            );
            return;
        }

        try {
            Integer supportTicketId = selectedTicket.getId();

            supportService.respondToTicket(
                    selectedTicket,
                    txtAdminResponse.getText(),
                    cmbStatus.getValue()
            );

            showAlert(
                    Alert.AlertType.INFORMATION,
                    "Resposta e estado guardados com sucesso."
            );

            loadTickets();

            ticketList.getItems().stream()
                    .filter(ticket -> ticket.getId().equals(supportTicketId))
                    .findFirst()
                    .ifPresent(ticket ->
                            ticketList.getSelectionModel().select(ticket)
                    );

        } catch (RuntimeException exception) {
            showAlert(
                    Alert.AlertType.ERROR,
                    exception.getMessage() == null
                            ? "Não foi possível guardar as alterações."
                            : exception.getMessage()
            );
        }
    }


    @FXML
    private void backToDashboard() {
        Navigator.go(Page.ADMIN);
    }

    private void clearDetails() {
        lblRequester.setText("Seleciona um pedido");
        lblSubject.setText("");
        lblDate.setText("");
        lblMessage.setText("");
        lblCurrentResponse.setText("");
        txtAdminResponse.clear();
        cmbStatus.setValue(null);
    }

    private String statusLabel(SupportTicketStatus status) {
        return switch (status) {
            case OPEN -> "Aberto";
            case IN_PROGRESS -> "Em andamento";
            case RESOLVED -> "Resolvido";
        };
    }

    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
