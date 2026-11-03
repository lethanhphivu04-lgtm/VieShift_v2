package vn.vieshift.entity;

public enum Role {
    CUSTOMER("Khách hàng"),
    VIEKER("Đối tác VieKer"),
    ADMIN("Quản trị viên tối cao"),
    STAFF("Nhân viên vận hành");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
