package vn.vieshift.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PaymentResult {
    private boolean success;
    private String txnRef;
    private BigDecimal amount;
    private String orderInfo;
    private String bankCode;
    private String transactionNo;
    private String responseCode;
    private String message;
    private LocalDateTime payDate;

    public PaymentResult() {
    }

    public PaymentResult(boolean success, String txnRef, BigDecimal amount, String orderInfo,
                         String bankCode, String transactionNo, String responseCode,
                         String message, LocalDateTime payDate) {
        this.success = success;
        this.txnRef = txnRef;
        this.amount = amount;
        this.orderInfo = orderInfo;
        this.bankCode = bankCode;
        this.transactionNo = transactionNo;
        this.responseCode = responseCode;
        this.message = message;
        this.payDate = payDate;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getTxnRef() {
        return txnRef;
    }

    public void setTxnRef(String txnRef) {
        this.txnRef = txnRef;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getOrderInfo() {
        return orderInfo;
    }

    public void setOrderInfo(String orderInfo) {
        this.orderInfo = orderInfo;
    }

    public String getBankCode() {
        return bankCode;
    }

    public void setBankCode(String bankCode) {
        this.bankCode = bankCode;
    }

    public String getTransactionNo() {
        return transactionNo;
    }

    public void setTransactionNo(String transactionNo) {
        this.transactionNo = transactionNo;
    }

    public String getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(String responseCode) {
        this.responseCode = responseCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getPayDate() {
        return payDate;
    }

    public void setPayDate(LocalDateTime payDate) {
        this.payDate = payDate;
    }
}
