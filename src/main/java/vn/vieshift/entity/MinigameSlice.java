package vn.vieshift.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "minigame_slices")
public class MinigameSlice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String label;

    @Column(nullable = false, length = 30)
    private String type; // COUPON, GIFT, MISS

    @Column(precision = 12, scale = 2)
    private BigDecimal rewardValue = BigDecimal.ZERO;

    @Column(length = 60)
    private String couponCode;

    @Column(nullable = false)
    private Integer winRate = 10; // Tỷ lệ trúng theo % trọng số

    @Column(nullable = false, length = 20)
    private String color = "#C24D63";

    @Column(length = 255)
    private String message;

    @Column(nullable = false)
    private Integer sliceIndex = 0;

    @Column(nullable = false)
    private boolean active = true;

    public MinigameSlice() {}

    public MinigameSlice(String label, String type, BigDecimal rewardValue, String couponCode, Integer winRate, String color, String message, Integer sliceIndex) {
        this.label = label;
        this.type = type;
        this.rewardValue = rewardValue;
        this.couponCode = couponCode;
        this.winRate = winRate;
        this.color = color;
        this.message = message;
        this.sliceIndex = sliceIndex;
        this.active = true;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public BigDecimal getRewardValue() { return rewardValue; }
    public void setRewardValue(BigDecimal rewardValue) { this.rewardValue = rewardValue; }

    public String getCouponCode() { return couponCode; }
    public void setCouponCode(String couponCode) { this.couponCode = couponCode; }

    public Integer getWinRate() { return winRate; }
    public void setWinRate(Integer winRate) { this.winRate = winRate; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Integer getSliceIndex() { return sliceIndex; }
    public void setSliceIndex(Integer sliceIndex) { this.sliceIndex = sliceIndex; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
