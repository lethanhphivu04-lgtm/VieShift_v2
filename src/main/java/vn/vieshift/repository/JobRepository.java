package vn.vieshift.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.vieshift.entity.Job;
import vn.vieshift.entity.JobStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT j FROM Job j WHERE j.id = :id")
    Optional<Job> findByIdForUpdate(@Param("id") Long id);

    List<Job> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    List<Job> findByViekerIdOrderByCreatedAtDesc(Long viekerId);

    List<Job> findByStatusOrderByCreatedAtDesc(JobStatus status);

    List<Job> findByStatusInOrderByCreatedAtDesc(List<JobStatus> statuses);

    @Query("SELECT j FROM Job j WHERE j.status = :status AND (:categoryId IS NULL OR j.category.id = :categoryId) ORDER BY j.createdAt DESC")
    List<Job> findAvailableJobs(@Param("status") JobStatus status, @Param("categoryId") Long categoryId);

    long countByStatus(JobStatus status);

    long countByCategoryId(Long categoryId);

    long countByCustomerId(Long customerId);

    long countByViekerId(Long viekerId);

    @Query("SELECT COALESCE(SUM(j.platformFee), 0) FROM Job j WHERE j.status = vn.vieshift.entity.JobStatus.COMPLETED")
    BigDecimal sumTotalPlatformFeeCollected();

    @Query("SELECT COALESCE(SUM(j.price), 0) FROM Job j WHERE j.status = vn.vieshift.entity.JobStatus.COMPLETED")
    BigDecimal sumTotalCompletedVolume();
}
