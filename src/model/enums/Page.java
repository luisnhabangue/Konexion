package model.enums;

public enum Page {
    DASHBOARD("dashboard.fxml"), GROUPS("groups.fxml"), GROUP_DETAIL("group-detail.fxml"),
    HISTORY("history.fxml"), PROFILE("profile.fxml");
    public final String file;
    Page(String file) { this.file = file; }
}
