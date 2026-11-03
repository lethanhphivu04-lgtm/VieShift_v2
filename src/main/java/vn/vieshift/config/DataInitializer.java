package vn.vieshift.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.vieshift.entity.*;
import vn.vieshift.repository.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ViekerProfileRepository profileRepository;
    private final JobCategoryRepository categoryRepository;
    private final WalletRepository walletRepository;
    private final JobRepository jobRepository;
    private final WalletTransactionRepository transactionRepository;
    private final ReviewRepository reviewRepository;
    private final PasswordEncoder passwordEncoder;
    private final vn.vieshift.repository.VoucherRepository voucherRepository;
    private final vn.vieshift.repository.ProductRepository productRepository;
    private final vn.vieshift.repository.ShopOrderRepository shopOrderRepository;
    private final vn.vieshift.repository.MinigameSliceRepository minigameSliceRepository;
    private final vn.vieshift.repository.MinigameGiftcodeRepository minigameGiftcodeRepository;
    private final vn.vieshift.repository.MinigameSpinLogRepository minigameSpinLogRepository;
    private final vn.vieshift.repository.ProductReviewRepository productReviewRepository;
    private final vn.vieshift.repository.StockReceiptRepository stockReceiptRepository;
    private final vn.vieshift.service.DataIntegrityService dataIntegrityService;
    private final vn.vieshift.service.StaffPermissionService staffPermissionService;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    public DataInitializer(UserRepository userRepository,
                           ViekerProfileRepository profileRepository,
                           JobCategoryRepository categoryRepository,
                           WalletRepository walletRepository,
                           JobRepository jobRepository,
                           WalletTransactionRepository transactionRepository,
                           ReviewRepository reviewRepository,
                           PasswordEncoder passwordEncoder,
                           vn.vieshift.repository.VoucherRepository voucherRepository,
                           vn.vieshift.repository.ProductRepository productRepository,
                           vn.vieshift.repository.ShopOrderRepository shopOrderRepository,
                           vn.vieshift.repository.MinigameSliceRepository minigameSliceRepository,
                           vn.vieshift.repository.MinigameGiftcodeRepository minigameGiftcodeRepository,
                           vn.vieshift.repository.MinigameSpinLogRepository minigameSpinLogRepository,
                           vn.vieshift.repository.ProductReviewRepository productReviewRepository,
                           vn.vieshift.repository.StockReceiptRepository stockReceiptRepository,
                           vn.vieshift.service.DataIntegrityService dataIntegrityService,
                           vn.vieshift.service.StaffPermissionService staffPermissionService,
                           org.springframework.jdbc.core.JdbcTemplate jdbcTemplate) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.categoryRepository = categoryRepository;
        this.walletRepository = walletRepository;
        this.jobRepository = jobRepository;
        this.transactionRepository = transactionRepository;
        this.reviewRepository = reviewRepository;
        this.passwordEncoder = passwordEncoder;
        this.voucherRepository = voucherRepository;
        this.productRepository = productRepository;
        this.shopOrderRepository = shopOrderRepository;
        this.minigameSliceRepository = minigameSliceRepository;
        this.minigameGiftcodeRepository = minigameGiftcodeRepository;
        this.minigameSpinLogRepository = minigameSpinLogRepository;
        this.productReviewRepository = productReviewRepository;
        this.stockReceiptRepository = stockReceiptRepository;
        this.dataIntegrityService = dataIntegrityService;
        this.staffPermissionService = staffPermissionService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void run(String... args) {
        try {
            jdbcTemplate.execute("ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check");
        } catch (Exception e) {
            // ignore if constraint doesn't exist
        }
        initCategories();
        initSampleVouchers();
        User admin = initAdmin();
        initSampleStaff();
        User customer = initCustomer();
        User approvedVieker = initApprovedVieker();
        initDefaultVieker();
        initAdditionalEliteTaskers();
        User pendingVieker = initPendingVieker();
        initSampleJobs(customer, approvedVieker);
        initShopProducts(customer);
        initMinigame(customer);
        ensureProductChecksums();
        cleanOldTransactionDescriptions();
        syncViekerCategoriesAndRadii();
        initTenYearHistory();
    }

    private void syncViekerCategoriesAndRadii() {
        profileRepository.findAll().forEach(p -> {
            boolean modified = false;
            if (p.getServiceRadiusKm() == null) {
                p.setServiceRadiusKm(10);
                modified = true;
            }
            if (p.getPrimaryCategory() == null || p.getPrimaryCategory().isBlank()) {
                if (p.getUser() != null && "tho.hoang@vieshift.vn".equals(p.getUser().getEmail())) {
                    p.setPrimaryCategory("Sửa chữa điện nước");
                    p.setAdditionalCategories("Lắp đặt & Treo đồ");
                } else if (p.getUser() != null && "tho.quan@vieshift.vn".equals(p.getUser().getEmail())) {
                    p.setPrimaryCategory("Lắp ráp nội thất");
                    p.setAdditionalCategories("Lắp đặt & Treo đồ");
                } else if (p.getUser() != null && "tho.ha@vieshift.vn".equals(p.getUser().getEmail())) {
                    p.setPrimaryCategory("Dọn dẹp nhà cửa");
                } else {
                    p.setPrimaryCategory("Lắp ráp nội thất");
                }
                modified = true;
            }
            if (modified) {
                profileRepository.save(p);
            }
        });
    }

    private void cleanOldTransactionDescriptions() {
        transactionRepository.findAll().forEach(tx -> {
            if (tx.getDescription() != null && tx.getDescription().contains("Escrow")) {
                tx.setDescription(tx.getDescription()
                        .replace("Ký quỹ bảo đảm (Escrow)", "Thanh toán bảo đảm")
                        .replace("Ký quỹ Escrow", "Bảo đảm")
                        .replace("Escrow", "Bảo đảm"));
                transactionRepository.save(tx);
            }
        });
    }

    private void initCategories() {
        createCategoryIfAbsent("Lắp ráp nội thất", "Lắp ráp đồ nội thất IKEA, bàn ghế, tủ kệ, giường ngủ tại nhà", "bi-tools", PricingMode.HOURLY, BigDecimal.valueOf(100000), "giờ");
        createCategoryIfAbsent("Lắp đặt & Treo đồ", "Khoan tường treo Tivi, kệ sách, tranh ảnh, rèm cửa, gương lớn", "bi-screwdriver", PricingMode.HOURLY, BigDecimal.valueOf(110000), "giờ");
        createCategoryIfAbsent("Vận chuyển & Chuyển đồ", "Hỗ trợ bốc xếp, khiêng đồ nặng và xe chở đồ đạc gia đình", "bi-truck", PricingMode.DISTANCE, BigDecimal.valueOf(15000), "km");
        createCategoryIfAbsent("Dọn dẹp nhà cửa", "Dọn dẹp phòng khách, bếp, lau dọn nhà cửa theo giờ", "bi-stars", PricingMode.HOURLY, BigDecimal.valueOf(80000), "giờ");
        createCategoryIfAbsent("Chăm sóc sân vườn", "Cắt tỉa cây cảnh, dọn cỏ, vệ sinh sân vườn và ban công", "bi-tree", PricingMode.HOURLY, BigDecimal.valueOf(95000), "giờ");
        createCategoryIfAbsent("Sửa chữa điện nước", "Khắc phục sự cố chập điện, rò rỉ ống nước, thay thiết bị", "bi-wrench-adjustable", PricingMode.HOURLY, BigDecimal.valueOf(120000), "giờ");
        createCategoryIfAbsent("Sơn sửa nhà cửa", "Sơn dặm tường, sơn phòng ngủ, chống thấm và làm mới không gian", "bi-paint-bucket", PricingMode.HOURLY, BigDecimal.valueOf(115000), "giờ");
        createCategoryIfAbsent("Giao hàng hỏa tốc", "Giao nhận bưu phẩm, đồ ăn, hàng hóa nội thành", "bi-box-seam", PricingMode.DISTANCE, BigDecimal.valueOf(10000), "km");
        createCategoryIfAbsent("Chăm sóc thú cưng", "Tắm rửa, dắt chó đi dạo, cho thú cưng ăn theo giờ", "bi-heart", PricingMode.HOURLY, BigDecimal.valueOf(90000), "giờ");
    }

    private void createCategoryIfAbsent(String name, String desc, String icon, PricingMode mode, BigDecimal rate, String unit) {
        if (!categoryRepository.existsByName(name)) {
            JobCategory cat = new JobCategory(name, desc, icon, mode, rate, unit);
            categoryRepository.save(cat);
        }
    }

    private User initAdmin() {
        String email = "admin@vieshift.vn";
        return userRepository.findByEmail(email).orElseGet(() -> {
            User admin = new User(email, passwordEncoder.encode("admin123"), "Quản trị viên Hệ thống", "0900000001", Role.ADMIN);
            User saved = userRepository.save(admin);
            Wallet w = new Wallet(saved);
            walletRepository.save(w);
            return saved;
        });
    }

    private void initSampleStaff() {
        String staffEmail = "nhanvien@vieshift.vn";
        if (userRepository.findByEmail(staffEmail).isEmpty()) {
            staffPermissionService.createStaff(
                    staffEmail,
                    "staff123",
                    "Trần Thị Thu Hà (Điều Phối Viên)",
                    "0908889900",
                    "Ban Vận Hành & CSKH Sàn",
                    true,  // canCreate
                    true,  // canEdit
                    false, // canDelete (BỊ GIỚI HẠN: Không được xóa)
                    true,  // canUsers
                    true,  // canServices
                    true,  // canVieker
                    true,  // canDispute
                    true,  // canShop
                    false, // canFinance (BỊ GIỚI HẠN: Không xem ví dòng tiền sàn)
                    true   // canMinigame
            );
        }
    }

    private User initDefaultVieker() {
        String email = "vieker@vieshift.vn";
        return userRepository.findByEmail(email).orElseGet(() -> {
            User vieker = new User(email, passwordEncoder.encode("vieker123"), "Đối tác VieKer 5 Sao", "0909000888", Role.VIEKER);
            User saved = userRepository.save(vieker);

            Wallet wallet = new Wallet(saved);
            wallet.setBalance(BigDecimal.valueOf(850000));
            walletRepository.save(wallet);

            ViekerProfile profile = new ViekerProfile(saved);
            profile.setIdCardNumber("079090009999");
            profile.setSkillsDescription("Chuyên gia đối tác VieKer đa năng: Lắp ráp nội thất IKEA, khoan treo và sửa chữa điện nước gia đình.");
            profile.setExperienceYears(4);
            profile.setOperatingZone("TP. Hồ Chí Minh");
            profile.setPrimaryCategory("Lắp ráp nội thất");
            profile.setVerificationStatus(VerificationStatus.APPROVED);
            profile.setRatingAvg(BigDecimal.valueOf(4.95));
            profile.setRatingCount(28);
            profile.setTotalJobsCompleted(35);
            profileRepository.save(profile);

            return saved;
        });
    }

    private User initCustomer() {
        String email = "khachhang@vieshift.vn";
        return userRepository.findByEmail(email).orElseGet(() -> {
            User cust = new User(email, passwordEncoder.encode("khach123"), "Nguyễn Văn Khách", "0901234567", Role.CUSTOMER);
            User saved = userRepository.save(cust);

            Wallet wallet = new Wallet(saved);
            wallet.setBalance(BigDecimal.valueOf(1500000)); // Nạp sẵn 1.500.000 đ
            walletRepository.save(wallet);

            WalletTransaction tx = new WalletTransaction(
                    wallet,
                    null,
                    BigDecimal.valueOf(1500000),
                    TransactionType.DEPOSIT,
                    wallet.getBalance(),
                    "Khởi tạo số dư ví kiểm thử mẫu"
            );
            transactionRepository.save(tx);

            return saved;
        });
    }

    private User initApprovedVieker() {
        String email = "tho.hoang@vieshift.vn";
        return userRepository.findByEmail(email).orElseGet(() -> {
            User vieker = new User(email, passwordEncoder.encode("tho123"), "Hoàng Văn Thợ", "0987654321", Role.VIEKER);
            User saved = userRepository.save(vieker);

            Wallet wallet = new Wallet(saved);
            wallet.setBalance(BigDecimal.valueOf(450000)); // Thu nhập sẵn 450.000 đ
            walletRepository.save(wallet);

            ViekerProfile profile = new ViekerProfile(saved);
            profile.setIdCardNumber("079090001234");
            profile.setSkillsDescription("Thợ tay nghề 5 năm chuyên sửa chữa điện nước gia đình, lắp máy bơm, xử lý chập điện an toàn.");
            profile.setExperienceYears(5);
            profile.setOperatingZone("Quận 1, Quận 3, Quận Bình Thạnh, TP.HCM");
            profile.setPrimaryCategory("Sửa chữa điện nước");
            profile.setAdditionalCategories("Lắp đặt & Treo đồ");
            profile.setServiceRadiusKm(10);
            profile.setVerificationStatus(VerificationStatus.APPROVED);
            profile.setRatingAvg(BigDecimal.valueOf(4.9));
            profile.setRatingCount(12);
            profile.setTotalJobsCompleted(18);
            profileRepository.save(profile);

            return saved;
        });
    }

    private void initAdditionalEliteTaskers() {
        createEliteTaskerIfAbsent(
                "tho.quan@vieshift.vn",
                "Lê Minh Quân",
                "0938112233",
                "079091112233",
                "Chuyên gia lắp ráp nội thất IKEA, kệ sách, bàn làm việc, giường tủ và khoan treo Tivi chuẩn xác, có đủ máy khoan Bosch chuyên dụng.",
                6,
                "Quận 1, Quận 2 (Thảo Điền), Quận 7, TP.HCM",
                BigDecimal.valueOf(5.0),
                34,
                42
        );
        createEliteTaskerIfAbsent(
                "tho.ha@vieshift.vn",
                "Phạm Thu Hà",
                "0977445566",
                "079094445566",
                "Chuyên viên Tổng vệ sinh căn hộ cao cấp, dọn dẹp theo giờ, vệ sinh bếp & khử khuẩn bằng dung dịch sinh học an toàn.",
                4,
                "Quận Bình Thạnh, Quận 1, Quận 3, Phú Nhuận, TP.HCM",
                BigDecimal.valueOf(4.95),
                27,
                31
        );
    }

    private void createEliteTaskerIfAbsent(String email, String fullName, String phone, String cccd,
                                           String skills, int expYears, String zone,
                                           BigDecimal rating, int ratingCount, int jobsCompleted) {
        if (userRepository.findByEmail(email).isEmpty()) {
            User vieker = new User(email, passwordEncoder.encode("tho123"), fullName, phone, Role.VIEKER);
            User saved = userRepository.save(vieker);

            Wallet wallet = new Wallet(saved);
            wallet.setBalance(BigDecimal.valueOf(680000));
            walletRepository.save(wallet);

            ViekerProfile profile = new ViekerProfile(saved);
            profile.setIdCardNumber(cccd);
            profile.setSkillsDescription(skills);
            profile.setExperienceYears(expYears);
            profile.setOperatingZone(zone);
            profile.setVerificationStatus(VerificationStatus.APPROVED);
            profile.setRatingAvg(rating);
            profile.setRatingCount(ratingCount);
            profile.setTotalJobsCompleted(jobsCompleted);
            profileRepository.save(profile);
        }
    }

    private User initPendingVieker() {
        String email = "tho.tuan@vieshift.vn";
        return userRepository.findByEmail(email).orElseGet(() -> {
            User vieker = new User(email, passwordEncoder.encode("tho123"), "Trần Anh Tuấn", "0912345678", Role.VIEKER);
            User saved = userRepository.save(vieker);

            Wallet wallet = new Wallet(saved);
            walletRepository.save(wallet);

            ViekerProfile profile = new ViekerProfile(saved);
            profile.setIdCardNumber("079095556789");
            profile.setSkillsDescription("Kinh nghiệm 2 năm giúp việc nhà, lau chùi kính, giặt ủi và nấu ăn gia đình.");
            profile.setExperienceYears(2);
            profile.setOperatingZone("Quận 7, Quận 4, TP.HCM");
            profile.setVerificationStatus(VerificationStatus.PENDING);
            profileRepository.save(profile);

            return saved;
        });
    }

    private void initSampleJobs(User customer, User approvedVieker) {
        if (jobRepository.count() > 0) {
            return;
        }

        JobCategory donNha = categoryRepository.findByName("Dọn dẹp nhà cửa").orElse(null);
        JobCategory dienNuoc = categoryRepository.findByName("Sửa chữa điện nước").orElse(null);
        JobCategory vanChuyen = categoryRepository.findByName("Vận chuyển & Chuyển đồ").orElse(null);

        Wallet customerWallet = walletRepository.findByUserId(customer.getId()).orElse(null);

        // 1. Công việc OPEN: Đang tìm thợ
        if (donNha != null && customerWallet != null) {
            BigDecimal price = BigDecimal.valueOf(240000); // 3 giờ * 80.000
            BigDecimal fee = BigDecimal.valueOf(24000);
            BigDecimal net = price.subtract(fee);

            Job job1 = new Job(
                    customer,
                    donNha,
                    "Tổng vệ sinh căn hộ 2 phòng ngủ Landmark",
                    "Cần dọn dẹp hút bụi phòng khách, 2 phòng ngủ, lau dọn khu bếp và cọ rửa nhà vệ sinh.",
                    "Vinhomes Central Park, P.22, Q. Bình Thạnh, TP.HCM",
                    LocalDateTime.now().plusHours(3),
                    BigDecimal.valueOf(3.0),
                    BigDecimal.valueOf(0),
                    price,
                    fee,
                    net
            );
            job1.setStatus(JobStatus.OPEN);
            Job savedJob1 = jobRepository.save(job1);

            // Escrow hold
            customerWallet.hold(price);
            walletRepository.save(customerWallet);
            transactionRepository.save(new WalletTransaction(
                    customerWallet,
                    savedJob1,
                    price,
                    TransactionType.ESCROW_HOLD,
                    customerWallet.getBalance(),
                    "Thanh toán bảo đảm cho việc #" + savedJob1.getId() + " [" + savedJob1.getTitle() + "]"
            ));
        }

        // 2. Công việc IN_PROGRESS: Thợ đang làm
        if (dienNuoc != null && customerWallet != null) {
            BigDecimal price = BigDecimal.valueOf(240000); // 2 giờ * 120.000
            BigDecimal fee = BigDecimal.valueOf(24000);
            BigDecimal net = price.subtract(fee);

            Job job2 = new Job(
                    customer,
                    dienNuoc,
                    "Sửa đường ống nước bồn rửa chén rò rỉ",
                    "Đường ống thoát nước bồn rửa chén bị rò rỉ nước xuống tủ bếp dưới, cần thợ kiểm tra thay ron hoặc đoạn ống.",
                    "45 Lê Văn Sỹ, P.13, Q.3, TP.HCM",
                    LocalDateTime.now().minusHours(1),
                    BigDecimal.valueOf(2.0),
                    BigDecimal.valueOf(0),
                    price,
                    fee,
                    net
            );
            job2.setVieker(approvedVieker);
            job2.setStatus(JobStatus.IN_PROGRESS);
            Job savedJob2 = jobRepository.save(job2);

            // Escrow hold
            customerWallet.hold(price);
            walletRepository.save(customerWallet);
            transactionRepository.save(new WalletTransaction(
                    customerWallet,
                    savedJob2,
                    price,
                    TransactionType.ESCROW_HOLD,
                    customerWallet.getBalance(),
                    "Thanh toán bảo đảm cho việc #" + savedJob2.getId() + " [" + savedJob2.getTitle() + "]"
            ));
        }

        // 3. Công việc COMPLETED: Đã hoàn thành và giải ngân
        if (vanChuyen != null) {
            BigDecimal price = BigDecimal.valueOf(150000); // 10 km * 15.000
            BigDecimal fee = BigDecimal.valueOf(15000);
            BigDecimal net = price.subtract(fee);

            Job job3 = new Job(
                    customer,
                    vanChuyen,
                    "Vận chuyển tủ lạnh 2 cánh sang nhà mới",
                    "Vận chuyển 1 tủ lạnh đứng cao 1m8 từ chung cư sang nhà phố, có thang máy cả 2 đầu.",
                    "Từ Nguyễn Thị Minh Khai Q1 sang Huỳnh Tấn Phát Q7",
                    LocalDateTime.now().minusDays(2),
                    BigDecimal.valueOf(1.0),
                    BigDecimal.valueOf(10.0),
                    price,
                    fee,
                    net
            );
            job3.setVieker(approvedVieker);
            job3.setStatus(JobStatus.COMPLETED);
            job3.setCompletedAt(LocalDateTime.now().minusDays(2).plusHours(2));
            Job savedJob3 = jobRepository.save(job3);

            // Tạo đánh giá mẫu 5 sao
            Review review = new Review(
                    savedJob3,
                    customer,
                    approvedVieker,
                    5,
                    "Thợ tới rất đúng giờ, khuân vác cẩn thận không một vết trầy xước. Nhiệt tình và lịch sự!"
            );
            reviewRepository.save(review);
        }
    }

    private void initSampleVouchers() {
        createVoucherIfAbsent(
                "VIESHIFT15",
                "Ưu đãi Mở màn: Giảm 15% tối đa 40.000 đ",
                "Áp dụng cho mọi dịch vụ theo giờ, đơn tối thiểu từ 100.000 đ",
                vn.vieshift.entity.DiscountType.PERCENTAGE,
                BigDecimal.valueOf(15),
                BigDecimal.valueOf(40000),
                BigDecimal.valueOf(100000),
                200,
                LocalDateTime.now().plusDays(60),
                true // CÔNG KHAI: Khách hàng thấy và bấm chọn 1-chạm
        );

        createVoucherIfAbsent(
                "GIAM50K",
                "Ưu đãi Trực tiếp: Giảm 50.000 đ tiền mặt",
                "Giảm ngay 50.000 đ trực tiếp vào chi phí dịch vụ cho đơn từ 200.000 đ",
                vn.vieshift.entity.DiscountType.FIXED_AMOUNT,
                BigDecimal.valueOf(50000),
                null,
                BigDecimal.valueOf(200000),
                100,
                LocalDateTime.now().plusDays(30),
                true // CÔNG KHAI: Khách hàng thấy và bấm chọn 1-chạm
        );

        createVoucherIfAbsent(
                "VIPSECRET100",
                "Mã Bí mật Tri ân VIP: Giảm 100.000 đ",
                "Ưu đãi nhập code bí mật từ CSKH / chiến dịch quảng cáo",
                vn.vieshift.entity.DiscountType.FIXED_AMOUNT,
                BigDecimal.valueOf(100000),
                null,
                BigDecimal.valueOf(200000),
                50,
                LocalDateTime.now().plusDays(90),
                false // ẨN: Khách hàng phải tự nhập code mới áp dụng được
        );

        createVoucherIfAbsent(
                "CHUYENGIA25",
                "Ưu đãi Nhập Code: Giảm 25% tối đa 80.000 đ",
                "Mã riêng dành cho chiến dịch quà tặng đặt dịch vụ chuyên sâu",
                vn.vieshift.entity.DiscountType.PERCENTAGE,
                BigDecimal.valueOf(25),
                BigDecimal.valueOf(80000),
                BigDecimal.valueOf(150000),
                30,
                LocalDateTime.now().plusDays(45),
                false // ẨN: Khách hàng phải tự nhập code
        );
    }

    private void createVoucherIfAbsent(String code, String title, String desc,
                                       vn.vieshift.entity.DiscountType type,
                                       BigDecimal val, BigDecimal maxDiscount,
                                       BigDecimal minOrder, Integer maxUses,
                                       LocalDateTime endDate, boolean isPublic) {
        if (!voucherRepository.existsByCodeIgnoreCase(code)) {
            vn.vieshift.entity.Voucher v = new vn.vieshift.entity.Voucher();
            v.setCode(code.trim().toUpperCase());
            v.setTitle(title);
            v.setDescription(desc);
            v.setDiscountType(type);
            v.setDiscountValue(val);
            v.setMaxDiscountAmount(maxDiscount);
            v.setMinOrderAmount(minOrder != null ? minOrder : BigDecimal.ZERO);
            v.setMaxUses(maxUses);
            v.setCurrentUses(0);
            v.setStartDate(LocalDateTime.now().minusDays(1));
            v.setEndDate(endDate);
            v.setPublic(isPublic);
            v.setActive(true);
            voucherRepository.save(v);
        }
    }

    private void initShopProducts(User customer) {
        if (productRepository.count() > 0) {
            return;
        }

        createProduct(
                "Bộ vòi nước & Dây cấp Inox 304 chống rò rỉ cao cấp",
                "Dụng cụ & Vật tư Điện Nước",
                "Vòi rửa chén/lavabo chuẩn Inox 304 chống gỉ sét kèm dây cấp chịu áp lực cao và băng tan chuyên dụng. Tương thích 99% đường nước gia đình.",
                BigDecimal.valueOf(245000),
                BigDecimal.valueOf(320000),
                45,
                "Bộ",
                "https://images.unsplash.com/photo-1584622650111-993a426fbf0a?auto=format&fit=crop&w=600&q=80",
                "Sửa chữa điện nước",
                68
        );

        createProduct(
                "Combo 2 Ổ cắm âm tường & Aptomat chống giật Panasonic",
                "Dụng cụ & Vật tư Điện Nước",
                "Bộ ổ cắm đôi 3 chấu mặt chống cháy kèm cầu dao tự động ngắt khi quá tải hoặc rò điện. Bảo hành chính hãng 24 tháng.",
                BigDecimal.valueOf(195000),
                BigDecimal.valueOf(250000),
                60,
                "Combo",
                "https://images.unsplash.com/photo-1558002038-1055907df827?auto=format&fit=crop&w=600&q=80",
                "Sửa chữa điện nước",
                54
        );

        createProduct(
                "Bộ 3 Bóng đèn LED Bulb Rạng Đông 12W Tiết kiệm điện",
                "Dụng cụ & Vật tư Điện Nước",
                "Ánh sáng trắng/trung tính bảo vệ thị lực, tiết kiệm 85% điện năng, tuổi thọ 25.000 giờ, đui xoáy E27 thông dụng.",
                BigDecimal.valueOf(135000),
                BigDecimal.valueOf(180000),
                80,
                "Hộp 3 bóng",
                "https://images.unsplash.com/photo-1550985616-10810253b84d?auto=format&fit=crop&w=600&q=80",
                "Sửa chữa điện nước",
                92
        );

        createProduct(
                "Giá treo Tivi đa năng Xoay Gật Gù (32 - 65 inch) Thép chịu lực",
                "Phụ kiện Khoan treo & Lắp đặt",
                "Khung thép sơn tĩnh điện dày chịu tải tới 45kg, kèm sẵn bộ tắc kê đạn & vít nở sắt chuyên dụng khoan tường gạch/bê tông.",
                BigDecimal.valueOf(180000),
                BigDecimal.valueOf(240000),
                35,
                "Bộ",
                "https://images.unsplash.com/photo-1593359677879-a4bb92f829d1?auto=format&fit=crop&w=600&q=80",
                "Lắp đặt & Treo đồ",
                83
        );

        createProduct(
                "Hộp 100 Vít nở nhựa, Tắc kê chịu lực & Bản lề giảm chấn Inox",
                "Phụ kiện Khoan treo & Lắp đặt",
                "Bộ phụ kiện tiêu chuẩn cho lắp ráp tủ kệ IKEA, thay bản lề cửa tủ bếp bị xệ và treo tranh ảnh, gương lớn.",
                BigDecimal.valueOf(95000),
                BigDecimal.valueOf(130000),
                100,
                "Hộp",
                "https://images.unsplash.com/photo-1572981779307-38b8cabb2407?auto=format&fit=crop&w=600&q=80",
                "Lắp ráp nội thất",
                115
        );

        createProduct(
                "Bộ Dụng cụ Gia đình Đa năng 42 món (Tua vít, Kìm, Búa, Thước)",
                "Phụ kiện Khoan treo & Lắp đặt",
                "Hộp dụng cụ thép hợp kim Chrome-Vanadium siêu bền dành cho mọi gia đình tự chăm sóc nhà cửa.",
                BigDecimal.valueOf(350000),
                BigDecimal.valueOf(450000),
                25,
                "Hộp",
                "https://images.unsplash.com/photo-1530124566582-a618bc2615dc?auto=format&fit=crop&w=600&q=80",
                "Lắp ráp nội thất",
                41
        );

        createProduct(
                "Combo Dung dịch Vệ sinh Sinh học & Tẩy cặn Canxi Kính/Inox",
                "Hóa phẩm & Vệ sinh chuyên dụng",
                "Gồm 1 chai tẩy cặn vách kính nhà tắm + 1 chai xịt khử khuẩn dầu mỡ nhà bếp + 4 khăn Microfiber không xơ bụi.",
                BigDecimal.valueOf(155000),
                BigDecimal.valueOf(210000),
                70,
                "Combo",
                "https://images.unsplash.com/photo-1585421514738-01798e348b17?auto=format&fit=crop&w=600&q=80",
                "Dọn dẹp nhà cửa",
                128
        );

        createProduct(
                "Set 10 Thùng Carton 5 lớp + Cuộn màng PE & Băng keo đóng gói",
                "Vật tư Chuyển nhà & Sơn sửa",
                "Thùng giấy sóng 5 lớp siêu cứng cáp chống móp méo kèm màng bọc chống trầy xước nội thất khi chuyển nhà.",
                BigDecimal.valueOf(165000),
                BigDecimal.valueOf(220000),
                50,
                "Set",
                "https://images.unsplash.com/photo-1607166452427-7e4477079cb9?auto=format&fit=crop&w=600&q=80",
                "Vận chuyển & Chuyển đồ",
                64
        );

        createProduct(
                "Bộ Keo trám tường Chống thấm & Lăn sơn dặm Dulux Trắng Sứ",
                "Vật tư Chuyển nhà & Sơn sửa",
                "Che phủ nhanh vết nứt chân chim, lỗ đinh khoan tường cũ và vết ố mốc chỉ sau 15 phút, không mùi độc hại.",
                BigDecimal.valueOf(125000),
                BigDecimal.valueOf(165000),
                55,
                "Bộ",
                "https://images.unsplash.com/photo-1562259949-e8e7689d7828?auto=format&fit=crop&w=600&q=80",
                "Sơn sửa nhà cửa",
                49
        );

        if (customer != null && shopOrderRepository.count() == 0) {
            ShopOrder sampleOrder = new ShopOrder();
            sampleOrder.setCustomer(customer);
            sampleOrder.setItemsSummary("1x Giá treo Tivi đa năng Xoay Gật Gù (180.000 đ), 1x Hộp 100 Vít nở nhựa & Bản lề (95.000 đ)");
            sampleOrder.setTotalAmount(BigDecimal.valueOf(275000));
            sampleOrder.setTotalItems(2);
            sampleOrder.setShippingAddress("Vinhomes Central Park, P.22, Q. Bình Thạnh, TP.HCM");
            sampleOrder.setRecipientPhone(customer.getPhone());
            sampleOrder.setOrderType("STANDALONE");
            sampleOrder.setStatus("COMPLETED");
            sampleOrder.setNotes("Giao giờ hành chính tại sảnh Landmark 2");
            shopOrderRepository.save(sampleOrder);
        }
    }

    private void createProduct(String name, String category, String desc,
                               BigDecimal price, BigDecimal origPrice,
                               int stock, String unit, String img,
                               String relatedServiceCat, int soldCount) {
        Product p = new Product(name, category, desc, price, origPrice, stock, unit, img, relatedServiceCat);
        p.setSoldCount(soldCount);
        p = productRepository.save(p);
        p.setChecksum(dataIntegrityService.computeProductChecksum(p));
        productRepository.save(p);
    }

    private void ensureProductChecksums() {
        List<Product> products = productRepository.findAll();
        for (Product p : products) {
            if (p.getChecksum() == null || p.getChecksum().isBlank()) {
                p.setChecksum(dataIntegrityService.computeProductChecksum(p));
                productRepository.save(p);
            }
        }
    }

    private void initMinigame(User customer) {
        if (minigameSliceRepository.count() == 0) {
            // 8 o quay may man theo dung he mau VieShift Boutique Elegance
            minigameSliceRepository.save(new vn.vieshift.entity.MinigameSlice(
                    "Voucher 20K", "COUPON", BigDecimal.valueOf(20000), "MINIGAME20K", 25, "#C24D63",
                    "Chúc mừng bạn nhận Voucher 20.000đ áp dụng cho mọi đơn đặt thợ & mua sắm vật tư!", 0));

            minigameSliceRepository.save(new vn.vieshift.entity.MinigameSlice(
                    "Giảm 50K phí thợ", "COUPON", BigDecimal.valueOf(50000), "THO50K", 15, "#2B2523",
                    "Chúc mừng bạn nhận ưu đãi 50.000đ khi đặt thợ sửa chữa điện nước & lắp đặt nội thất!", 1));

            minigameSliceRepository.save(new vn.vieshift.entity.MinigameSlice(
                    "Chúc may mắn", "MISS", BigDecimal.ZERO, null, 20, "#8A817C",
                    "Rất tiếc lượt quay này chưa trúng. Đừng nản lòng, hãy tích lũy thêm lượt quay và thử lại nhé!", 2));

            minigameSliceRepository.save(new vn.vieshift.entity.MinigameSlice(
                    "Hộp tắc kê nở", "GIFT", BigDecimal.valueOf(95000), "GIFT_BOX", 10, "#E6AF76",
                    "Chúc mừng bạn trúng Quà hiện vật: Hộp 100 Vít nở nhựa & Tắc kê chịu lực cao cấp!", 3));

            minigameSliceRepository.save(new vn.vieshift.entity.MinigameSlice(
                    "Voucher 30K", "COUPON", BigDecimal.valueOf(30000), "MINIGAME30K", 15, "#0D7A5F",
                    "Chúc mừng bạn nhận Voucher 30.000đ khấu trừ trực tiếp khi thanh toán dịch vụ!", 4));

            minigameSliceRepository.save(new vn.vieshift.entity.MinigameSlice(
                    "Quà bí mật", "GIFT", BigDecimal.valueOf(150000), "GIFT_SECRET", 5, "#AB3C51",
                    "Tuyệt vời! Bạn nhận được Hộp quà bí mật Dụng cụ đa năng từ Cộng đồng VieKer!", 5));

            minigameSliceRepository.save(new vn.vieshift.entity.MinigameSlice(
                    "Miễn phí công thợ", "COUPON", BigDecimal.valueOf(80000), "FREESURVEY", 10, "#4A252D",
                    "Chúc mừng bạn được Miễn phí 100% công thợ khảo sát & lắp đặt đồ đạc tận nhà!", 6));

            minigameSliceRepository.save(new vn.vieshift.entity.MinigameSlice(
                    "Voucher 10K", "COUPON", BigDecimal.valueOf(10000), "MINIGAME10K", 25, "#D95D74",
                    "Chúc mừng bạn trúng Voucher 10.000đ nạp ví may mắn!", 7));
        }

        if (minigameGiftcodeRepository.count() == 0) {
            minigameGiftcodeRepository.save(new vn.vieshift.entity.MinigameGiftcode("VIESHIFT2026", 3, 500));
            minigameGiftcodeRepository.save(new vn.vieshift.entity.MinigameGiftcode("MAYMAN88", 2, 500));
            minigameGiftcodeRepository.save(new vn.vieshift.entity.MinigameGiftcode("THOVIET", 2, 500));
            minigameGiftcodeRepository.save(new vn.vieshift.entity.MinigameGiftcode("REVIEWVIP", 1, 1000));
        } else if (minigameGiftcodeRepository.findByCodeIgnoreCase("REVIEWVIP").isEmpty()) {
            minigameGiftcodeRepository.save(new vn.vieshift.entity.MinigameGiftcode("REVIEWVIP", 1, 1000));
        }

        if (productReviewRepository.count() == 0) {
            List<vn.vieshift.entity.Product> allProds = productRepository.findAll();
            for (vn.vieshift.entity.Product prod : allProds) {
                productReviewRepository.save(new vn.vieshift.entity.ProductReview(
                        prod,
                        "Nguyễn Minh Tâm",
                        "0912345678",
                        5,
                        "Hàng chính hãng đóng gói rất kỹ, giao hỏa tốc chưa tới 1 tiếng rưỡi. Mình mua về tự lắp theo hướng dẫn cực kỳ dễ dàng!",
                        true
                ));
                productReviewRepository.save(new vn.vieshift.entity.ProductReview(
                        prod,
                        "Trần Hoàng Yến",
                        "0988776655",
                        5,
                        "Chất lượng vượt mong đợi so với giá tiền, có dán tem bảo hành 1 đổi 1 của VieShift Mall nên rất yên tâm.",
                        true
                ));
            }
        }

        // Mau lich su trung thuong ban dau
        if (minigameSpinLogRepository.count() == 0 && customer != null) {
            List<vn.vieshift.entity.MinigameSlice> slices = minigameSliceRepository.findByActiveTrueOrderBySliceIndexAsc();
            if (!slices.isEmpty()) {
                vn.vieshift.entity.MinigameSpinLog sampleLog1 = new vn.vieshift.entity.MinigameSpinLog(
                        customer, customer.getPhone(), slices.get(0), "COUPON", "Voucher 20.000đ"
                );
                sampleLog1 = minigameSpinLogRepository.save(sampleLog1);
                sampleLog1.setChecksum(dataIntegrityService.computeSpinLogChecksum(sampleLog1));
                minigameSpinLogRepository.save(sampleLog1);

                vn.vieshift.entity.MinigameSpinLog sampleLog2 = new vn.vieshift.entity.MinigameSpinLog(
                        null, "0912345678", slices.get(1), "COUPON", "Giảm 50K phí thợ"
                );
                sampleLog2 = minigameSpinLogRepository.save(sampleLog2);
                sampleLog2.setChecksum(dataIntegrityService.computeSpinLogChecksum(sampleLog2));
                minigameSpinLogRepository.save(sampleLog2);
            }
        }

        if (stockReceiptRepository.count() == 0) {
            List<vn.vieshift.entity.Product> allProds = productRepository.findAll();
            if (!allProds.isEmpty()) {
                vn.vieshift.entity.Product p1 = allProds.get(0);
                stockReceiptRepository.save(new vn.vieshift.entity.StockReceipt(
                        p1,
                        "Công ty Thiết bị Vệ sinh & Điện nước Inax Việt Nam",
                        40,
                        BigDecimal.valueOf(60000),
                        BigDecimal.valueOf(2400000),
                        "Nhập lô đầu tháng 11, đã thanh toán chuyển khoản 100%",
                        "Quản trị viên Hệ thống"
                ));
                if (allProds.size() > 1) {
                    vn.vieshift.entity.Product p2 = allProds.get(1);
                    stockReceiptRepository.save(new vn.vieshift.entity.StockReceipt(
                            p2,
                            "Nhà phân phối Phụ kiện Nội thất & Kim khí Hafele",
                            50,
                            BigDecimal.valueOf(55000),
                            BigDecimal.valueOf(1500000),
                            "Thanh toán đợt 1: 1.500.000đ, công nợ còn lại thanh toán sau 15 ngày",
                            "Quản trị viên Hệ thống"
                    ));
                }
            }
        }
    }

    private void initTenYearHistory() {
        if (userRepository.findByEmail("tho.laolang.2016@vieshift.vn").isPresent()) {
            return;
        }

        // 1. Tạo 5 khách hàng gắn bó qua các thời kỳ (2016 - 2024)
        User cust2016 = createHistoryCustomer("khach.vip.2016@vieshift.vn", "Bà Hoàng Yến Chi", "0903334455",
                "Căn hộ Penthouse, Vinhomes Central Park, Q. Bình Thạnh", LocalDateTime.of(2016, 4, 10, 9, 0), BigDecimal.valueOf(5000000));
        User cust2018 = createHistoryCustomer("khach.dn.2018@vieshift.vn", "Cty TNHH Sáng Tạo Alpha", "0283888999",
                "Tòa nhà Alpha Tower, 123 Pasteur, Q.3, TP.HCM", LocalDateTime.of(2018, 11, 20, 14, 30), BigDecimal.valueOf(12000000));
        User cust2020 = createHistoryCustomer("khach.thanh.2020@vieshift.vn", "Nguyễn Hải Đăng", "0904445566",
                "Khu Căn hộ Sunrise City, Q.7, TP.HCM", LocalDateTime.of(2020, 8, 15, 10, 15), BigDecimal.valueOf(2500000));
        User cust2022 = createHistoryCustomer("khach.lan.2022@vieshift.vn", "Trần Mai Lan", "0905556677",
                "Tháp T5, Masteri Thảo Điền, TP. Thủ Đức", LocalDateTime.of(2022, 5, 12, 16, 0), BigDecimal.valueOf(3200000));
        User cust2024 = createHistoryCustomer("khach.tuan.2024@vieshift.vn", "Lê Anh Tuấn", "0906667788",
                "Khu Đô Thị Sala, TP. Thủ Đức", LocalDateTime.of(2024, 2, 18, 11, 30), BigDecimal.valueOf(1800000));

        // 2. Tạo 10 thợ kỳ cựu tiêu biểu gắn bó theo từng mốc năm (2016 - 2025)
        User v2016 = createHistoryVieker("tho.laolang.2016@vieshift.vn", "Đỗ Văn Lão Làng", "0908111222",
                "Sửa chữa điện nước", "Lắp đặt & Treo đồ, Chống thấm dột", 10, 1850, BigDecimal.valueOf(5.0), 380,
                "Chuyên gia trưởng điện nước công trình & hộ gia đình. Gắn bó cùng VieShift từ ngày đầu 2016.",
                LocalDateTime.of(2016, 3, 15, 8, 0), BigDecimal.valueOf(15000000));

        User v2017 = createHistoryVieker("tho.dienlanh.2017@vieshift.vn", "Phạm Quốc Toàn", "0918222333",
                "Sửa chữa điện nước", "Vệ sinh máy lạnh, Lắp ráp nội thất", 9, 1420, BigDecimal.valueOf(4.98), 295,
                "Kỹ sư cơ điện lạnh, chuyên xử lý sự cố điều hòa, máy lạnh multi và hệ thống thông gió gia đình.",
                LocalDateTime.of(2017, 5, 20, 9, 30), BigDecimal.valueOf(11200000));

        User v2018 = createHistoryVieker("tho.khoantreo.2018@vieshift.vn", "Lê Thanh Tùng", "0938333444",
                "Lắp đặt & Treo đồ", "Sửa chữa điện nước, Gia cố tường", 8, 1150, BigDecimal.valueOf(4.95), 240,
                "Chuyên gia khoan tường không bụi, lắp đặt kệ tivi âm tường, gương kính lớn và tranh phong thủy.",
                LocalDateTime.of(2018, 8, 10, 10, 0), BigDecimal.valueOf(8900000));

        User v2019 = createHistoryVieker("tho.sonnha.2019@vieshift.vn", "Vũ Đình Trọng", "0948444555",
                "Sơn sửa nhà & Chống thấm", "Lắp đặt & Treo đồ, Sơn bả matit", 7, 980, BigDecimal.valueOf(4.92), 210,
                "Thợ sơn cao cấp, sơn hiệu ứng và chống thấm dột trần ban công, sơn tường nhà cũ mới.",
                LocalDateTime.of(2019, 2, 14, 8, 30), BigDecimal.valueOf(7400000));

        User v2020 = createHistoryVieker("tho.vesinh.2020@vieshift.vn", "Nguyễn Thị Mai Hoa", "0968555666",
                "Dọn dẹp nhà cửa", "Giặt sofa đệm rèm, Khử khuẩn cao cấp", 6, 1260, BigDecimal.valueOf(5.0), 310,
                "Trưởng nhóm dịch vụ vệ sinh công nghiệp VieShift, dọn nhà chuyên sâu, giặt thảm và đệm sofa.",
                LocalDateTime.of(2020, 6, 1, 13, 0), BigDecimal.valueOf(9800000));

        User v2021 = createHistoryVieker("tho.noithat.2021@vieshift.vn", "Bùi Thế Anh", "0978666777",
                "Lắp ráp nội thất", "Sửa khóa cửa gỗ, Bản lề giảm chấn", 5, 840, BigDecimal.valueOf(4.96), 180,
                "Thợ mộc lành nghề, nhận lắp đặt giường tủ gỗ công nghiệp, sofa bed, phụ kiện Hafele.",
                LocalDateTime.of(2021, 9, 9, 14, 0), BigDecimal.valueOf(6500000));

        User v2022 = createHistoryVieker("tho.thongtac.2022@vieshift.vn", "Trịnh Quang Khải", "0988777888",
                "Sửa chữa điện nước", "Thông tắc cống bồn cầu bằng máy lò xo", 4, 690, BigDecimal.valueOf(4.88), 155,
                "Xử lý nhanh chóng triệt để tắc nghẽn lavabo, bồn cầu, thoát sàn không đục phá nền nhà.",
                LocalDateTime.of(2022, 3, 25, 11, 0), BigDecimal.valueOf(5300000));

        User v2023 = createHistoryVieker("tho.khoadientu.2023@vieshift.vn", "Hồ Minh Quân", "0909888999",
                "Lắp đặt & Treo đồ", "Khóa thông minh vân tay, Camera Wifi", 3, 480, BigDecimal.valueOf(4.97), 115,
                "Lắp đặt và cài đặt khóa cửa thông minh Kaadas, Bosch, camera EZVIZ, hệ thống smarthome.",
                LocalDateTime.of(2023, 7, 18, 15, 30), BigDecimal.valueOf(4200000));

        User v2024 = createHistoryVieker("tho.nhomkinh.2024@vieshift.vn", "Đinh Văn Hưng", "0919999000",
                "Sửa chữa điện nước", "Cửa nhôm kính Xingfa, Vách tắm kính", 2, 310, BigDecimal.valueOf(4.91), 75,
                "Chuyên căn chỉnh bản lề sàn, thay kính cường lực, chống dột cửa sổ mùa mưa.",
                LocalDateTime.of(2024, 4, 12, 10, 0), BigDecimal.valueOf(3100000));

        User v2025 = createHistoryVieker("tho.giadung.2025@vieshift.vn", "Lý Gia Bảo", "0929111222",
                "Sửa chữa điện nước", "Sửa máy giặt máy sấy, Máy rửa chén", 2, 185, BigDecimal.valueOf(4.94), 48,
                "Kỹ thuật viên điện gia dụng các hãng Electrolux, LG, Panasonic, bảo dưỡng vệ sinh lồng giặt.",
                LocalDateTime.of(2025, 1, 8, 9, 0), BigDecimal.valueOf(2200000));

        // 3. Tạo các đơn dịch vụ lịch sử hoàn thành (2016 - 2025) kèm Đánh giá chân thực & Giao dịch ví
        JobCategory catDienNuoc = categoryRepository.findByName("Sửa chữa điện nước").orElse(null);
        JobCategory catDonNha = categoryRepository.findByName("Dọn dẹp nhà cửa").orElse(null);
        JobCategory catKhoanTreo = categoryRepository.findByName("Lắp đặt & Treo đồ").orElse(catDienNuoc);

        createCompletedJobWithReview(cust2016, v2016, catDienNuoc,
                "Cải tạo và thay toàn bộ hệ thống ống PPR nước nóng lạnh",
                "Hệ thống cấp nước âm tường bị rò rỉ âm thầm, cần thợ kinh nghiệm dò vết rò và hàn lại toàn bộ đường ống PPR chịu nhiệt.",
                "Penthouse Landmark, Bình Thạnh, TP.HCM",
                LocalDateTime.of(2016, 5, 20, 8, 30),
                BigDecimal.valueOf(1800000), BigDecimal.valueOf(180000), BigDecimal.valueOf(1620000),
                5, "Từ thời VieShift mới thành lập năm 2016 tôi đã dùng. Bác Lão Làng làm cẩn thận từng chi tiết, 10 năm nay nhà tôi hỏng hóc gì chỉ tin tưởng gọi thợ VieShift!");

        createCompletedJobWithReview(cust2018, v2017, catDienNuoc,
                "Bảo dưỡng định kỳ 6 cụm điều hòa âm trần văn phòng công ty",
                "Vệ sinh dàn nóng lạnh, nạp bổ sung gas R410A, kiểm tra đường thoát nước ngưng chống tràn vào trần thạch cao.",
                "Tòa nhà Alpha Tower, 123 Pasteur, Q.3, TP.HCM",
                LocalDateTime.of(2018, 12, 15, 13, 0),
                BigDecimal.valueOf(2400000), BigDecimal.valueOf(240000), BigDecimal.valueOf(2160000),
                5, "Bảo dưỡng 6 cụm máy lạnh công nghiệp văn phòng rất kỹ, đo gas chuẩn, dán tem bảo hành đàng hoàng. Hơn 8 năm nay công ty mình luôn đặt lịch thợ VieShift.");

        createCompletedJobWithReview(cust2020, v2018, catKhoanTreo,
                "Gia cố khoan treo hệ tủ sách lớn và tranh trang trí phòng khách",
                "Tường gạch nhẹ cần dùng tắc kê chuyên dụng Fischer để giữ trọng tải lớn an toàn.",
                "Sunrise City, Q.7, TP.HCM",
                LocalDateTime.of(2020, 9, 10, 10, 0),
                BigDecimal.valueOf(650000), BigDecimal.valueOf(65000), BigDecimal.valueOf(585000),
                5, "Thợ mang đầy đủ máy đo thăng bằng tia laser, máy hút bụi đầu khoan nên phòng khách sạch tinh không một hạt bụi. Rất ưng ý!");

        createCompletedJobWithReview(cust2020, v2020, catDonNha,
                "Tổng vệ sinh và phun khử trùng bề mặt định kỳ mùa dịch",
                "Vệ sinh toàn diện căn hộ 3 phòng ngủ, khử khuẩn bằng dung dịch Cloramin B chuẩn y tế.",
                "Sunrise City, Q.7, TP.HCM",
                LocalDateTime.of(2020, 10, 5, 8, 0),
                BigDecimal.valueOf(950000), BigDecimal.valueOf(95000), BigDecimal.valueOf(855000),
                5, "Đội thợ dọn nhà của cô Mai Hoa cực kỳ tỉ mỉ và thân thiện, làm sạch từng khe rèm và chân tủ. Dịch vụ tuyệt đối 5 sao.");

        createCompletedJobWithReview(cust2022, v2021, catKhoanTreo,
                "Lắp ráp trọn gói combo nội thất phòng ngủ: Giường tầng & Tủ quần áo",
                "Kiện hàng đóng hộp gỗ công nghiệp MDF chống ẩm, cần thợ có đủ máy bắt vít và kinh nghiệm siết cam chốt.",
                "Masteri Thảo Điền, TP. Thủ Đức",
                LocalDateTime.of(2022, 6, 20, 14, 0),
                BigDecimal.valueOf(850000), BigDecimal.valueOf(85000), BigDecimal.valueOf(765000),
                5, "Anh Thế Anh lắp ráp rất cẩn thận, căn chỉnh cánh tủ kín khít không bị lệch xệ. Sẽ tiếp tục ủng hộ!");

        createCompletedJobWithReview(cust2024, v2023, catKhoanTreo,
                "Lắp khóa vân tay thông minh cửa chính Kaadas & Camera an ninh",
                "Đục đố cửa gỗ lắp thân khóa điện tử, cài đặt ứng dụng điện thoại và đăng ký vân tay cho cả nhà.",
                "KĐT Sala, TP. Thủ Đức",
                LocalDateTime.of(2024, 3, 15, 15, 0),
                BigDecimal.valueOf(500000), BigDecimal.valueOf(50000), BigDecimal.valueOf(450000),
                5, "Kỹ thuật viên rất chuyên nghiệp, thao tác đục đố cửa tinh tế không trầy xước, hướng dẫn ông bà lớn tuổi dùng vân tay rất dễ hiểu.");

        createCompletedJobWithReview(cust2024, v2025, catDienNuoc,
                "Xử lý máy giặt Electrolux kêu to khi vắt và bảo dưỡng lồng giặt",
                "Thay bộ giảm chấn phuộc nhún và vệ sinh tẩy cặn canxi lồng giặt bằng dung dịch chuyên dụng.",
                "KĐT Sala, TP. Thủ Đức",
                LocalDateTime.of(2025, 2, 10, 9, 30),
                BigDecimal.valueOf(700000), BigDecimal.valueOf(70000), BigDecimal.valueOf(630000),
                5, "Đúng giờ, bắt đúng bệnh máy kêu, giá niêm yết rõ ràng không lo bị vẽ thêm lỗi. Rất hài lòng!");

        // 4. Bổ sung các phiếu nhập kho vật tư qua các năm (2018 - 2025)
        List<Product> prods = productRepository.findAll();
        if (!prods.isEmpty()) {
            Product p1 = prods.get(0);
            p1.setSoldCount(1850);
            productRepository.save(p1);
            StockReceipt sr1 = new StockReceipt(
                    p1, "Tổng kho Vật tư Cơ điện Schneider & Inax Việt Nam", 250,
                    BigDecimal.valueOf(55000), BigDecimal.valueOf(13750000),
                    "Nhập lô lớn phục vụ quý 4/2018, thanh toán 100% ủy nhiệm chi", "Quản trị viên Hệ thống"
            );
            sr1.setCreatedAt(LocalDateTime.of(2018, 10, 15, 8, 30));
            stockReceiptRepository.save(sr1);

            StockReceipt sr2 = new StockReceipt(
                    p1, "Công ty TNHH Thiết bị Nước & Vệ sinh Minh Phát", 300,
                    BigDecimal.valueOf(58000), BigDecimal.valueOf(17400000),
                    "Lô nhập định kỳ 2021, đối tác cung cấp phụ kiện ống nước chính hãng", "Quản trị viên Hệ thống"
            );
            sr2.setCreatedAt(LocalDateTime.of(2021, 6, 20, 10, 0));
            stockReceiptRepository.save(sr2);

            if (prods.size() > 1) {
                Product p2 = prods.get(1);
                p2.setSoldCount(2420);
                productRepository.save(p2);
                StockReceipt sr3 = new StockReceipt(
                        p2, "Nhà phân phối Phụ kiện Nội thất Hafele miền Nam", 200,
                        BigDecimal.valueOf(52000), BigDecimal.valueOf(10400000),
                        "Hàng nhập khẩu chuẩn quy chuẩn Đức, phục vụ thợ mộc 2023", "Quản trị viên Hệ thống"
                );
                sr3.setCreatedAt(LocalDateTime.of(2023, 9, 5, 14, 15));
                stockReceiptRepository.save(sr3);
            }
        }
    }

    private User createHistoryCustomer(String email, String fullName, String phone, String address, LocalDateTime joinedAt, BigDecimal balance) {
        User u = new User(email, passwordEncoder.encode("khach123"), fullName, phone, Role.CUSTOMER);
        u.setAddress(address);
        u.setCreatedAt(joinedAt);
        User saved = userRepository.save(u);

        Wallet w = new Wallet(saved);
        w.setBalance(balance);
        w.setUpdatedAt(joinedAt);
        walletRepository.save(w);

        WalletTransaction tx = new WalletTransaction(w, null, balance, TransactionType.DEPOSIT, balance, "Nạp tiền vào ví VieShift");
        tx.setCreatedAt(joinedAt);
        transactionRepository.save(tx);
        return saved;
    }

    private User createHistoryVieker(String email, String fullName, String phone, String primaryCat, String addCats,
                                     int expYears, int jobsCompleted, BigDecimal ratingAvg, int ratingCount,
                                     String bio, LocalDateTime joinedAt, BigDecimal walletBalance) {
        User u = new User(email, passwordEncoder.encode("tho123"), fullName, phone, Role.VIEKER);
        u.setCreatedAt(joinedAt);
        User saved = userRepository.save(u);

        Wallet w = new Wallet(saved);
        w.setBalance(walletBalance);
        w.setUpdatedAt(joinedAt);
        walletRepository.save(w);

        ViekerProfile p = new ViekerProfile(saved);
        p.setIdCardNumber("0790" + (10000000L + (long)(Math.random() * 89999999L)));
        p.setSkillsDescription(bio);
        p.setExperienceYears(expYears);
        p.setOperatingZone("TP. Hồ Chí Minh & Khu vực lân cận");
        p.setPrimaryCategory(primaryCat);
        p.setAdditionalCategories(addCats);
        p.setServiceRadiusKm(15);
        p.setVerificationStatus(VerificationStatus.APPROVED);
        p.setRatingAvg(ratingAvg);
        p.setRatingCount(ratingCount);
        p.setTotalJobsCompleted(jobsCompleted);
        p.setCreatedAt(joinedAt);
        profileRepository.save(p);
        return saved;
    }

    private void createCompletedJobWithReview(User customer, User vieker, JobCategory cat, String title, String desc,
                                              String address, LocalDateTime time, BigDecimal price, BigDecimal fee, BigDecimal net,
                                              int stars, String reviewText) {
        if (cat == null) return;
        Job j = new Job(customer, cat, title, desc, address, time, BigDecimal.valueOf(3.0), BigDecimal.valueOf(5.0), price, fee, net);
        j.setVieker(vieker);
        j.setStatus(JobStatus.COMPLETED);
        j.setCreatedAt(time.minusHours(4));
        j.setCompletedAt(time.plusHours(2));
        Job saved = jobRepository.save(j);

        Review r = new Review(saved, customer, vieker, stars, reviewText);
        r.setCreatedAt(time.plusHours(2).plusMinutes(15));
        reviewRepository.save(r);

        Wallet vWallet = walletRepository.findByUserId(vieker.getId()).orElse(null);
        if (vWallet != null) {
            WalletTransaction tx = new WalletTransaction(vWallet, saved, net, TransactionType.ESCROW_RELEASE, vWallet.getBalance(), "Nhận thanh toán việc #" + saved.getId() + " - " + title);
            tx.setCreatedAt(time.plusHours(2));
            transactionRepository.save(tx);
        }
    }
}
