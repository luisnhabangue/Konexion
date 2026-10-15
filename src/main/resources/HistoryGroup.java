//package model;
//
//import jakarta.persistence.*;
//import model.enums.HistoryGroupStatus;
//
//import java.sql.Date;
//
//@Entity
//@Table(name = "history_Group")
//public class HistoryGroup {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "grupo_id", nullable = false)
//    private Group groupId;
//
//    @Column(nullable = false, length = 50)
//    private String groupDescription;
//
//    @Enumerated(EnumType.STRING)
//    private HistoryGroupStatus historyGroupStatus;
//
//    @Column(name = "created_at", nullable = false)
//    private Date createdAt;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    private User createdBy;
//
//    public HistoryGroup(Group groupId, String groupDescription, HistoryGroupStatus historyGroupStatus, Date createdAt, User createdBy) {
//        this.groupId = groupId;
//        this.groupDescription = groupDescription;
//        this.historyGroupStatus = historyGroupStatus;
//        this.createdAt = createdAt;
//        this.createdBy = createdBy;
//    }
//
//    public HistoryGroup() {
//
//    }
//
//    public Long getId() {
//        return id;
//    }
//
//    public void setId(Long id) {
//        this.id = id;
//    }
//
//    public Group getGroupId() {
//        return groupId;
//    }
//
//    public void setGroupId(Group groupId) {
//        this.groupId = groupId;
//    }
//
//    public String getGroupDescription() {
//        return groupDescription;
//    }
//
//    public void setGroupDescription(String groupDescription) {
//        this.groupDescription = groupDescription;
//    }
//
//    public HistoryGroupStatus getHistoryGroupStatus() {
//        return historyGroupStatus;
//    }
//
//    public void setHistoryGroupStatus(HistoryGroupStatus historyGroupStatus) {
//        this.historyGroupStatus = historyGroupStatus;
//    }
//
//    public Date getCreatedAt() {
//        return createdAt;
//    }
//
//    public void setCreatedAt(Date createdAt) {
//        this.createdAt = createdAt;
//    }
//
//    public User getCreatedBy() {
//        return createdBy;
//    }
//
//    public void setCreatedBy(User createdBy) {
//        this.createdBy = createdBy;
//    }
//}
