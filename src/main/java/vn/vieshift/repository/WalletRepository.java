package vn.vieshift.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.vieshift.entity.Wallet;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {
    Optional<Wallet> findByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.user.id = :userId")
    Optional<Wallet> findByUserIdForUpdate(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(w.heldAmount), 0) FROM Wallet w")
    BigDecimal sumTotalHeldAmount();

    @Query("SELECT COALESCE(SUM(w.balance), 0) FROM Wallet w")
    BigDecimal sumTotalBalance();
}
