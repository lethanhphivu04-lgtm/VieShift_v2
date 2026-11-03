package vn.vieshift.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.vieshift.entity.StockReceipt;

import java.util.List;

@Repository
public interface StockReceiptRepository extends JpaRepository<StockReceipt, Long> {
    List<StockReceipt> findAllByOrderByCreatedAtDesc();
}
