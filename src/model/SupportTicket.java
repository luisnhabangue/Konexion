
package model;

import jakarta.persistence.*;
import model.enums.SupportTicketStatus;

import java.time.LocalDateTime;

/**
 * Entidade que representa um pedido de ajuda enviado por um utilizador.
 *
 * @author Sistema iXitique
 * @version 1.0
 */
@Entity
@Table(name = "support_tickets")
public class SupportTicket {

    /** Identificador único do pedido. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** Utilizador que enviou o pedido. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Assunto do pedido de ajuda. */
    @Column(nullable = false, length = 120)
    private String subject;

    /** Descrição da dúvida ou problema. */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    /**
     * Resposta enviada pelo administrador ao utilizador.
     */
    @Column(name = "admin_response", columnDefinition = "TEXT")
    private String adminResponse;

    /** Estado atual do pedido. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SupportTicketStatus status = SupportTicketStatus.OPEN;

    /** Data e hora de criação do pedido. */
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public SupportTicket() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public SupportTicketStatus getStatus() {
        return status;
    }

    public void setStatus(SupportTicketStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getAdminResponse() {
        return adminResponse;
    }

    public void setAdminResponse(String adminResponse) {
        this.adminResponse = adminResponse;
    }
}
