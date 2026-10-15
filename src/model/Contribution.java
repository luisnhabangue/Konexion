package model;

import jakarta.persistence.*;
import model.enums.ContributionStatus;
import model.enums.PaymentMethod;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entidade que representa o pagamento/contribuição efetuado por um membro para um determinado ciclo.
 *
 * @author Sistema iXitique
 * @version 1.0
 */
@Entity
@Table(
        name = "contributions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_contribution_member_cycle",
                columnNames = {"user_group_id", "cycle_id"}
        )
)
public class Contribution {

    /** Identificador único da contribuição. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Membro que efetuou a contribuição. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_group_id", nullable = false)
    private UserGroup userGroup;

    /** Ciclo ao qual o pagamento corresponde. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cycle_id", nullable = false)
    private Cycle cycle;

    /** Montante pago pelo membro. */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    /** Data em que o pagamento foi efetuado. */
    private LocalDate paymentDate;

    /** Método de pagamento utilizado (ex.: M-Pesa, Transferência). */
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    /** Estado do pagamento (ex.: PENDING, PAID, LATE). */
    @Enumerated(EnumType.STRING)
    private ContributionStatus status;

    public Contribution() {
    }

    /**
     * Construtor parametrizado de contribuição.
     *
     * @param id Identificador da contribuição
     * @param userGroup Membro pagador
     * @param cycle Ciclo do pagamento
     * @param amount Valor pago
     * @param paymentDate Data do pagamento
     * @param paymentMethod Método de pagamento
     * @param status Estado do pagamento
     */
    public Contribution(Long id, UserGroup userGroup, Cycle cycle, BigDecimal amount,
                        LocalDate paymentDate, PaymentMethod paymentMethod, ContributionStatus status) {
        this.id = id;
        this.userGroup = userGroup;
        this.cycle = cycle;
        this.amount = amount;
        this.paymentDate = paymentDate;
        this.paymentMethod = paymentMethod;
        this.status = status;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public UserGroup getUserGroup() { return userGroup; }
    public void setUserGroup(UserGroup userGroup) { this.userGroup = userGroup; }

    public Cycle getCycle() { return cycle; }
    public void setCycle(Cycle cycle) { this.cycle = cycle; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }

    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }

    public ContributionStatus getStatus() { return status; }
    public void setStatus(ContributionStatus status) { this.status = status; }
}