package vn.vieshift.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "product_reviews")
public class ProductReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, length = 100)
    private String reviewerName;

    @Column(length = 40)
    private String reviewerPhone;

    @Column(nullable = false)
    private Integer rating = 5;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String comment;

    @Column(nullable = false)
    private boolean verifiedPurchase = false;

    @Column(columnDefinition = "TEXT")
    private String adminReply;

    @Column
    private Boolean visible = true;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public ProductReview() {
    }

    public ProductReview(Product product, String reviewerName, String reviewerPhone,
                         Integer rating, String comment, boolean verifiedPurchase) {
        this.product = product;
        this.reviewerName = reviewerName;
        this.reviewerPhone = reviewerPhone;
        this.rating = rating != null ? Math.max(1, Math.min(5, rating)) : 5;
        this.comment = comment;
        this.verifiedPurchase = verifiedPurchase;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public String getReviewerName() {
        return reviewerName;
    }

    public void setReviewerName(String reviewerName) {
        this.reviewerName = reviewerName;
    }

    public String getReviewerPhone() {
        return reviewerPhone;
    }

    public void setReviewerPhone(String reviewerPhone) {
        this.reviewerPhone = reviewerPhone;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public boolean isVerifiedPurchase() {
        return verifiedPurchase;
    }

    public void setVerifiedPurchase(boolean verifiedPurchase) {
        this.verifiedPurchase = verifiedPurchase;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getAdminReply() {
        return adminReply;
    }

    public void setAdminReply(String adminReply) {
        this.adminReply = adminReply;
    }

    public boolean isVisible() {
        return visible == null || visible;
    }

    public void setVisible(Boolean visible) {
        this.visible = visible;
    }
}
