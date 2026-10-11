package model;

import jakarta.persistence.*;
import model.enums.CycleStatus;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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
public class Cycle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @Column(nullable = false)
    private Integer numero; // Número da rodada (Ex: Ciclo 1, Ciclo 2...)

    private LocalDate startDate;

    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    private CycleStatus status;

    // Membro do grupo que é o beneficiário (quem recebe o dinheiro) neste ciclo
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "beneficiary_id", nullable = false)
    private UserGroup beneficiary;

    @OneToMany(mappedBy = "cycle", cascade = CascadeType.ALL)
    private List<Contribution> contributions = new ArrayList<>();

    public Cycle(Integer id, Group group, Integer numero, LocalDate startDate, UserGroup beneficiary) {
        this.id = id;
        this.group = group;
        this.numero = numero;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.beneficiary = beneficiary;

    }

    public Cycle() {
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
    public UserGroup getBeneficiary() { return beneficiary; }
    public void setBeneficiary(UserGroup beneficiary) { this.beneficiary = beneficiary; }
    public List<Contribution> getContributions() { return contributions; }
    public void setContributions(List<Contribution> contributions) { this.contributions = contributions; }
}