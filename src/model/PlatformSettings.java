package model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "platform_settings")
public class PlatformSettings {
    @Id
    private Integer id = 1;

    @Column(nullable = false)
    private int latePenaltyPoints = 10;

    @Column(nullable = false)
    private int reminderDaysBefore = 1;

    @Column(nullable = false)
    private boolean remindersEnabled = true;

    @Column(nullable = false)
    private boolean overdueNotificationsEnabled = true;

    @Column(nullable = false)
    private boolean latePaymentNotificationsEnabled = true;

    public Integer getId() { return id; }
    public int getLatePenaltyPoints() { return latePenaltyPoints; }
    public void setLatePenaltyPoints(int latePenaltyPoints) { this.latePenaltyPoints = latePenaltyPoints; }
    public int getReminderDaysBefore() { return reminderDaysBefore; }
    public void setReminderDaysBefore(int reminderDaysBefore) { this.reminderDaysBefore = reminderDaysBefore; }
    public boolean isRemindersEnabled() { return remindersEnabled; }
    public void setRemindersEnabled(boolean remindersEnabled) { this.remindersEnabled = remindersEnabled; }
    public boolean isOverdueNotificationsEnabled() { return overdueNotificationsEnabled; }
    public void setOverdueNotificationsEnabled(boolean enabled) { this.overdueNotificationsEnabled = enabled; }
    public boolean isLatePaymentNotificationsEnabled() { return latePaymentNotificationsEnabled; }
    public void setLatePaymentNotificationsEnabled(boolean enabled) {
        this.latePaymentNotificationsEnabled = enabled;
    }
}
