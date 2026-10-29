package vn.vieshift.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.vieshift.entity.VerificationStatus;
import vn.vieshift.entity.ViekerProfile;

import java.util.List;
import java.util.Optional;

@Repository
public interface ViekerProfileRepository extends JpaRepository<ViekerProfile, Long> {
    Optional<ViekerProfile> findByUserId(Long userId);
    Optional<ViekerProfile> findByOnboardingToken(String onboardingToken);
    List<ViekerProfile> findByVerificationStatusOrderByCreatedAtDesc(VerificationStatus status);
    List<ViekerProfile> findByPendingCategoryRequestIsNotNullOrderByPendingCategoryRequestedAtDesc();
    long countByVerificationStatus(VerificationStatus status);
}
