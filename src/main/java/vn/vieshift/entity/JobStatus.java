package vn.vieshift.entity;

public enum JobStatus {
    OPEN("Đang tìm đối tác VieKer", "badge-primary"),
    ACCEPTED("Đã nhận việc", "badge-info"),
    IN_PROGRESS("Đang thực hiện", "badge-warning"),
    PENDING_REVIEW("Chờ nghiệm thu", "badge-secondary"),
    COMPLETED("Đã hoàn thành", "badge-success"),
    CANCELLED("Đã hủy", "badge-dark"),
    DISPUTED("Đang khiếu nại", "badge-danger"),
    RESOLVED("Đã giải quyết", "badge-info");

    private final String displayName;
    private final String badgeClass;

    JobStatus(String displayName, String badgeClass) {
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
