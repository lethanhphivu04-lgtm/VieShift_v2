package vn.vieshift.entity;

public enum Role {
    CUSTOMER("Khách hàng"),
    VIEKER("Đối tác VieKer"),
    ADMIN("Quản trị viên");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
