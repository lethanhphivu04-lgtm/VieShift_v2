package vn.vieshift.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CreateJobRequest {

    @NotNull(message = "Vui lòng chọn danh mục dịch vụ")
    private Long categoryId;

    @NotBlank(message = "Vui lòng nhập tiêu đề công việc")
    private String title;

    @NotBlank(message = "Vui lòng nhập mô tả chi tiết yêu cầu")
    private String description;

    @NotBlank(message = "Vui lòng nhập địa chỉ thực hiện")
    private String address;

    @NotNull(message = "Vui lòng chọn thời gian bắt đầu")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime scheduledAt = LocalDateTime.now().plusHours(2);

    @DecimalMin(value = "0.5", message = "Thời lượng tối thiểu 0.5 giờ")
    private BigDecimal durationHours = BigDecimal.valueOf(2.0);

    private BigDecimal distanceKm = BigDecimal.valueOf(3.0);

    private String notes;

    private String voucherCode;

    private BigDecimal discountAmount;

    public String getVoucherCode() {
        return voucherCode;
    }

    public void setVoucherCode(String voucherCode) {
        this.voucherCode = voucherCode;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public LocalDateTime getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(LocalDateTime scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public BigDecimal getDurationHours() {
        return durationHours;
    }

    public void setDurationHours(BigDecimal durationHours) {
        this.durationHours = durationHours;
    }

    public BigDecimal getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(BigDecimal distanceKm) {
        this.distanceKm = distanceKm;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
