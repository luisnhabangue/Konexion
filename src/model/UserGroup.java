package model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "user_group",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "group_id"})
        }
)
public class UserGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal balance;

    @Column(nullable = false)
    private boolean received; // Se já recebeu o prêmio da rodada atual

    @Column(name = "member_order", nullable = false)
    private Integer userReceivingOrder; // Posição na ordem de recebimento

    @OneToMany(mappedBy = "userGroup", cascade = CascadeType.ALL)
    private List<Contribution> contributions = new ArrayList<>();

    public UserGroup() {
    }

    public UserGroup(Long id, User user, Group group, BigDecimal balance, boolean received, Integer userReceivingOrder) {
        this.id = id;
        this.user = user;
        this.group = group;
        this.balance = balance;
        this.received = received;
        this.userReceivingOrder = userReceivingOrder;
    }


    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Group getGroup() { return group; }
    public void setGroup(Group group) { this.group = group; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    public boolean isReceived() { return received; }
    public void setReceived(boolean received) { this.received = received; }
    public Integer getUserReceivingOrder() { return userReceivingOrder; }
    public void setUserReceivingOrder(Integer userReceivingOrder) { this.userReceivingOrder = userReceivingOrder; }
    public List<Contribution> getContributions() { return contributions; }
    public void setContributions(List<Contribution> contributions) { this.contributions = contributions; }
}