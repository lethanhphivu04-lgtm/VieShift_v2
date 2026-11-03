package vn.vieshift.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import vn.vieshift.entity.ShopOrder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShopOrderRepository extends JpaRepository<ShopOrder, Long> {
    List<ShopOrder> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
    List<ShopOrder> findByRelatedJobId(Long jobId);
    List<ShopOrder> findAllByOrderByCreatedAtDesc();
    Optional<ShopOrder> findByOrderCodeIgnoreCase(String orderCode);
    List<ShopOrder> findByRecipientPhoneOrderByCreatedAtDesc(String recipientPhone);
    long countByCustomerId(Long customerId);
    long countByRecipientPhone(String recipientPhone);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM ShopOrder o WHERE o.status <> 'CANCELLED'")
    BigDecimal sumTotalShopRevenue();
}
