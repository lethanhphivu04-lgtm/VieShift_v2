package vn.vieshift.entity;

public enum DisputeStatus {
    OPEN("Chờ Admin xử lý"),
    RESOLVED("Đã giải quyết");

    private final String displayName;

    DisputeStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
