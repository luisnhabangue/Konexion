package model;

import model.enums.UserRole;

public record AdminUserSummary(Long id, String firstName, String lastName, String username,
                               UserRole userRole, boolean active, boolean verified,
                               int strikeCount, int punctualityPoints) {
}
