package vn.vieshift.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class WithdrawRequest {

    @NotNull(message = "Vui lòng nhập số tiền rút")
    @DecimalMin(value = "50000", message = "Số tiền rút tối thiểu là 50.000 đ")
    private BigDecimal amount = BigDecimal.valueOf(100000);

    @NotBlank(message = "Vui lòng nhập tên ngân hàng")
    private String bankName;

    @NotBlank(message = "Vui lòng nhập số tài khoản nhận")
    private String accountNumber;

    @NotBlank(message = "Vui lòng nhập tên chủ tài khoản")
    private String accountHolder;

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getAccountHolder() {
        return accountHolder;
    }

    public void setAccountHolder(String accountHolder) {
        this.accountHolder = accountHolder;
    }
}
