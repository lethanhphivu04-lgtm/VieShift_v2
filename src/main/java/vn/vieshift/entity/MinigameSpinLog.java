package vn.vieshift.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "minigame_spin_logs")
public class MinigameSpinLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(length = 20)
    private String phone;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "slice_id", nullable = false)
    private MinigameSlice slice;

    @Column(nullable = false, length = 30)
    private String rewardType; // COUPON, GIFT, MISS

    @Column(length = 255)
    private String rewardDetail;

    @Column(length = 64)
    private String checksum; // Chữ ký số HMAC-SHA256 chống sửa trực tiếp trong CSDL

    @Column
    private Boolean claimed = false;

    @Column(length = 40)
    private String claimMethod; // WALLET, SHIP_GIFT, VOUCHER

    @Column(length = 255)
    private String claimedNote;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public MinigameSpinLog() {}

    public MinigameSpinLog(User user, String phone, MinigameSlice slice, String rewardType, String rewardDetail) {
        this.user = user;
        this.phone = phone;
        this.slice = slice;
        this.rewardType = rewardType;
        this.rewardDetail = rewardDetail;
        this.claimed = false;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public MinigameSlice getSlice() { return slice; }
    public void setSlice(MinigameSlice slice) { this.slice = slice; }

    public String getRewardType() { return rewardType; }
    public void setRewardType(String rewardType) { this.rewardType = rewardType; }

    public String getRewardDetail() { return rewardDetail; }
    public void setRewardDetail(String rewardDetail) { this.rewardDetail = rewardDetail; }

    public String getChecksum() { return checksum; }
    public void setChecksum(String checksum) { this.checksum = checksum; }

    public boolean isClaimed() { return Boolean.TRUE.equals(claimed); }
    public Boolean getClaimed() { return claimed != null ? claimed : false; }
    public void setClaimed(Boolean claimed) { this.claimed = claimed; }

    public String getClaimMethod() { return claimMethod; }
    public void setClaimMethod(String claimMethod) { this.claimMethod = claimMethod; }

    public String getClaimedNote() { return claimedNote; }
    public void setClaimedNote(String claimedNote) { this.claimedNote = claimedNote; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
