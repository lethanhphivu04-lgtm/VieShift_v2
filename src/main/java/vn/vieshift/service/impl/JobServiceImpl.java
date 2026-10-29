package vn.vieshift.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.vieshift.dto.CreateJobRequest;
import vn.vieshift.entity.*;
import vn.vieshift.exception.InvalidJobStateException;
import vn.vieshift.exception.ProfileNotQualifiedException;
import vn.vieshift.exception.ResourceNotFoundException;
import vn.vieshift.exception.UnauthorizedActionException;
import vn.vieshift.repository.JobCategoryRepository;
import vn.vieshift.repository.JobRepository;
import vn.vieshift.repository.UserRepository;
import vn.vieshift.repository.ViekerProfileRepository;
import vn.vieshift.service.JobService;
import vn.vieshift.service.WalletService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class JobServiceImpl implements JobService {

    private final JobRepository jobRepository;
    private final JobCategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ViekerProfileRepository profileRepository;
    private final WalletService walletService;
    private final vn.vieshift.service.NotificationService notificationService;
    private final vn.vieshift.service.VoucherService voucherService;

    @Value("${vieshift.platform-fee-percent:10}")
    private int platformFeePercent;

    public JobServiceImpl(JobRepository jobRepository,
            JobCategoryRepository categoryRepository,
            UserRepository userRepository,
            ViekerProfileRepository profileRepository,
            WalletService walletService,
            vn.vieshift.service.NotificationService notificationService,
            vn.vieshift.service.VoucherService voucherService) {
        this.jobRepository = jobRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.walletService = walletService;
        this.notificationService = notificationService;
        this.voucherService = voucherService;
    }

    @Override
    public Job createJob(Long customerId, CreateJobRequest request) {
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng ID: " + customerId));

        if (customer.getRole() != Role.CUSTOMER) {
            throw new UnauthorizedActionException("Chỉ tài khoản Khách hàng mới có quyền đăng yêu cầu dịch vụ");
        }

        JobCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy danh mục dịch vụ ID: " + request.getCategoryId()));

        // 1. Tính toán giá dịch vụ gốc dựa trên danh mục & thời lượng/quãng đường
        BigDecimal originalPrice = calculateEstimatedPrice(category.getId(), request.getDurationHours(),
                request.getDistanceKm());

        // 2. Kiểm tra và áp dụng Voucher ưu đãi (nếu có)
        BigDecimal discountAmount = BigDecimal.ZERO;
        String appliedVoucherCode = null;
        if (request.getVoucherCode() != null && !request.getVoucherCode().isBlank()) {
            Optional<Voucher> voucherOpt = voucherService.validateVoucher(request.getVoucherCode().trim(),
                    originalPrice);
            if (voucherOpt.isPresent()) {
                Voucher voucher = voucherOpt.get();
                discountAmount = voucher.calculateDiscount(originalPrice);
                appliedVoucherCode = voucher.getCode();
                voucherService.recordVoucherUsage(appliedVoucherCode);
            }
        }

        BigDecimal finalPrice = originalPrice.subtract(discountAmount);
        if (finalPrice.compareTo(BigDecimal.ZERO) < 0) {
            finalPrice = BigDecimal.ZERO;
        }

        // 3. Tính phí nền tảng & tiền thực nhận của Đối tác VieKer (bảo vệ quyền lợi
        // đối tác dựa trên giá gốc)
        BigDecimal feeRate = BigDecimal.valueOf(platformFeePercent).divide(BigDecimal.valueOf(100), 2,
                RoundingMode.HALF_UP);
        BigDecimal platformFee = originalPrice.multiply(feeRate).setScale(0, RoundingMode.HALF_UP);
        BigDecimal netAmount = originalPrice.subtract(platformFee);

        // 4. Khởi tạo đối tượng Job
        Job job = new Job(
                customer,
                category,
                request.getTitle().trim(),
                request.getDescription().trim(),
                request.getAddress().trim(),
                request.getScheduledAt(),
                request.getDurationHours(),
                request.getDistanceKm(),
                finalPrice,
                platformFee,
                netAmount);
        job.setOriginalPrice(originalPrice);
        job.setDiscountAmount(discountAmount);
        job.setVoucherCode(appliedVoucherCode);
        job.setNotes(request.getNotes());
        Job savedJob = jobRepository.save(job);

        // 5. Ký quỹ Escrow: Tự động khóa số tiền thực thanh toán trong ví của khách
        // hàng
        walletService.holdEscrow(customerId, savedJob, finalPrice);

        // 6. Gửi thông báo đến các Đối tác VieKer trong hệ thống
        try {
            notificationService.notifyWorkersOfNewJob(
                    "Việc mới trên sàn: " + savedJob.getTitle(),
                    "Khách hàng cần đặt: " + savedJob.getTitle() + " tại " + savedJob.getAddress(),
                    "/vieker?tab=marketplace",
                    savedJob.getCategory().getId(),
                    savedJob.getAddress());
        } catch (Exception ignored) {
        }

        return savedJob;
    }

    @Override
    @Transactional(readOnly = true)
    public Job getJobById(Long jobId) {
        return jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy công việc ID: " + jobId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Job> getJobsByCustomer(Long customerId) {
        return jobRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Job> getJobsByVieker(Long viekerId) {
        return jobRepository.findByViekerIdOrderByCreatedAtDesc(viekerId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Job> getAvailableJobsForMarketplace(Long categoryId) {
        return jobRepository.findAvailableJobs(JobStatus.OPEN, categoryId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Job> getAllJobs() {
        return jobRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Job> getJobsByStatus(JobStatus status) {
        return jobRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    @Override
    public Job acceptJob(Long viekerId, Long jobId) {
        User vieker = userRepository.findById(viekerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thợ ID: " + viekerId));

        if (vieker.getRole() != Role.VIEKER) {
            throw new UnauthorizedActionException("Chỉ tài khoản Thợ/Vieker mới được nhận việc");
        }

        ViekerProfile profile = profileRepository.findByUserId(viekerId)
                .orElseThrow(() -> new ProfileNotQualifiedException("Bạn chưa hoàn thiện hồ sơ định danh thợ"));

        if (!profile.isApproved()) {
            throw new ProfileNotQualifiedException("Hồ sơ định danh của bạn đang chờ phê duyệt hoặc chưa đạt yêu cầu");
        }

        Job job = jobRepository.findByIdForUpdate(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy công việc ID: " + jobId));
        if (job.getStatus() != JobStatus.OPEN) {
            throw new InvalidJobStateException("Công việc này không còn ở trạng thái mở nhận việc");
        }

        if (job.getCustomer().getId().equals(viekerId)) {
            throw new UnauthorizedActionException("Bạn không thể tự nhận công việc do chính mình đăng");
        }

        // Kiểm tra phạm vi dịch vụ / chuyên môn của Đối tác VieKer
        String jobCategoryName = job.getCategory() != null ? job.getCategory().getName() : "";
        if (!profile.canPerformCategory(jobCategoryName)) {
            String approvedCats = profile.getAllApprovedCategories().isEmpty() ? "Chưa có"
                    : String.join(", ", profile.getAllApprovedCategories());
            throw new ProfileNotQualifiedException("Công việc này thuộc chuyên môn '" + jobCategoryName
                    + "'. Bạn chỉ được nhận việc thuộc các chuyên môn đã đăng ký và được duyệt: [" + approvedCats
                    + "]. Vui lòng gửi yêu cầu mở rộng chuyên môn trong hồ sơ để nhận việc!");
        }

        job.setVieker(vieker);
        job.setStatus(JobStatus.ACCEPTED);
        Job saved = jobRepository.save(job);

        try {
            notificationService.notifyUser(
                    job.getCustomer(),
                    "Thợ đã nhận việc: " + job.getTitle(),
                    "Đối tác " + vieker.getFullName() + " đã nhận công việc #" + job.getId() + " của bạn.",
                    "/customer/jobs/" + job.getId(),
                    NotificationType.JOB_ACCEPTED);
        } catch (Exception ignored) {
        }

        return saved;
    }

    @Override
    public Job startJob(Long viekerId, Long jobId) {
        Job job = jobRepository.findByIdForUpdate(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy công việc ID: " + jobId));
        if (job.getVieker() == null || !job.getVieker().getId().equals(viekerId)) {
            throw new UnauthorizedActionException("Bạn không phải thợ được phân công cho việc này");
        }
        if (job.getStatus() != JobStatus.ACCEPTED) {
            throw new InvalidJobStateException("Công việc cần ở trạng thái Đã nhận trước khi bấm Bắt đầu làm");
        }
        job.setStatus(JobStatus.IN_PROGRESS);
        return jobRepository.save(job);
    }

    @Override
    public Job reportCompleteJob(Long viekerId, Long jobId) {
        Job job = jobRepository.findByIdForUpdate(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy công việc ID: " + jobId));
        if (job.getVieker() == null || !job.getVieker().getId().equals(viekerId)) {
            throw new UnauthorizedActionException("Bạn không phải thợ được phân công cho việc này");
        }
        if (job.getStatus() != JobStatus.IN_PROGRESS) {
            throw new InvalidJobStateException("Chỉ công việc đang thực hiện mới có thể báo cáo hoàn thành");
        }
        job.setStatus(JobStatus.PENDING_REVIEW);
        Job saved = jobRepository.save(job);

        try {
            notificationService.notifyUser(
                    job.getCustomer(),
                    "Thợ đã báo hoàn thành công việc",
                    "Đối tác " + (job.getVieker() != null ? job.getVieker().getFullName() : "Thợ")
                            + " đã báo cáo hoàn tất việc #" + job.getId() + ". Vui lòng nghiệm thu.",
                    "/customer/jobs/" + job.getId(),
                    NotificationType.JOB_COMPLETED);
        } catch (Exception ignored) {
        }

        return saved;
    }

    @Override
    public Job approveAndPayJob(Long customerId, Long jobId) {
        Job job = jobRepository.findByIdForUpdate(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy công việc ID: " + jobId));
        if (!job.getCustomer().getId().equals(customerId)) {
            throw new UnauthorizedActionException("Chỉ khách hàng tạo việc mới có quyền nghiệm thu và thanh toán");
        }
        if (job.getStatus() != JobStatus.PENDING_REVIEW && job.getStatus() != JobStatus.IN_PROGRESS) {
            throw new InvalidJobStateException("Công việc cần có thợ báo cáo hoàn thành trước khi nghiệm thu");
        }

        // 1. Cập nhật trạng thái công việc
        job.setStatus(JobStatus.COMPLETED);
        job.setCompletedAt(LocalDateTime.now());
        Job completedJob = jobRepository.save(job);

        // 2. Kích hoạt giải ngân tiền Escrow sang ví Thợ
        walletService.releaseEscrowToVieker(completedJob, completedJob.getNetAmount(), completedJob.getPlatformFee());

        // 3. Tăng số việc đã làm cho Thợ
        if (job.getVieker() != null) {
            profileRepository.findByUserId(job.getVieker().getId()).ifPresent(p -> {
                p.setTotalJobsCompleted(p.getTotalJobsCompleted() + 1);
                profileRepository.save(p);
            });

            try {
                notificationService.notifyUser(
                        job.getVieker(),
                        "Khách hàng đã nghiệm thu công việc #" + job.getId(),
                        "Khách hàng " + job.getCustomer().getFullName() + " đã nghiệm thu công việc '" + job.getTitle()
                                + "'. Tiền công đã được cộng vào ví của bạn.",
                        "/vieker?tab=wallet",
                        NotificationType.JOB_APPROVED);
            } catch (Exception ignored) {
            }
        }

        return completedJob;
    }

    @Override
    public Job cancelJob(Long customerId, Long jobId) {
        Job job = jobRepository.findByIdForUpdate(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy công việc ID: " + jobId));
        if (!job.getCustomer().getId().equals(customerId)) {
            throw new UnauthorizedActionException("Chỉ khách hàng tạo việc mới có quyền hủy công việc");
        }
        if (job.getStatus() != JobStatus.OPEN && job.getStatus() != JobStatus.ACCEPTED) {
            throw new InvalidJobStateException("Không thể hủy công việc đã bắt đầu thực hiện hoặc đã hoàn thành");
        }

        job.setStatus(JobStatus.CANCELLED);
        Job cancelledJob = jobRepository.save(job);

        // Hoàn 100% tiền ký quỹ Escrow về số dư khả dụng của khách hàng
        walletService.refundEscrowToCustomer(cancelledJob, cancelledJob.getPrice());

        if (job.getVieker() != null) {
            try {
                notificationService.notifyUser(
                        job.getVieker(),
                        "Công việc đã bị hủy",
                        "Công việc #" + job.getId() + " (" + job.getTitle() + ") đã bị hủy bởi khách hàng.",
                        "/vieker?tab=my-jobs",
                        NotificationType.JOB_CANCELLED);
            } catch (Exception ignored) {
            }
        }

        return cancelledJob;
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateEstimatedPrice(Long categoryId, BigDecimal hours, BigDecimal distanceKm) {
        JobCategory category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục ID: " + categoryId));

        if (category.getPricingMode() == PricingMode.DISTANCE) {
            BigDecimal km = distanceKm != null ? distanceKm : BigDecimal.valueOf(3.0);
            return category.getBaseRate().multiply(km).setScale(0, RoundingMode.HALF_UP);
        } else {
            BigDecimal h = hours != null ? hours : BigDecimal.valueOf(2.0);
            return category.getBaseRate().multiply(h).setScale(0, RoundingMode.HALF_UP);
        }
    }
}
