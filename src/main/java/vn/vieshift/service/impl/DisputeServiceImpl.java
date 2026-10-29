package vn.vieshift.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import vn.vieshift.dto.DisputeRequest;
import vn.vieshift.dto.ResolveDisputeRequest;
import vn.vieshift.entity.*;
import vn.vieshift.exception.InvalidJobStateException;
import vn.vieshift.exception.ResourceNotFoundException;
import vn.vieshift.exception.UnauthorizedActionException;
import vn.vieshift.repository.DisputeRepository;
import vn.vieshift.repository.JobRepository;
import vn.vieshift.repository.UserRepository;
import vn.vieshift.service.DisputeService;
import vn.vieshift.service.WalletService;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class DisputeServiceImpl implements DisputeService {

    private static final Logger log = LoggerFactory.getLogger(DisputeServiceImpl.class);
    private final Path disputeUploadRoot = Paths.get("uploads/disputes").toAbsolutePath().normalize();

    private final DisputeRepository disputeRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final WalletService walletService;
    private final vn.vieshift.service.NotificationService notificationService;

    public DisputeServiceImpl(DisputeRepository disputeRepository,
                              JobRepository jobRepository,
                              UserRepository userRepository,
                              WalletService walletService,
                              vn.vieshift.service.NotificationService notificationService) {
        this.disputeRepository = disputeRepository;
        this.jobRepository = jobRepository;
        this.userRepository = userRepository;
        this.walletService = walletService;
        this.notificationService = notificationService;
    }

    @Override
    public Dispute openDispute(Long userId, DisputeRequest request) {
        return openDispute(userId, request, null);
    }

    @Override
    public Dispute openDispute(Long userId, DisputeRequest request, MultipartFile attachment) {
        Job job = jobRepository.findById(request.getJobId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy công việc ID: " + request.getJobId()));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng ID: " + userId));

        boolean isCustomer = job.getCustomer().getId().equals(userId);
        boolean isVieker = job.getVieker() != null && job.getVieker().getId().equals(userId);

        if (!isCustomer && !isVieker) {
            throw new UnauthorizedActionException("Chỉ khách hàng hoặc thợ trong công việc mới có quyền gửi khiếu nại");
        }

        if (job.getStatus() == JobStatus.COMPLETED || job.getStatus() == JobStatus.CANCELLED || job.getStatus() == JobStatus.RESOLVED) {
            throw new InvalidJobStateException("Công việc ở trạng thái này không thể mở khiếu nại");
        }

        if (disputeRepository.findByJobId(job.getId()).isPresent()) {
            throw new IllegalStateException("Công việc này đã có hồ sơ khiếu nại đang xử lý");
        }

        // Đóng băng trạng thái công việc
        job.setStatus(JobStatus.DISPUTED);
        jobRepository.save(job);

        Dispute dispute = new Dispute(
                job,
                user,
                request.getReason().trim(),
                request.getEvidenceText().trim()
        );

        // Lưu tệp bằng chứng đính kèm nếu có
        if (attachment != null && !attachment.isEmpty()) {
            try {
                if (!Files.exists(disputeUploadRoot)) {
                    Files.createDirectories(disputeUploadRoot);
                }
                String originalFilename = StringUtils.cleanPath(attachment.getOriginalFilename() != null ? attachment.getOriginalFilename() : "evidence");
                String storedFilename = "dispute_" + job.getId() + "_" + UUID.randomUUID().toString().substring(0, 8) + "_" + originalFilename;
                Path targetLocation = disputeUploadRoot.resolve(storedFilename).normalize();

                Files.copy(attachment.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
                dispute.setAttachmentPath(storedFilename);
                dispute.setAttachmentOriginalName(originalFilename);
                log.info("Đã lưu tệp bằng chứng khiếu nại đơn #{}: {}", job.getId(), storedFilename);
            } catch (IOException e) {
                log.error("Lỗi lưu file bằng chứng khiếu nại", e);
            }
        }

        Dispute savedDispute = disputeRepository.save(dispute);

        try {
            // Thông báo cho bên còn lại
            User otherUser = user.getId().equals(job.getCustomer().getId()) ? job.getVieker() : job.getCustomer();
            if (otherUser != null) {
                String link = otherUser.getId().equals(job.getCustomer().getId()) ? "/customer/jobs/" + job.getId() : "/vieker?tab=my-jobs";
                notificationService.notifyUser(
                        otherUser,
                        "Có yêu cầu khiếu nại công việc #" + job.getId(),
                        user.getFullName() + " đã gửi yêu cầu khiếu nại với lý do: " + dispute.getReason() + ". Tiền công đang được tạm giữ an toàn.",
                        link,
                        NotificationType.DISPUTE_UPDATE
                );
            }
        } catch (Exception ignored) {}

        return savedDispute;
    }

    @Override
    @Transactional(readOnly = true)
    public Path resolveAttachment(Long disputeId) {
        Dispute dispute = getDisputeById(disputeId);
        if (dispute.getAttachmentPath() == null || dispute.getAttachmentPath().isBlank()) {
            return null;
        }
        Path path = disputeUploadRoot.resolve(dispute.getAttachmentPath()).normalize();
        if (!path.startsWith(disputeUploadRoot) || !Files.exists(path)) {
            return null;
        }
        return path;
    }

    @Override
    @Transactional(readOnly = true)
    public Dispute getDisputeById(Long disputeId) {
        return disputeRepository.findById(disputeId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khiếu nại ID: " + disputeId));
    }

    @Override
    @Transactional(readOnly = true)
    public Dispute getDisputeByJobId(Long jobId) {
        return disputeRepository.findByJobId(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Chưa có khiếu nại cho công việc ID: " + jobId));
    }

    @Override
    public Dispute resolveDispute(Long adminId, ResolveDisputeRequest request) {
        Dispute dispute = disputeRepository.findById(request.getDisputeId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khiếu nại ID: " + request.getDisputeId()));

        if (dispute.getStatus() == DisputeStatus.RESOLVED) {
            throw new InvalidJobStateException("Hồ sơ khiếu nại này đã được phân xử trước đó");
        }

        Job job = dispute.getJob();
        BigDecimal totalEscrow = job.getPrice();
        BigDecimal customerRefund;
        BigDecimal viekerPayout;

        String choice = request.getResolutionType();
        if ("FULL_PAYOUT_VIEKER".equalsIgnoreCase(choice)) {
            // Thợ thắng: Thợ nhận tiền netAmount, Khách nhận 0
            customerRefund = BigDecimal.ZERO;
            viekerPayout = job.getNetAmount();
        } else if ("SPLIT_EQUAL".equalsIgnoreCase(choice)) {
            // Hòa giải 50-50: Hoàn 50% tiền cọc cho khách, giải ngân 50% tiền công cho thợ
            customerRefund = totalEscrow.divide(BigDecimal.valueOf(2), 0, RoundingMode.HALF_UP);
            viekerPayout = job.getNetAmount().divide(BigDecimal.valueOf(2), 0, RoundingMode.HALF_UP);
        } else {
            // Mặc định FULL_REFUND_CUSTOMER: Khách thắng, hoàn 100% tiền cọc cho khách
            customerRefund = totalEscrow;
            viekerPayout = BigDecimal.ZERO;
        }

        // 1. Cập nhật Dispute
        dispute.setStatus(DisputeStatus.RESOLVED);
        dispute.setAdminResolutionNote(request.getAdminNote().trim());
        dispute.setCustomerRefundAmount(customerRefund);
        dispute.setViekerPayoutAmount(viekerPayout);
        dispute.setResolvedAt(LocalDateTime.now());
        Dispute savedDispute = disputeRepository.save(dispute);

        // 2. Cập nhật Job sang RESOLVED
        job.setStatus(JobStatus.RESOLVED);
        jobRepository.save(job);

        // 3. Phân chia giải ngân ví Escrow thực tế
        walletService.resolveEscrowSplit(job, customerRefund, viekerPayout);

        try {
            // Thông báo kết quả cho Khách hàng
            notificationService.notifyUser(
                    job.getCustomer(),
                    "Khiếu nại công việc #" + job.getId() + " đã được xử lý",
                    "Ban Quản Trị đã xử lý khiếu nại: " + request.getAdminNote() + ". Số tiền hoàn về ví của bạn: " + customerRefund + " đ.",
                    "/customer/jobs/" + job.getId(),
                    NotificationType.DISPUTE_UPDATE
            );
            // Thông báo kết quả cho Thợ
            if (job.getVieker() != null) {
                notificationService.notifyUser(
                        job.getVieker(),
                        "Khiếu nại công việc #" + job.getId() + " đã được xử lý",
                        "Ban Quản Trị đã xử lý khiếu nại: " + request.getAdminNote() + ". Số tiền chi trả vào ví của bạn: " + viekerPayout + " đ.",
                        "/vieker?tab=wallet",
                        NotificationType.DISPUTE_UPDATE
                );
            }
        } catch (Exception ignored) {}

        return savedDispute;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Dispute> getDisputesByStatus(DisputeStatus status) {
        return disputeRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Dispute> getAllDisputes() {
        return disputeRepository.findAll();
    }
}
