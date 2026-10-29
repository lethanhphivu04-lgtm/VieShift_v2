package vn.vieshift.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.vieshift.entity.Voucher;

import java.util.List;
import java.util.Optional;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Long> {

    Optional<Voucher> findByCodeIgnoreCase(String code);

    List<Voucher> findByActiveTrueAndIsPublicTrueOrderByCreatedAtDesc();

    List<Voucher> findAllByOrderByCreatedAtDesc();

    boolean existsByCodeIgnoreCase(String code);
}
