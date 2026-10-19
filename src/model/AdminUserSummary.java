package model;

import model.enums.UserRole;
/**
 * Entidade ou objeto de domínio que representa os dados e conceitos centrais da aplicação.
 *
 * @since 1.0
 */

public record AdminUserSummary(Long id, String firstName, String lastName, String username,
                               UserRole userRole, boolean active, boolean verified,
                               int strikeCount, int punctualityPoints) {
}
