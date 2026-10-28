
package service;

import dao.SupportTicketDao;
import model.SupportTicket;
import model.User;
import model.enums.SupportTicketStatus;

import java.util.List;

/**
 * Serviço responsável pelas regras de negócio
 * dos pedidos de suporte.
 *
 * @author Sistema iXitique
 * @version 1.0
 */
public class SupportService {

    private final SupportTicketDao supportTicketDao;

    public SupportService(SupportTicketDao supportTicketDao) {
        this.supportTicketDao = supportTicketDao;
    }

    /**
     * Regista um novo pedido de suporte.
     */
    public void createTicket(
            User user,
            String subject,
            String message) {

        if (user == null) {
            throw new IllegalArgumentException(
                    "É necessário identificar o utilizador."
            );
        }

        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException(
                    "O assunto é obrigatório."
            );
        }

        if (subject.trim().length() > 120) {
            throw new IllegalArgumentException(
                    "O assunto não pode ultrapassar 120 caracteres."
            );
        }

        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException(
                    "A mensagem é obrigatória."
            );
        }

        SupportTicket supportTicket = new SupportTicket();
        supportTicket.setUser(user);
        supportTicket.setSubject(subject.trim());
        supportTicket.setMessage(message.trim());

        supportTicketDao.save(supportTicket);
    }

    /**
     * Consulta os pedidos de suporte de um utilizador.
     */
    public List<SupportTicket> getTicketsByUser(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException(
                    "O identificador do utilizador é obrigatório."
            );
        }

        return supportTicketDao.findByUserId(userId);
    }

    /**
     * Consulta todos os pedidos de suporte.
     */
    public List<SupportTicket> getAllTickets() {
        return supportTicketDao.findAll();
    }

    /**
     * Atualiza um pedido de suporte.
     */
    public void updateTicket(SupportTicket supportTicket) {
        if (supportTicket == null || supportTicket.getId() == null) {
            throw new IllegalArgumentException(
                    "É necessário indicar um pedido válido."
            );
        }

        if (supportTicket.getStatus() == null) {
            throw new IllegalArgumentException(
                    "O estado do pedido é obrigatório."
            );
        }

        supportTicketDao.update(supportTicket);
    }
    /**
     * Guarda a resposta do administrador e atualiza o estado do pedido.
     */
    public void respondToTicket(
            SupportTicket supportTicket,
            String adminResponse,
            SupportTicketStatus status) {

        if (supportTicket == null || supportTicket.getId() == null) {
            throw new IllegalArgumentException("Pedido de suporte inválido.");
        }

        if (adminResponse == null || adminResponse.isBlank()) {
            throw new IllegalArgumentException("A resposta é obrigatória.");
        }

        if (status == null) {
            throw new IllegalArgumentException("Seleciona o estado do pedido.");
        }

        supportTicket.setAdminResponse(adminResponse.trim());
        supportTicket.setStatus(status);

        supportTicketDao.update(supportTicket);
    }
}
