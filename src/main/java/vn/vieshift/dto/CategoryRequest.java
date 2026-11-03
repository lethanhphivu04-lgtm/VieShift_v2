package vn.vieshift.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import vn.vieshift.entity.PricingMode;

import java.math.BigDecimal;

public class CategoryRequest {

    private Long id;

    @NotBlank(message = "Vui lòng nhập tên danh mục")
    private String name;

    private String description;

    private String icon = "bi-tools";

    @NotNull(message = "Vui lòng chọn hình thức tính giá")
    private PricingMode pricingMode = PricingMode.HOURLY;

    @NotNull(message = "Vui lòng nhập đơn giá cơ bản")
    @DecimalMin(value = "1000", message = "Đơn giá tối thiểu là 1.000 đ")
    private BigDecimal baseRate;

    @NotBlank(message = "Vui lòng nhập đơn vị tính")
    private String unitName = "giờ";

    private boolean active = true;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public PricingMode getPricingMode() {
        return pricingMode;
    }

    public void setPricingMode(PricingMode pricingMode) {
        this.pricingMode = pricingMode;
    }

    public BigDecimal getBaseRate() {
        return baseRate;
    }

    public void setBaseRate(BigDecimal baseRate) {
        this.baseRate = baseRate;
    }

    public String getUnitName() {
        return unitName;
    }

    public void setUnitName(String unitName) {
        this.unitName = unitName;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
