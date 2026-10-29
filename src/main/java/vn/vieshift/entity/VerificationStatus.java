package vn.vieshift.entity;

public enum VerificationStatus {
    DRAFT("Chưa hoàn thiện hồ sơ", "badge-info"),
    PENDING("Chờ phê duyệt", "badge-warning"),
    NEEDS_UPDATE("Cần bổ sung", "badge-warning"),
    APPROVED("Đã xác minh", "badge-success"),
    REJECTED("Bị từ chối", "badge-danger");

    private final String displayName;
    private final String badgeClass;

    VerificationStatus(String displayName, String badgeClass) {
        this.displayName = displayName;
        this.badgeClass = badgeClass;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getBadgeClass() {
        return badgeClass;
    }
}
