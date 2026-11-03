package vn.vieshift.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "job_categories")
public class JobCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 50)
    private String icon = "bi-briefcase";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PricingMode pricingMode = PricingMode.HOURLY;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal baseRate;

    @Column(nullable = false, length = 20)
    private String unitName = "giờ";

    @Column(nullable = false)
    private boolean active = true;

    public JobCategory() {
    }

    public JobCategory(String name, String description, String icon, PricingMode pricingMode, BigDecimal baseRate, String unitName) {
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.pricingMode = pricingMode;
        this.baseRate = baseRate;
        this.unitName = unitName;
        this.active = true;
    }

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
