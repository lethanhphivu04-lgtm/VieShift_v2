package vn.vieshift.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.vieshift.entity.PaymentTransaction;

import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    Optional<PaymentTransaction> findByTxnRef(String txnRef);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT pt FROM PaymentTransaction pt WHERE pt.txnRef = :txnRef")
    Optional<PaymentTransaction> findByTxnRefForUpdate(@Param("txnRef") String txnRef);
}
