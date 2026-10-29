package vn.vieshift.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.vieshift.entity.*;
import vn.vieshift.repository.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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

    public DataInitializer(UserRepository userRepository,
                           ViekerProfileRepository profileRepository,
                           JobCategoryRepository categoryRepository,
                           WalletRepository walletRepository,
                           JobRepository jobRepository,
                           WalletTransactionRepository transactionRepository,
                           ReviewRepository reviewRepository,
                           PasswordEncoder passwordEncoder,
                           vn.vieshift.repository.VoucherRepository voucherRepository) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.categoryRepository = categoryRepository;
        this.walletRepository = walletRepository;
        this.jobRepository = jobRepository;
        this.transactionRepository = transactionRepository;
        this.reviewRepository = reviewRepository;
        this.passwordEncoder = passwordEncoder;
        this.voucherRepository = voucherRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        initCategories();
        initSampleVouchers();
        User admin = initAdmin();
        User customer = initCustomer();
        User approvedVieker = initApprovedVieker();
        initDefaultVieker();
        initAdditionalEliteTaskers();
        User pendingVieker = initPendingVieker();
        initSampleJobs(customer, approvedVieker);
        cleanOldTransactionDescriptions();
        syncViekerCategoriesAndRadii();
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
}
