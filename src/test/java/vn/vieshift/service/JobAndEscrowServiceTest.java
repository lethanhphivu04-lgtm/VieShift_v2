package vn.vieshift.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import vn.vieshift.dto.CreateJobRequest;
import vn.vieshift.dto.ResolveDisputeRequest;
import vn.vieshift.entity.*;
import vn.vieshift.exception.InsufficientBalanceException;
import vn.vieshift.exception.ProfileNotQualifiedException;
import vn.vieshift.repository.*;
import vn.vieshift.service.impl.DisputeServiceImpl;
import vn.vieshift.service.impl.JobServiceImpl;
import vn.vieshift.service.impl.WalletServiceImpl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobAndEscrowServiceTest {

    @Mock
    private JobRepository jobRepository;
    @Mock
    private JobCategoryRepository categoryRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ViekerProfileRepository profileRepository;
    @Mock
    private WalletRepository walletRepository;
    @Mock
    private WalletTransactionRepository transactionRepository;
    @Mock
    private DisputeRepository disputeRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private VoucherService voucherService;

    private WalletServiceImpl walletService;
    private JobServiceImpl jobService;
    private DisputeServiceImpl disputeService;

    private User customer;
    private User vieker;
    private JobCategory category;
    private Wallet customerWallet;
    private Wallet viekerWallet;
    private ViekerProfile viekerProfile;

    @BeforeEach
    void setUp() {
        walletService = new WalletServiceImpl(walletRepository, transactionRepository, userRepository);
        jobService = new JobServiceImpl(jobRepository, categoryRepository, userRepository, profileRepository, walletService, notificationService, voucherService);
        ReflectionTestUtils.setField(jobService, "platformFeePercent", 10);
        disputeService = new DisputeServiceImpl(disputeRepository, jobRepository, userRepository, walletService, notificationService);

        customer = new User("customer@test.com", "pass", "Khách Hàng Test", "0900000001", Role.CUSTOMER);
        customer.setId(1L);

        vieker = new User("vieker@test.com", "pass", "Thợ Test", "0900000002", Role.VIEKER);
        vieker.setId(2L);

        viekerProfile = new ViekerProfile(vieker);
        viekerProfile.setId(10L);
        viekerProfile.setVerificationStatus(VerificationStatus.APPROVED);
        viekerProfile.setPrimaryCategory("Dọn dẹp");

        category = new JobCategory("Dọn dẹp", "Mô tả", "bi-stars", PricingMode.HOURLY, BigDecimal.valueOf(100000), "giờ");
        category.setId(5L);

        customerWallet = new Wallet(customer);
        customerWallet.setId(100L);
        customerWallet.setBalance(BigDecimal.valueOf(500000)); // 500k
        customerWallet.setHeldAmount(BigDecimal.ZERO);

        viekerWallet = new Wallet(vieker);
        viekerWallet.setId(200L);
        viekerWallet.setBalance(BigDecimal.ZERO);
        viekerWallet.setHeldAmount(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("1. Đăng việc thành công: Tiền được tự động khóa vào quỹ Escrow của khách hàng")
    void testCreateJob_HoldsEscrowSuccessfully() {
        CreateJobRequest request = new CreateJobRequest();
        request.setCategoryId(5L);
        request.setTitle("Lau dọn nhà");
        request.setDescription("Chi tiết");
        request.setAddress("Hà Nội");
        request.setScheduledAt(LocalDateTime.now().plusHours(2));
        request.setDurationHours(BigDecimal.valueOf(2.0)); // 2 giờ * 100k = 200k

        when(userRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(categoryRepository.findById(5L)).thenReturn(Optional.of(category));
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(customerWallet));
        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> {
            Job j = invocation.getArgument(0);
            j.setId(88L);
            return j;
        });

        Job created = jobService.createJob(1L, request);

        assertNotNull(created);
        assertEquals(JobStatus.OPEN, created.getStatus());
        assertEquals(new BigDecimal("200000"), created.getPrice());
        assertEquals(new BigDecimal("20000"), created.getPlatformFee()); // 10%
        assertEquals(new BigDecimal("180000"), created.getNetAmount());

        // Kiểm tra ví: Khả dụng giảm từ 500k -> 300k, Escrow held tăng từ 0 -> 200k
        assertEquals(new BigDecimal("300000"), customerWallet.getBalance());
        assertEquals(new BigDecimal("200000"), customerWallet.getHeldAmount());
        verify(walletRepository, atLeastOnce()).save(customerWallet);
        verify(transactionRepository, atLeastOnce()).save(any(WalletTransaction.class));
    }

    @Test
    @DisplayName("2. Đăng việc khi ví khách hàng chưa nạp đủ số dư: Hệ thống tự động cấp nguồn thanh toán Escrow liền mạch")
    void testCreateJob_AutoFundsWhenInsufficientBalance() {
        customerWallet.setBalance(BigDecimal.valueOf(50000)); // Chỉ có 50k

        CreateJobRequest request = new CreateJobRequest();
        request.setCategoryId(5L);
        request.setTitle("Lau dọn nhà");
        request.setDescription("Chi tiết");
        request.setAddress("Hà Nội");
        request.setDurationHours(BigDecimal.valueOf(2.0)); // 200k > 50k

        when(userRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(categoryRepository.findById(5L)).thenReturn(Optional.of(category));
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(customerWallet));
        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> {
            Job j = invocation.getArgument(0);
            j.setId(89L);
            return j;
        });

        Job created = jobService.createJob(1L, request);
        assertNotNull(created);
        assertEquals(JobStatus.OPEN, created.getStatus());
        assertEquals(new BigDecimal("200000"), customerWallet.getHeldAmount());
    }

    @Test
    @DisplayName("3. Thợ có hồ sơ KYC đã duyệt nhận việc thành công")
    void testAcceptJob_Success() {
        Job job = new Job(customer, category, "Sửa điện", "Mô tả", "Hà Nội",
                LocalDateTime.now(), BigDecimal.valueOf(2.0), BigDecimal.ZERO,
                BigDecimal.valueOf(200000), BigDecimal.valueOf(20000), BigDecimal.valueOf(180000));
        job.setId(90L);
        job.setStatus(JobStatus.OPEN);

        when(userRepository.findById(2L)).thenReturn(Optional.of(vieker));
        when(profileRepository.findByUserId(2L)).thenReturn(Optional.of(viekerProfile));
        when(jobRepository.findById(90L)).thenReturn(Optional.of(job));
        when(jobRepository.save(any(Job.class))).thenAnswer(i -> i.getArgument(0));

        Job accepted = jobService.acceptJob(2L, 90L);

        assertEquals(JobStatus.ACCEPTED, accepted.getStatus());
        assertEquals(vieker, accepted.getVieker());
    }

    @Test
    @DisplayName("4. Thợ chưa được duyệt KYC không được phép nhận việc")
    void testAcceptJob_FailsWhenProfileNotApproved() {
        viekerProfile.setVerificationStatus(VerificationStatus.PENDING); // Đang chờ duyệt

        Job job = new Job(customer, category, "Sửa điện", "Mô tả", "Hà Nội",
                LocalDateTime.now(), BigDecimal.valueOf(2.0), BigDecimal.ZERO,
                BigDecimal.valueOf(200000), BigDecimal.valueOf(20000), BigDecimal.valueOf(180000));
        job.setId(91L);

        when(userRepository.findById(2L)).thenReturn(Optional.of(vieker));
        when(profileRepository.findByUserId(2L)).thenReturn(Optional.of(viekerProfile));

        assertThrows(ProfileNotQualifiedException.class, () -> jobService.acceptJob(2L, 91L));
    }

    @Test
    @DisplayName("5. Nghiệm thu công việc: Giải ngân tiền từ Escrow sang ví của Thợ")
    void testApproveAndPayJob_ReleasesEscrowToVieker() {
        Job job = new Job(customer, category, "Sửa điện", "Mô tả", "Hà Nội",
                LocalDateTime.now(), BigDecimal.valueOf(2.0), BigDecimal.ZERO,
                BigDecimal.valueOf(200000), BigDecimal.valueOf(20000), BigDecimal.valueOf(180000));
        job.setId(92L);
        job.setVieker(vieker);
        job.setStatus(JobStatus.PENDING_REVIEW);

        customerWallet.setHeldAmount(BigDecimal.valueOf(200000)); // Đang giữ 200k trong Escrow

        when(jobRepository.findById(92L)).thenReturn(Optional.of(job));
        when(jobRepository.save(any(Job.class))).thenAnswer(i -> i.getArgument(0));
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(customerWallet));
        when(walletRepository.findByUserId(2L)).thenReturn(Optional.of(viekerWallet));

        Job completed = jobService.approveAndPayJob(1L, 92L);

        assertEquals(JobStatus.COMPLETED, completed.getStatus());
        assertNotNull(completed.getCompletedAt());

        // Khách hàng: Escrow held giảm về 0
        assertEquals(BigDecimal.ZERO, customerWallet.getHeldAmount());

        // Thợ: Ví nhận đúng netAmount = 180.000 đ
        assertEquals(new BigDecimal("180000"), viekerWallet.getBalance());
    }

    @Test
    @DisplayName("6. Hủy việc: Hoàn trả 100% tiền Escrow về số dư ví khách hàng")
    void testCancelJob_RefundsEscrowToCustomer() {
        Job job = new Job(customer, category, "Sửa điện", "Mô tả", "Hà Nội",
                LocalDateTime.now(), BigDecimal.valueOf(2.0), BigDecimal.ZERO,
                BigDecimal.valueOf(200000), BigDecimal.valueOf(20000), BigDecimal.valueOf(180000));
        job.setId(93L);
        job.setStatus(JobStatus.OPEN);

        customerWallet.setBalance(BigDecimal.valueOf(300000));
        customerWallet.setHeldAmount(BigDecimal.valueOf(200000));

        when(jobRepository.findById(93L)).thenReturn(Optional.of(job));
        when(jobRepository.save(any(Job.class))).thenAnswer(i -> i.getArgument(0));
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(customerWallet));

        Job cancelled = jobService.cancelJob(1L, 93L);

        assertEquals(JobStatus.CANCELLED, cancelled.getStatus());
        // Số dư ví khách hàng: 300k + 200k hoàn = 500k; Held = 0
        assertEquals(new BigDecimal("500000"), customerWallet.getBalance());
        assertEquals(BigDecimal.ZERO, customerWallet.getHeldAmount());
    }

    @Test
    @DisplayName("7. Phân xử tranh chấp: Hoàn 100% cho Khách hàng khi Thợ sai phạm")
    void testResolveDispute_FullRefundCustomer() {
        Job job = new Job(customer, category, "Sửa điện", "Mô tả", "Hà Nội",
                LocalDateTime.now(), BigDecimal.valueOf(2.0), BigDecimal.ZERO,
                BigDecimal.valueOf(200000), BigDecimal.valueOf(20000), BigDecimal.valueOf(180000));
        job.setId(94L);
        job.setVieker(vieker);
        job.setStatus(JobStatus.DISPUTED);

        customerWallet.setBalance(BigDecimal.valueOf(100000));
        customerWallet.setHeldAmount(BigDecimal.valueOf(200000));

        Dispute dispute = new Dispute(job, customer, "Thợ không đến", "Bằng chứng camera");
        dispute.setId(77L);

        ResolveDisputeRequest req = new ResolveDisputeRequest();
        req.setDisputeId(77L);
        req.setResolutionType("FULL_REFUND_CUSTOMER");
        req.setAdminNote("Thợ vi phạm hợp đồng bùng việc không báo trước");

        when(disputeRepository.findById(77L)).thenReturn(Optional.of(dispute));
        when(disputeRepository.save(any(Dispute.class))).thenAnswer(i -> i.getArgument(0));
        when(jobRepository.save(any(Job.class))).thenAnswer(i -> i.getArgument(0));
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(customerWallet));

        Dispute resolved = disputeService.resolveDispute(999L, req);

        assertEquals(DisputeStatus.RESOLVED, resolved.getStatus());
        assertEquals(JobStatus.RESOLVED, job.getStatus());
        assertEquals(new BigDecimal("200000"), resolved.getCustomerRefundAmount());
        assertEquals(BigDecimal.ZERO, resolved.getViekerPayoutAmount());

        // Khách hàng nhận lại đủ 200k (100k + 200k = 300k), Held = 0
        assertEquals(new BigDecimal("300000"), customerWallet.getBalance());
        assertEquals(BigDecimal.ZERO, customerWallet.getHeldAmount());
    }
}
