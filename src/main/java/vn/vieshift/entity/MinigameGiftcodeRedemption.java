package vn.vieshift.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "minigame_giftcode_redemptions")
public class MinigameGiftcodeRedemption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "giftcode_id", nullable = false)
    private MinigameGiftcode giftcode;

    @Column(length = 20, nullable = false)
    private String phone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private Integer spinCountAwarded;

    @Column(nullable = false)
    private LocalDateTime redeemedAt = LocalDateTime.now();

    public MinigameGiftcodeRedemption() {}

    public MinigameGiftcodeRedemption(MinigameGiftcode giftcode, String phone, User user, Integer spinCountAwarded) {
        this.giftcode = giftcode;
        this.phone = phone;
        this.user = user;
        this.spinCountAwarded = spinCountAwarded;
        this.redeemedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public MinigameGiftcode getGiftcode() { return giftcode; }
    public void setGiftcode(MinigameGiftcode giftcode) { this.giftcode = giftcode; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Integer getSpinCountAwarded() { return spinCountAwarded; }
    public void setSpinCountAwarded(Integer spinCountAwarded) { this.spinCountAwarded = spinCountAwarded; }

    public LocalDateTime getRedeemedAt() { return redeemedAt; }
    public void setRedeemedAt(LocalDateTime redeemedAt) { this.redeemedAt = redeemedAt; }
}
