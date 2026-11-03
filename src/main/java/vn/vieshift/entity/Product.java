package vn.vieshift.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "shop_products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(length = 100)
    private String relatedServiceCategory;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(precision = 12, scale = 2)
    private BigDecimal originalPrice;

    @Column(nullable = false, length = 40)
    private String unit = "Cái";

    @Column(nullable = false)
    private Integer stock = 100;

    @Column(nullable = false)
    private Integer soldCount = 0;

    @Column(length = 500)
    private String imageUrl;

    @Column(length = 64)
    private String checksum;

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private boolean active = true;

    @Column
    private Boolean deleted = false;

    public boolean isDeleted() {
        return Boolean.TRUE.equals(deleted);
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Product() {
    }

    public Product(String name, String category, String relatedServiceCategory,
                   BigDecimal price, BigDecimal originalPrice, String unit,
                   Integer stock, Integer soldCount, String imageUrl, String description) {
        this.name = name;
        this.category = category;
        this.relatedServiceCategory = relatedServiceCategory;
        this.price = price;
        this.originalPrice = originalPrice;
        this.unit = unit;
        this.stock = stock;
        this.soldCount = soldCount;
        this.imageUrl = imageUrl;
        this.description = description;
        this.active = true;
        this.createdAt = LocalDateTime.now();
    }

    public Product(String name, String category, String description,
                   BigDecimal price, BigDecimal originalPrice,
                   Integer stockQuantity, String unit,
                   String imageUrl, String relatedServiceCategory) {
        this.name = name;
        this.category = category;
        this.description = description;
        this.price = price;
        this.originalPrice = originalPrice;
        this.stock = stockQuantity != null ? stockQuantity : 50;
        this.unit = unit != null ? unit : "Bộ";
        this.imageUrl = imageUrl;
        this.relatedServiceCategory = relatedServiceCategory;
        this.soldCount = 0;
        this.active = true;
        this.createdAt = LocalDateTime.now();
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getRelatedServiceCategory() {
        return relatedServiceCategory;
    }

    public void setRelatedServiceCategory(String relatedServiceCategory) {
        this.relatedServiceCategory = relatedServiceCategory;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getOriginalPrice() {
        return originalPrice;
    }

    public void setOriginalPrice(BigDecimal originalPrice) {
        this.originalPrice = originalPrice;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public Integer getStockQuantity() {
        return stock;
    }

    public void setStockQuantity(Integer stockQuantity) {
        this.stock = stockQuantity;
    }

    public int getDiscountPercent() {
        if (originalPrice == null || price == null || originalPrice.compareTo(price) <= 0 || originalPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        return originalPrice.subtract(price)
                .multiply(BigDecimal.valueOf(100))
                .divide(originalPrice, 0, java.math.RoundingMode.HALF_UP)
                .intValue();
    }

    public Integer getSoldCount() {
        return soldCount;
    }

    public void setSoldCount(Integer soldCount) {
        this.soldCount = soldCount;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
