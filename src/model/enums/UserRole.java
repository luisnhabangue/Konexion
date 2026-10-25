package model.enums;

/** Account-wide access roles. Group management is scoped to each group, not this profile. */
public enum UserRole {

    ADMINISTRATOR,
    USER;

    public boolean isAdministrator() {
        return this == ADMINISTRATOR;
    }
}