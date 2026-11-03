package vn.vieshift.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.vieshift.entity.MinigameSpinLog;
import vn.vieshift.entity.User;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

@Repository
public interface MinigameSpinLogRepository extends JpaRepository<MinigameSpinLog, Long> {
    List<MinigameSpinLog> findByUserOrderByCreatedAtDesc(User user);
    List<MinigameSpinLog> findByPhoneOrderByCreatedAtDesc(String phone);
    long countByUser(User user);
    long countByPhone(String phone);
    List<MinigameSpinLog> findTop15ByOrderByCreatedAtDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM MinigameSpinLog s WHERE s.id = :id")
    Optional<MinigameSpinLog> findByIdForUpdate(@Param("id") Long id);
}
