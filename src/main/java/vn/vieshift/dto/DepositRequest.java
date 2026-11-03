package vn.vieshift.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class DepositRequest {

    @NotNull(message = "Vui lòng nhập số tiền muốn nạp")
    @DecimalMin(value = "20000", message = "Số tiền nạp tối thiểu là 20.000 đ")
    private BigDecimal amount = BigDecimal.valueOf(100000);

    private String paymentMethod = "QR_BANK";

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
}
