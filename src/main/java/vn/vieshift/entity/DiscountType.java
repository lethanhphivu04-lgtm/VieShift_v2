package vn.vieshift.entity;

public enum DiscountType {
    PERCENTAGE("Giảm theo %"),
    FIXED_AMOUNT("Giảm số tiền cụ thể");

    private final String displayName;

    DiscountType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
