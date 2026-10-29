package vn.vieshift.entity;

public enum NotificationType {
    JOB_NEW("Việc mới trên sàn", "bi-lightning-charge-fill", "#F59E0B"),
    JOB_ACCEPTED("Đối tác VieKer đã nhận việc", "bi-person-check-fill", "#0284C7"),
    JOB_COMPLETED("Đối tác VieKer báo hoàn thành", "bi-hourglass-bottom", "#8B5CF6"),
    JOB_APPROVED("Nghiệm thu thành công", "bi-patch-check-fill", "#10B981"),
    JOB_CANCELLED("Công việc bị hủy", "bi-x-circle-fill", "#EF4444"),
    DISPUTE_UPDATE("Cập nhật khiếu nại", "bi-shield-exclamation", "#F97316"),
    KYC_UPDATE("Xét duyệt hồ sơ Đối tác VieKer", "bi-person-badge-fill", "#059669"),
    WALLET_UPDATE("Biến động số dư ví", "bi-wallet2", "#10B981"),
    SYSTEM("Thông báo hệ thống", "bi-bell-fill", "#C24D63");

    private final String displayName;
    private final String icon;
    private final String color;

    NotificationType(String displayName, String icon, String color) {
        this.displayName = displayName;
        this.icon = icon;
        this.color = color;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getIcon() {
        return icon;
    }

    public String getColor() {
        return color;
    }
}
