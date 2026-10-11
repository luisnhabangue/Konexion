package model;

import jakarta.persistence.*;
import model.enums.ContributionStatus;
import model.enums.PaymentMethod;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "contributions")
public class Contribution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_group_id", nullable = false)
    private UserGroup userGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cycle_id", nullable = false)
    private Cycle cycle;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    private LocalDate paymentDate;

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    private ContributionStatus status; // PENDENTE, PAGO, ATRASADO

    public Contribution() {
    }

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