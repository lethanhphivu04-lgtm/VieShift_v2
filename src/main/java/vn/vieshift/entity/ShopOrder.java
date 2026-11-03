package vn.vieshift.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "shop_orders")
public class ShopOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String orderCode = "VS-MALL-" + (System.currentTimeMillis() % 1000000) + "-" + (int)(Math.random() * 900 + 100);

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "job_id")
    private Job relatedJob;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String itemsSummary;

    @Column(precision = 14, scale = 2)
    private BigDecimal subtotalAmount = BigDecimal.ZERO;

    @Column(precision = 14, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(precision = 14, scale = 2)
    private BigDecimal shippingFee = BigDecimal.ZERO;

    @Column(length = 40)
    private String voucherCode;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false)
    private Integer totalItems = 1;

    @Column(length = 120)
    private String recipientName;

    @Column(nullable = false, length = 300)
    private String shippingAddress;

    @Column(length = 30)
    private String recipientPhone;

    @Column(length = 300)
    private String notes;

    @Column(length = 30)
    private String paymentMethod = "WALLET"; // WALLET, COD, VNPAY

    @Column(length = 80)
    private String shippingMethodName = "🚀 GHN Giao Nhanh Chuẩn";

    @Column(length = 30)
    private String installationMode = "SELF_INSTALL"; // SELF_INSTALL, NEED_VIEKER

    @Column(nullable = false, length = 30)
    private String status = "PAID"; // PENDING_COD, PAID, DELIVERING, COMPLETED, CANCELLED

    @Column(nullable = false, length = 30)
    private String orderType = "STANDALONE"; // STANDALONE or JOB_ADDON

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public ShopOrder() {
    }

    public ShopOrder(String orderCode, User customer, Job relatedJob, String itemsSummary,
                     BigDecimal totalAmount, String shippingAddress, String recipientPhone,
                     String notes, String orderType) {
        this.orderCode = orderCode;
        this.customer = customer;
        this.relatedJob = relatedJob;
        this.itemsSummary = itemsSummary;
        this.subtotalAmount = totalAmount;
        this.totalAmount = totalAmount;
        this.shippingAddress = shippingAddress;
        this.recipientPhone = recipientPhone;
        this.recipientName = customer != null ? customer.getFullName() : "Khách hàng";
        this.notes = notes;
        this.orderType = orderType != null ? orderType : "STANDALONE";
        this.status = "PAID";
        this.paymentMethod = "WALLET";
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrderCode() {
        return orderCode;
    }

    public void setOrderCode(String orderCode) {
        this.orderCode = orderCode;
    }

    public User getCustomer() {
        return customer;
    }

    public void setCustomer(User customer) {
        this.customer = customer;
    }

    public Job getRelatedJob() {
        return relatedJob;
    }

    public void setRelatedJob(Job relatedJob) {
        this.relatedJob = relatedJob;
    }

    public String getItemsSummary() {
        return itemsSummary;
    }

    public void setItemsSummary(String itemsSummary) {
        this.itemsSummary = itemsSummary;
    }

    public BigDecimal getSubtotalAmount() {
        return subtotalAmount != null ? subtotalAmount : totalAmount;
    }

    public void setSubtotalAmount(BigDecimal subtotalAmount) {
        this.subtotalAmount = subtotalAmount;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount != null ? discountAmount : BigDecimal.ZERO;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public BigDecimal getShippingFee() {
        return shippingFee != null ? shippingFee : BigDecimal.ZERO;
    }

    public void setShippingFee(BigDecimal shippingFee) {
        this.shippingFee = shippingFee;
    }

    public String getVoucherCode() {
        return voucherCode;
    }

    public void setVoucherCode(String voucherCode) {
        this.voucherCode = voucherCode;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Integer getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(Integer totalItems) {
        this.totalItems = totalItems;
    }

    public String getRecipientName() {
        if (recipientName != null && !recipientName.isBlank()) {
            return recipientName;
        }
        return customer != null ? customer.getFullName() : "Khách hàng";
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public String getRecipientPhone() {
        return recipientPhone;
    }

    public void setRecipientPhone(String recipientPhone) {
        this.recipientPhone = recipientPhone;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getPaymentMethod() {
        return paymentMethod != null ? paymentMethod : "WALLET";
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getPaymentMethodDisplayName() {
        String pm = getPaymentMethod().toUpperCase();
        return switch (pm) {
            case "COD" -> "Thanh toán khi nhận hàng (COD)";
            case "VNPAY" -> "Cổng VNPAY Online";
            default -> "Ví bảo đảm VieShift";
        };
    }

    public String getInstallationMode() {
        return installationMode != null ? installationMode : "SELF_INSTALL";
    }

    public void setInstallationMode(String installationMode) {
        this.installationMode = installationMode;
    }

    public String getInstallationModeDisplayName() {
        return "NEED_VIEKER".equalsIgnoreCase(getInstallationMode())
                ? "Cần thợ VieKer hỗ trợ lắp"
                : "Tự lắp đặt tại nhà";
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getOrderType() {
        return orderType;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getShippingMethodName() {
        return shippingMethodName != null && !shippingMethodName.isBlank()
                ? shippingMethodName
                : "🚀 GHN Giao Nhanh Chuẩn";
    }

    public void setShippingMethodName(String shippingMethodName) {
        this.shippingMethodName = shippingMethodName;
    }

    public String getStatusDisplayName() {
        return switch (status) {
            case "PENDING_VNPAY" -> "Chờ thanh toán VNPAY (30p)";
            case "PENDING_COD" -> "Chờ giao (Thu hộ COD)";
            case "PAID" -> "Đã thanh toán (Chờ giao)";
            case "DELIVERING", "SHIPPING" -> "Đang giao vật tư (GHN)";
            case "COMPLETED" -> "Đã nhận hàng (Bảo hành 1 đổi 1)";
            case "CANCELLED" -> "Đã hủy đơn";
            default -> status;
        };
    }

    public String getStatusBadgeClass() {
        return switch (status) {
            case "COMPLETED" -> "badge-success";
            case "DELIVERING", "SHIPPING" -> "badge-info";
            case "CANCELLED" -> "badge-danger";
            default -> "badge-warning";
        };
    }
}
