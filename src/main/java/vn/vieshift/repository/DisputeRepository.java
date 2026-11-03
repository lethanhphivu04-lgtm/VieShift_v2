package vn.vieshift.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.vieshift.entity.Dispute;
import vn.vieshift.entity.DisputeStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface DisputeRepository extends JpaRepository<Dispute, Long> {
    Optional<Dispute> findByJobId(Long jobId);
    List<Dispute> findByStatusOrderByCreatedAtDesc(DisputeStatus status);
    long countByStatus(DisputeStatus status);
}
