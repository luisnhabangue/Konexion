package model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidade de associação N:M entre {@link User} e {@link Group}.
 * Armazena informações específicas da participação do utilizador no grupo (saldo, ordem de receção, etc.).
 *
 * @author Sistema iXitique
 * @version 1.0
 */
@Entity
@Table(
        name = "user_group",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "group_id"})
        }
)
/**
 * Entidade ou objeto de domínio que representa os dados e conceitos centrais da aplicação.
 *
 * @since 1.0
 */
public class UserGroup {

    /** Identificador único do vínculo. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Utilizador associado ao grupo. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Grupo ao qual o utilizador pertence. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    /** Saldo acumulado ou disponível do membro no grupo. */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal balance;

    /** Indica se o membro já recebeu o montante acumulado na rotação atual. */
    @Column(nullable = false)
    private boolean received;

    /** Posição/ordem em que o membro receberá o montante do Xitique. */
    @Column(name = "member_order", nullable = false)
    private Integer userReceivingOrder;

    /** Lista de contribuições realizadas por este membro. */
    @OneToMany(mappedBy = "userGroup", cascade = CascadeType.ALL)
    private List<Contribution> contributions = new ArrayList<>();

    public UserGroup() {
    }

    /**
     * Construtor parametrizado para criação do vínculo membro-grupo.
     *
     * @param id Identificador do vínculo
     * @param user Utilizador
     * @param group Grupo
     * @param balance Saldo inicial
     * @param received Estado de receção do prémio
     * @param userReceivingOrder Ordem de receção
     */
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