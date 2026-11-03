package vn.vieshift.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.vieshift.entity.JobStatus;
import vn.vieshift.entity.Role;
import vn.vieshift.entity.VerificationStatus;
import vn.vieshift.repository.*;
import vn.vieshift.service.DashboardService;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private final UserRepository userRepository;
    private final JobRepository jobRepository;
    private final WalletRepository walletRepository;
    private final ViekerProfileRepository profileRepository;
    private final DisputeRepository disputeRepository;

    public DashboardServiceImpl(UserRepository userRepository,
                                JobRepository jobRepository,
                                WalletRepository walletRepository,
                                ViekerProfileRepository profileRepository,
                                DisputeRepository disputeRepository) {
        this.userRepository = userRepository;
        this.jobRepository = jobRepository;
        this.walletRepository = walletRepository;
        this.profileRepository = profileRepository;
        this.disputeRepository = disputeRepository;
    }

    @Override
    public Map<String, Object> getAdminStatistics() {
        Map<String, Object> stats = new HashMap<>();

        long totalCustomers = userRepository.countByRole(Role.CUSTOMER);
        long totalViekers = userRepository.countByRole(Role.VIEKER);
        long pendingViekers = profileRepository.countByVerificationStatus(VerificationStatus.PENDING);

        long totalJobs = jobRepository.count();
        long openJobs = jobRepository.countByStatus(JobStatus.OPEN);
        long inProgressJobs = jobRepository.countByStatus(JobStatus.IN_PROGRESS);
        long completedJobs = jobRepository.countByStatus(JobStatus.COMPLETED);
        long disputedJobs = jobRepository.countByStatus(JobStatus.DISPUTED);

        BigDecimal totalEscrowHeld = walletRepository.sumTotalHeldAmount();
        BigDecimal totalPlatformFee = jobRepository.sumTotalPlatformFeeCollected();
        BigDecimal totalCompletedVolume = jobRepository.sumTotalCompletedVolume();

        stats.put("totalCustomers", totalCustomers);
        stats.put("totalViekers", totalViekers);
        stats.put("pendingViekers", pendingViekers);
        stats.put("totalJobs", totalJobs);
        stats.put("openJobs", openJobs);
        stats.put("inProgressJobs", inProgressJobs);
        stats.put("completedJobs", completedJobs);
        stats.put("disputedJobs", disputedJobs);
        stats.put("totalEscrowHeld", totalEscrowHeld != null ? totalEscrowHeld : BigDecimal.ZERO);
        stats.put("totalPlatformFee", totalPlatformFee != null ? totalPlatformFee : BigDecimal.ZERO);
        stats.put("totalCompletedVolume", totalCompletedVolume != null ? totalCompletedVolume : BigDecimal.ZERO);

        return stats;
    }
}
