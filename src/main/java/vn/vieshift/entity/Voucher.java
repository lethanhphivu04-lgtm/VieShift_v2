package vn.vieshift.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Table(name = "vouchers")
public class Voucher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DiscountType discountType = DiscountType.PERCENTAGE;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal discountValue;

    @Column(precision = 12, scale = 2)
    private BigDecimal maxDiscountAmount;

    @Column(precision = 12, scale = 2)
    private BigDecimal minOrderAmount = BigDecimal.ZERO;

    private Integer maxUses;

    @Column(nullable = false)
    private Integer currentUses = 0;

    @Column(nullable = false)
    private LocalDateTime startDate = LocalDateTime.now();

    private LocalDateTime endDate;

    /**
     * true = Hiển thị công khai ở chỗ người dùng chọn 1-chạm
     * false = Ẩn, người dùng phải tự nhập mã (Ưu đãi nhập code)
     */
    @Column(nullable = false)
    private boolean isPublic = true;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Voucher() {
    }

    public Voucher(String code, String title, DiscountType discountType, BigDecimal discountValue,
                   BigDecimal minOrderAmount, LocalDateTime endDate, boolean isPublic) {
        this.code = code != null ? code.trim().toUpperCase() : "";
        this.title = title;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.minOrderAmount = minOrderAmount != null ? minOrderAmount : BigDecimal.ZERO;
        this.endDate = endDate;
        this.isPublic = isPublic;
        this.active = true;
        this.currentUses = 0;
        this.startDate = LocalDateTime.now();
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Kiểm tra tính hợp lệ của voucher với số tiền đơn hàng
     */
    public boolean isValidForOrder(BigDecimal orderAmount) {
        if (!active) return false;
        LocalDateTime now = LocalDateTime.now();
        if (startDate != null && now.isBefore(startDate)) return false;
        if (endDate != null && now.isAfter(endDate)) return false;
        if (maxUses != null && currentUses >= maxUses) return false;
        if (minOrderAmount != null && orderAmount != null && orderAmount.compareTo(minOrderAmount) < 0) return false;
        return true;
    }

    /**
     * Tính số tiền được giảm giá
     */
    public BigDecimal calculateDiscount(BigDecimal orderAmount) {
        if (orderAmount == null || orderAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal discount = BigDecimal.ZERO;
        if (discountType == DiscountType.PERCENTAGE) {
            BigDecimal rate = discountValue.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            discount = orderAmount.multiply(rate).setScale(0, RoundingMode.HALF_UP);
            if (maxDiscountAmount != null && maxDiscountAmount.compareTo(BigDecimal.ZERO) > 0) {
                if (discount.compareTo(maxDiscountAmount) > 0) {
                    discount = maxDiscountAmount;
                }
            }
        } else if (discountType == DiscountType.FIXED_AMOUNT) {
            discount = discountValue;
        }

        if (discount.compareTo(orderAmount) > 0) {
            discount = orderAmount;
        }

        return discount.setScale(0, RoundingMode.HALF_UP);
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code != null ? code.trim().toUpperCase() : null;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public DiscountType getDiscountType() {
        return discountType;
    }

    public void setDiscountType(DiscountType discountType) {
        this.discountType = discountType;
    }

    public BigDecimal getDiscountValue() {
        return discountValue;
    }

    public void setDiscountValue(BigDecimal discountValue) {
        this.discountValue = discountValue;
    }

    public BigDecimal getMaxDiscountAmount() {
        return maxDiscountAmount;
    }

    public void setMaxDiscountAmount(BigDecimal maxDiscountAmount) {
        this.maxDiscountAmount = maxDiscountAmount;
    }

    public BigDecimal getMinOrderAmount() {
        return minOrderAmount;
    }

    public void setMinOrderAmount(BigDecimal minOrderAmount) {
        this.minOrderAmount = minOrderAmount;
    }

    public Integer getMaxUses() {
        return maxUses;
    }

    public void setMaxUses(Integer maxUses) {
        this.maxUses = maxUses;
    }

    public Integer getCurrentUses() {
        return currentUses;
    }

    public void setCurrentUses(Integer currentUses) {
        this.currentUses = currentUses;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean aPublic) {
        isPublic = aPublic;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
