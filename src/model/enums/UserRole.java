package model.enums;

/** Account access roles. Legacy values remain for existing persisted rows. */
public enum UserRole {

    ADMINISTRATOR,
    USER,
    GROUP_LEADER,

    // Legacy values remain readable for existing database rows.
    admin,
    group_manager,
    member,
    base;

    public boolean isAdministrator() {
        return this == ADMINISTRATOR || this == admin;
    }

    public boolean isGroupLeader() {
        return this == GROUP_LEADER || this == group_manager;
    }
}