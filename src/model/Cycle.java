package model;

import jakarta.persistence.*;
import model.enums.CycleStatus;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidade que representa uma rodada ou ciclo específico dentro de um grupo de Xitique.
 *
 * @author Sistema iXitique
 * @version 1.0
 */
@Entity
@Table(
        name = "cycles",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_ciclo_grupo_numero",
                        columnNames = {"group_id", "numero"}
                )
        }
)
/**
 * Entidade ou objeto de domínio que representa os dados e conceitos centrais da aplicação.
 *
 * @since 1.0
 */
public class Cycle {

    /** Identificador único do ciclo. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** Grupo ao qual o ciclo pertence. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    /** Número sequencial da rodada (ex.: Ciclo 1, Ciclo 2...). */
    @Column(nullable = false)
    private Integer numero;

    /** Data inicial do ciclo. */
    private LocalDate startDate;

    /** Data limite/de encerramento do ciclo. */
    private LocalDate endDate;

    /** Estado do ciclo (ex.: IN_PROGRESS, CONCLUDED). */
    @Enumerated(EnumType.STRING)
    private CycleStatus status;

    @Column(name = "automatic_payout_enabled", nullable = false)
    private boolean automaticPayoutEnabled;

    /** Membro do grupo contemplado para receber o valor acumulado neste ciclo. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "beneficiary_id", nullable = false)
    private UserGroup beneficiary;

    /** Lista de contribuições de todos os membros associadas a este ciclo. */
    @OneToMany(mappedBy = "cycle", cascade = CascadeType.ALL)
    private List<Contribution> contributions = new ArrayList<>();

    public Cycle() {
    }

    /**
     * Construtor para inicialização de um ciclo.
     *
     * @param id Identificador do ciclo
     * @param group Grupo proprietário
     * @param numero Número sequencial
     * @param startDate Data de início
     * @param beneficiary Membro beneficiário
     */
    public Cycle(Integer id, Group group, Integer numero, LocalDate startDate, UserGroup beneficiary) {
        this.id = id;
        this.group = group;
        this.numero = numero;
        this.startDate = startDate;
        this.beneficiary = beneficiary;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Group getGroup() { return group; }
    public void setGroup(Group group) { this.group = group; }

    public Integer getNumero() { return numero; }
    public void setNumero(Integer numero) { this.numero = numero; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public CycleStatus getStatus() { return status; }
    public void setStatus(CycleStatus status) { this.status = status; }

    public boolean isAutomaticPayoutEnabled() { return automaticPayoutEnabled; }
    public void setAutomaticPayoutEnabled(boolean automaticPayoutEnabled) {
        this.automaticPayoutEnabled = automaticPayoutEnabled;
    }

    public UserGroup getBeneficiary() { return beneficiary; }
    public void setBeneficiary(UserGroup beneficiary) { this.beneficiary = beneficiary; }

    public List<Contribution> getContributions() { return contributions; }
    public void setContributions(List<Contribution> contributions) { this.contributions = contributions; }
}