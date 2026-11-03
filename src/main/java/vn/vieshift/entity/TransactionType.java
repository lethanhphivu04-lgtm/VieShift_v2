package vn.vieshift.entity;

public enum TransactionType {
    DEPOSIT("Nạp tiền vào ví", "+"),
    ESCROW_HOLD("Bảo đảm thanh toán đơn hàng", "-"),
    ESCROW_RELEASE("Nhận thanh toán hoàn thành việc", "+"),
    ESCROW_REFUND("Hoàn tiền đơn hàng về ví", "+"),
    PLATFORM_FEE("Phí dịch vụ nền tảng", "-"),
    WITHDRAW("Rút tiền về tài khoản ngân hàng", "-"),
    TIP_SENT("Tiền boa cho Đối tác VieKer", "-"),
    TIP_RECEIVED("Nhận tiền boa từ Khách hàng", "+"),
    SHOP_PURCHASE("Mua vật tư & thiết bị VieShift Mall", "-"),
    SHOP_REFUND("Hoàn tiền đơn hàng vật tư", "+");

    private final String description;
    private final String sign;

    TransactionType(String description, String sign) {
        this.description = description;
        this.sign = sign;
    }

    public String getDescription() {
        return description;
    }

    public String getDisplayName() {
        return description;
    }

    public String getSign() {
        return sign;
    }
}
