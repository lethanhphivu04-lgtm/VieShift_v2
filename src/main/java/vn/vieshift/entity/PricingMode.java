package vn.vieshift.entity;

public enum PricingMode {
    HOURLY("Theo giờ", "giờ"),
    DISTANCE("Theo khoảng cách", "km");

    private final String label;
    private final String unit;

    PricingMode(String label, String unit) {
        this.label = label;
        this.unit = unit;
    }

    public String getLabel() {
        return label;
    }

    public String getDisplayName() {
        return label;
    }

    public String getUnit() {
        return unit;
    }
}
