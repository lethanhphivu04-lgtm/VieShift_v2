package vn.vieshift.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "wallets")
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal heldAmount = BigDecimal.ZERO;

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Wallet() {
    }

    public Wallet(User user) {
        this.user = user;
        this.balance = BigDecimal.ZERO;
        this.heldAmount = BigDecimal.ZERO;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public BigDecimal getHeldAmount() {
        return heldAmount;
    }

    public void setHeldAmount(BigDecimal heldAmount) {
        this.heldAmount = heldAmount;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Tiện ích tính toán số dư thực tế
    public BigDecimal getTotalAsset() {
        return balance.add(heldAmount);
    }

    public boolean canAfford(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        return this.balance.compareTo(amount) >= 0;
    }

    public void hold(BigDecimal amount) {
        if (!canAfford(amount)) {
            throw new IllegalStateException("Số dư khả dụng không đủ để ký quỹ Escrow: " + amount);
        }
        this.balance = this.balance.subtract(amount);
        this.heldAmount = this.heldAmount.add(amount);
        this.updatedAt = LocalDateTime.now();
    }

    public void releaseHold(BigDecimal amount) {
        if (this.heldAmount.compareTo(amount) < 0) {
            throw new IllegalStateException("Số tiền ký quỹ hiện tại không đủ để giải ngân: " + amount);
        }
        this.heldAmount = this.heldAmount.subtract(amount);
        this.updatedAt = LocalDateTime.now();
    }

    public void refundHold(BigDecimal amount) {
        releaseHold(amount);
        this.balance = this.balance.add(amount);
        this.updatedAt = LocalDateTime.now();
    }

    public void addBalance(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Số tiền thêm vào ví phải lớn hơn 0");
        }
        this.balance = this.balance.add(amount);
        this.updatedAt = LocalDateTime.now();
    }

    public void subtractBalance(BigDecimal amount) {
        if (!canAfford(amount)) {
            throw new IllegalStateException("Số dư khả dụng không đủ để trừ: " + amount);
        }
        this.balance = this.balance.subtract(amount);
        this.updatedAt = LocalDateTime.now();
    }
}
