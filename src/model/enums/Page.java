package model.enums;
/**
 * Enumeração dos valores válidos para o conceito indicado pelo nome da classe.
 *
 * @since 1.0
 */

public enum Page {
    DASHBOARD("dashboard.fxml"), GROUPS("groups.fxml"), GROUP_DETAIL("group-detail.fxml"),
    HISTORY("history.fxml"), PROFILE("profile.fxml"),
    ADMIN("admin-dashboard.fxml"),
    ADMIN_REPORTS("admin-reports.fxml"),
    ADMIN_RULES("admin-rules.fxml"),
    ADMIN_COMPLAINTS("admin-complaints.fxml"),
    ADMIN_KYC("admin-kyc.fxml"),
    ADMIN_USERS("admin-users.fxml"),
    ADMIN_GROUPS("admin-groups.fxml");
    public final String file;
    Page(String file) { this.file = file; }
}
