package model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidade que representa um grupo de Xitique (poupança rotativa).
 *
 * @author Sistema iXitique
 * @version 1.0
 */
@Entity
@Table(name = "tb_group")
public class Group {

    /** Identificador único do grupo. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long groupId;

    /** Nome único do grupo. */
    @Column(nullable = false, unique = true)
    private String groupName;

    /** Descrição detalhada sobre as regras ou objetivo do grupo. */
    @Column(nullable = false, length = 150)
    private String groupDescription;

    /** Valor estipulado da quota de contribuição por membro. */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amountPerUser;

    /** Dia do mês limite para efetuar o pagamento. */
    @Column(name = "payday", nullable = false)
    private Integer payday;

    /** Data de criação do grupo. */
    @Column(name = "created_at", nullable = false)
    private LocalDate createdAt;

    /** Utilizador criador/administrador do grupo. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdBy;

    /** Frequência da contribuição em dias. */
    private int frequency;

    @Column(name = "is_private", nullable = false)
    private boolean privateGroup = true;

    /** Membros pertencentes a este grupo. */
    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserGroup> members = new ArrayList<>();

    /** Ciclos ou rodadas de pagamento do grupo. */
    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Cycle> cycles = new ArrayList<>();

    /** Construtor padrão da JPA. */
    public Group() {
    }

    /**
     * Construtor parametrizado para criação de um grupo.
     *
     * @param groupId Identificador do grupo
     * @param groupName Nome único do grupo
     * @param groupDescription Descrição do grupo
     * @param amountPerUser Valor a pagar por utilizador
     * @param payday Dia limite do pagamento
     * @param createdAt Data de criação
     * @param createdBy Criador do grupo
     * @param frequency Frequência do ciclo em dias
     */

    public Group(Long groupId, String groupName, String groupDescription, BigDecimal amountPerUser,
                 Integer payday, LocalDate createdAt, User createdBy, int frequency) {
        this.groupId = groupId;
        this.groupName = groupName;
        this.groupDescription = groupDescription;
        this.amountPerUser = amountPerUser;
        this.payday = payday;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.frequency = frequency;
    }

    /**
     * Adiciona um ciclo ao grupo e associa a referência inversa.
     * @param cycle Ciclo a ser adicionado
     */
    public void addCycle(Cycle cycle) {
        this.cycles.add(cycle);
        cycle.setGroup(this);
    }

    /**
     * Remove um ciclo do grupo.
     * @param cycle Ciclo a ser removido
     */
    public void removeCycle(Cycle cycle) {
        this.cycles.remove(cycle);
        cycle.setGroup(null);
    }

    /**
     * Adiciona um membro ao grupo e associa a referência inversa.
     * @param userGroup Vínculo do membro a adicionar
     */
    public void addMember(UserGroup userGroup) {
        this.members.add(userGroup);
        userGroup.setGroup(this);
    }

    /**
     * Remove um membro do grupo.
     * @param userGroup Vínculo do membro a remover
     */
    public void removeMember(UserGroup userGroup) {
        this.members.remove(userGroup);
        userGroup.setGroup(null);
    }

    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }

    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }

    public String getGroupDescription() { return groupDescription; }
    public void setGroupDescription(String groupDescription) { this.groupDescription = groupDescription; }

    public BigDecimal getAmountPerUser() { return amountPerUser; }
    public void setAmountPerUser(BigDecimal amountPerUser) { this.amountPerUser = amountPerUser; }

    public Integer getPayday() { return payday; }
    public void setPayday(Integer payday) { this.payday = payday; }

    public LocalDate getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDate createdAt) { this.createdAt = createdAt; }

    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }

    public int getFrequency() { return frequency; }
    public void setFrequency(int frequency) { this.frequency = frequency; }

    public boolean isPrivateGroup() { return privateGroup; }
    public void setPrivateGroup(boolean privateGroup) { this.privateGroup = privateGroup; }

    public List<UserGroup> getMembers() { return members; }
    public void setMembers(List<UserGroup> members) { this.members = members; }

    public List<Cycle> getCycles() { return cycles; }
    public void setCycles(List<Cycle> cycles) { this.cycles = cycles; }
}