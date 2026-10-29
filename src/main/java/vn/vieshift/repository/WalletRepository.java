package vn.vieshift.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import vn.vieshift.entity.Wallet;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {
    Optional<Wallet> findByUserId(Long userId);

    @Query("SELECT COALESCE(SUM(w.heldAmount), 0) FROM Wallet w")
    BigDecimal sumTotalHeldAmount();

    @Query("SELECT COALESCE(SUM(w.balance), 0) FROM Wallet w")
    BigDecimal sumTotalBalance();
}
