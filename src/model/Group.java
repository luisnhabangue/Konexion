package model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tb_group") // Evita palavra reservada 'group' no MySQL
public class Group {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long groupId;

    @Column(nullable = false, unique = true)
    private String groupName;

    @Column(nullable = false, length = 150)
    private String groupDescription;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amountPerUser;

    @Column(name = "payday", nullable = false)
    private Integer payday;

    @Column(name = "created_at", nullable = false)
    private LocalDate createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdBy;

    private int frequency;


    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserGroup> members = new ArrayList<>();

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Cycle> cycles = new ArrayList<>();




    public Group() {
    }

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

    public void addCycle(Cycle cycle) {
        this.cycles.add(cycle);
        cycle.setGroup(this);
    }

    public void removeCycle(Cycle cycle) {
        this.cycles.remove(cycle);
        cycle.setGroup(null);
    }

    public void addMember(UserGroup userGroup) {
        this.members.add(userGroup);
        userGroup.setGroup(this);
    }

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
    public List<UserGroup> getMembers() { return members; }
    public void setMembers(List<UserGroup> members) { this.members = members; }
    public List<Cycle> getCycles() { return cycles; }
    public void setCycles(List<Cycle> cycles) { this.cycles = cycles; }
}