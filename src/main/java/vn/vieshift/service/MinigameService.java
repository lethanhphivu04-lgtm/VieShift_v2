package vn.vieshift.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.vieshift.entity.*;
import vn.vieshift.repository.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class MinigameService {

    private static final java.security.SecureRandom SECURE_RANDOM = new java.security.SecureRandom();

    private final MinigameSliceRepository sliceRepository;
    private final MinigameSpinLogRepository spinLogRepository;
    private final MinigameGiftcodeRepository giftcodeRepository;
    private final MinigameGiftcodeRedemptionRepository redemptionRepository;
    private final JobRepository jobRepository;
    private final ShopOrderRepository shopOrderRepository;
    private final ReviewRepository reviewRepository;
    private final VoucherRepository voucherRepository;
    private final DataIntegrityService dataIntegrityService;
    private final WalletService walletService;

    public MinigameService(MinigameSliceRepository sliceRepository,
                           MinigameSpinLogRepository spinLogRepository,
                           MinigameGiftcodeRepository giftcodeRepository,
                           MinigameGiftcodeRedemptionRepository redemptionRepository,
                           JobRepository jobRepository,
                           ShopOrderRepository shopOrderRepository,
                           ReviewRepository reviewRepository,
                           VoucherRepository voucherRepository,
                           DataIntegrityService dataIntegrityService,
                           WalletService walletService) {
        this.sliceRepository = sliceRepository;
        this.spinLogRepository = spinLogRepository;
        this.giftcodeRepository = giftcodeRepository;
        this.redemptionRepository = redemptionRepository;
        this.jobRepository = jobRepository;
        this.shopOrderRepository = shopOrderRepository;
        this.reviewRepository = reviewRepository;
        this.voucherRepository = voucherRepository;
        this.dataIntegrityService = dataIntegrityService;
        this.walletService = walletService;
    }

    @Transactional(readOnly = true)
    public List<MinigameSlice> getActiveSlices() {
        return sliceRepository.findByActiveTrueOrderBySliceIndexAsc();
    }

    @Transactional(readOnly = true)
    public List<MinigameSlice> getAllSlices() {
        return sliceRepository.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC, "sliceIndex"));
    }

    public MinigameSlice updateSlice(Long id, String label, Integer winRate, String couponCode, BigDecimal rewardValue) {
        MinigameSlice slice = sliceRepository.findById(id)
                .orElseThrow(() -> new vn.vieshift.exception.ResourceNotFoundException("Không tìm thấy ô vòng quay #" + id));
        if (label != null && !label.isBlank()) {
            slice.setLabel(label.trim());
        }
        if (winRate != null && winRate >= 0) {
            slice.setWinRate(winRate);
        }
        if (couponCode != null) {
            slice.setCouponCode(couponCode.isBlank() ? null : couponCode.trim().toUpperCase());
        }
        if (rewardValue != null && rewardValue.compareTo(BigDecimal.ZERO) >= 0) {
            slice.setRewardValue(rewardValue);
        }
        return sliceRepository.save(slice);
    }

    @Transactional(readOnly = true)
    public List<MinigameGiftcode> getAllGiftcodes() {
        return giftcodeRepository.findAll();
    }

    public MinigameGiftcode createGiftcode(String code, int spinCount, int maxUses) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Mã Giftcode không được để trống.");
        }
        String cleanCode = code.trim().toUpperCase();
        Optional<MinigameGiftcode> existing = giftcodeRepository.findByCodeIgnoreCase(cleanCode);
        if (existing.isPresent()) {
            MinigameGiftcode g = existing.get();
            g.setSpinCount(Math.max(1, spinCount));
            g.setMaxUses(Math.max(1, maxUses));
            g.setActive(true);
            return giftcodeRepository.save(g);
        }
        return giftcodeRepository.save(new MinigameGiftcode(cleanCode, Math.max(1, spinCount), Math.max(1, maxUses)));
    }

    public void deleteGiftcode(Long id) {
        giftcodeRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<MinigameSpinLog> getRecentSpinLogs() {
        return spinLogRepository.findTop15ByOrderByCreatedAtDesc();
    }

    /**
     * Tinh toan tong luot quay cua User hoac So dien thoai
     */
    @Transactional(readOnly = true)
    public SpinBalanceDetail calculateSpins(User user, String phone) {
        SpinBalanceDetail detail = new SpinBalanceDetail();
        int baseSpins = 1; // Moi khach deu duoc tang 1 luot quay khoi dau
        int completedJobs = 0;
        int shopOrders = 0;
        int reviews = 0;
        int giftcodeSpins = 0;
        long usedSpins = 0;

        if (user != null) {
            completedJobs = (int) jobRepository.countByCustomerIdAndStatus(user.getId(), JobStatus.COMPLETED);
            shopOrders = (int) shopOrderRepository.countByCustomerId(user.getId());
            reviews = (int) reviewRepository.countByCustomerId(user.getId());
            usedSpins = spinLogRepository.countByUser(user);

            List<MinigameGiftcodeRedemption> redemptions = redemptionRepository.findByUser(user);
            for (MinigameGiftcodeRedemption r : redemptions) {
                giftcodeSpins += r.getSpinCountAwarded();
            }
        } else if (phone != null && !phone.isBlank()) {
            String cleanPhone = phone.trim();
            shopOrders = (int) shopOrderRepository.countByRecipientPhone(cleanPhone);
            usedSpins = spinLogRepository.countByPhone(cleanPhone);

            List<MinigameGiftcodeRedemption> redemptions = redemptionRepository.findByPhone(cleanPhone);
            for (MinigameGiftcodeRedemption r : redemptions) {
                giftcodeSpins += r.getSpinCountAwarded();
            }
        }

        int totalEarned = baseSpins + completedJobs + shopOrders + reviews + giftcodeSpins;
        int remaining = (int) Math.max(0, totalEarned - usedSpins);

        detail.setBaseSpins(baseSpins);
        detail.setCompletedJobs(completedJobs);
        detail.setShopOrders(shopOrders);
        detail.setReviews(reviews);
        detail.setGiftcodeSpins(giftcodeSpins);
        detail.setTotalEarned(totalEarned);
        detail.setUsedSpins((int) usedSpins);
        detail.setRemainingSpins(remaining);

        return detail;
    }

    /**
     * Thuc hien luot quay may man (Weighted Random Algorithm)
     */
    public SpinResult spin(User user, String phone) {
        String cleanPhone = (phone != null && !phone.isBlank())
                ? phone.trim()
                : (user != null && user.getPhone() != null ? user.getPhone() : "0900000000");

        SpinBalanceDetail balance = calculateSpins(user, cleanPhone);
        if (balance.getRemainingSpins() <= 0) {
            throw new IllegalStateException("Bạn đã sử dụng hết lượt quay may mắn. Hãy đặt việc mới, mua sắm vật tư hoặc nhập mã Giftcode để nhận thêm lượt quay!");
        }

        List<MinigameSlice> slices = getActiveSlices();
        if (slices.isEmpty()) {
            throw new IllegalStateException("Hệ thống vòng quay đang bảo trì ô giải thưởng.");
        }

        // Tinh tong trong so ti le
        int totalWeight = 0;
        for (MinigameSlice s : slices) {
            totalWeight += (s.getWinRate() != null && s.getWinRate() > 0) ? s.getWinRate() : 1;
        }

        // Random theo trong so bang SecureRandom
        int randVal = SECURE_RANDOM.nextInt(totalWeight);
        int runningWeight = 0;
        MinigameSlice winningSlice = slices.get(0);

        for (MinigameSlice s : slices) {
            int w = (s.getWinRate() != null && s.getWinRate() > 0) ? s.getWinRate() : 1;
            runningWeight += w;
            if (randVal < runningWeight) {
                winningSlice = s;
                break;
            }
        }

        // Tao ban ghi nhat ky luot quay
        MinigameSpinLog log = new MinigameSpinLog();
        log.setUser(user);
        log.setPhone(cleanPhone);
        log.setSlice(winningSlice);
        log.setRewardType(winningSlice.getType());
        log.setRewardDetail(winningSlice.getLabel());
        log.setCreatedAt(LocalDateTime.now());

        // Ky so HMAC-SHA256 (Anti-Bip CSDL SQL)
        log = spinLogRepository.save(log);
        String checksum = dataIntegrityService.computeSpinLogChecksum(log);
        log.setChecksum(checksum);
        spinLogRepository.save(log);

        // Neu trung Voucher, tao hoac kich hoat voucher cho khach
        if ("COUPON".equalsIgnoreCase(winningSlice.getType()) && winningSlice.getCouponCode() != null) {
            ensureVoucherAvailable(winningSlice);
        }

        int newRemaining = balance.getRemainingSpins() - 1;

        SpinResult res = new SpinResult();
        res.setSpinLogId(log.getId());
        res.setSliceIndex(winningSlice.getSliceIndex());
        res.setSliceId(winningSlice.getId());
        res.setSliceLabel(winningSlice.getLabel());
        res.setSliceType(winningSlice.getType());
        res.setCouponCode(winningSlice.getCouponCode());
        res.setRewardValue(winningSlice.getRewardValue());
        res.setMessage(winningSlice.getMessage());
        res.setRemainingSpins(newRemaining);
        res.setChecksum(checksum);

        return res;
    }

    /**
     * Lay Kho Qua & Lich su quay cua rieng User hoac SDT
     */
    @Transactional(readOnly = true)
    public List<MinigameSpinLog> getMySpinHistory(User user, String phone) {
        if (user != null) {
            return spinLogRepository.findByUserOrderByCreatedAtDesc(user);
        }
        if (phone != null && !phone.isBlank()) {
            return spinLogRepository.findByPhoneOrderByCreatedAtDesc(phone.trim());
        }
        return Collections.emptyList();
    }

    /**
     * Quy doi Qua Hien Vat hoac Voucher trung thuong thanh Tien cong truc tiep vao Vi VieShift
     */
    public BigDecimal redeemPrizeToWallet(Long spinLogId, User user) {
        if (user == null) {
            throw new IllegalStateException("Vui lòng đăng nhập tài khoản để quy đổi giải thưởng vào Ví VieShift.");
        }
        MinigameSpinLog log = spinLogRepository.findByIdForUpdate(spinLogId)
                .orElseThrow(() -> new vn.vieshift.exception.ResourceNotFoundException("Không tìm thấy giải thưởng #" + spinLogId));

        // Kiểm tra quyền sở hữu giải thưởng (Chống cướp giải thưởng IDOR)
        if (log.getUser() != null && !log.getUser().getId().equals(user.getId())) {
            throw new vn.vieshift.exception.UnauthorizedActionException("Giải thưởng này không thuộc về tài khoản của bạn!");
        }
        if (log.getUser() == null && log.getPhone() != null && !log.getPhone().isBlank()
                && user.getPhone() != null && !log.getPhone().equals(user.getPhone().trim())) {
            throw new vn.vieshift.exception.UnauthorizedActionException("Số điện thoại trúng thưởng không khớp với tài khoản của bạn!");
        }

        if (log.isClaimed()) {
            throw new IllegalStateException("Giải thưởng này đã được bạn nhận hoặc quy đổi trước đó!");
        }
        if ("MISS".equalsIgnoreCase(log.getRewardType())) {
            throw new IllegalStateException("Lượt quay này không có giá trị quy đổi.");
        }
        if (!dataIntegrityService.verifySpinLogChecksum(log)) {
            throw new IllegalStateException("Dữ liệu giải thưởng đang được kiểm tra đồng bộ hệ thống. Vui lòng liên hệ CSKH.");
        }

        BigDecimal amount = (log.getSlice() != null && log.getSlice().getRewardValue() != null
                && log.getSlice().getRewardValue().compareTo(BigDecimal.ZERO) > 0)
                ? log.getSlice().getRewardValue()
                : BigDecimal.valueOf(20000);

        walletService.deposit(user.getId(), amount,
                "Quy đổi giải thưởng Vòng Quay May Mắn: " + log.getRewardDetail());

        log.setUser(user);
        log.setClaimed(true);
        log.setClaimMethod("WALLET");
        log.setClaimedNote("Đã quy đổi +" + String.format("%,.0f", amount) + "đ vào Ví VieShift");
        spinLogRepository.save(log);

        return amount;
    }

    /**
     * Nhan Qua Hien Vat (Tao don hang 0d giao tan nha trong VieShift Mall)
     */
    public ShopOrder claimPhysicalGiftOrder(Long spinLogId, User user,
                                            String recipientName, String recipientPhone, String shippingAddress) {
        if (user == null) {
            throw new IllegalStateException("Vui lòng đăng nhập tài khoản để xác nhận địa chỉ nhận quà tặng.");
        }
        MinigameSpinLog log = spinLogRepository.findByIdForUpdate(spinLogId)
                .orElseThrow(() -> new vn.vieshift.exception.ResourceNotFoundException("Không tìm thấy giải thưởng #" + spinLogId));

        // Kiểm tra quyền sở hữu giải thưởng
        if (log.getUser() != null && !log.getUser().getId().equals(user.getId())) {
            throw new vn.vieshift.exception.UnauthorizedActionException("Giải thưởng này không thuộc về tài khoản của bạn!");
        }
        if (log.getUser() == null && log.getPhone() != null && !log.getPhone().isBlank()
                && user.getPhone() != null && !log.getPhone().equals(user.getPhone().trim())) {
            throw new vn.vieshift.exception.UnauthorizedActionException("Số điện thoại trúng thưởng không khớp với tài khoản của bạn!");
        }

        if (log.isClaimed()) {
            throw new IllegalStateException("Phần quà này đã được bạn xác nhận nhận thưởng trước đó!");
        }
        if (!"GIFT".equalsIgnoreCase(log.getRewardType())) {
            throw new IllegalStateException("Chỉ giải thưởng Quà hiện vật mới hỗ trợ tạo đơn giao tận nhà.");
        }
        if (!dataIntegrityService.verifySpinLogChecksum(log)) {
            throw new IllegalStateException("Dữ liệu giải thưởng đang được kiểm tra đồng bộ hệ thống.");
        }

        ShopOrder order = new ShopOrder();
        order.setCustomer(user);
        order.setOrderType("STANDALONE");
        order.setRecipientName(recipientName != null && !recipientName.isBlank() ? recipientName.trim() : user.getFullName());
        order.setRecipientPhone(recipientPhone != null && !recipientPhone.isBlank() ? recipientPhone.trim() : user.getPhone());
        order.setShippingAddress(shippingAddress != null && !shippingAddress.isBlank() ? shippingAddress.trim() : "Giao tận nhà theo địa chỉ tài khoản");
        order.setItemsSummary("[QUÀ TẶNG VÒNG QUAY 0Đ] 1x " + log.getRewardDetail());
        order.setSubtotalAmount(BigDecimal.ZERO);
        order.setShippingFee(BigDecimal.ZERO);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setTotalAmount(BigDecimal.ZERO);
        order.setPaymentMethod("WALLET");
        order.setStatus("PAID");
        order.setNotes("Đơn quà tặng trúng thưởng từ Vòng Quay May Mắn VieShift (Miễn phí 100% vật tư & phí giao hàng)");

        ShopOrder savedOrder = shopOrderRepository.save(order);

        log.setUser(user);
        log.setClaimed(true);
        log.setClaimMethod("SHIP_GIFT");
        log.setClaimedNote("Đã tạo đơn giao quà 0đ #VS-MALL-" + savedOrder.getId());
        spinLogRepository.save(log);

        return savedOrder;
    }

    /**
     * Nhap Giftcode doi them luot quay
     */
    public int redeemGiftcode(String code, String phone, User user) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập mã sự kiện Giftcode.");
        }
        String cleanPhone = (phone != null && !phone.isBlank())
                ? phone.trim()
                : (user != null && user.getPhone() != null ? user.getPhone() : "");

        if (cleanPhone.isBlank()) {
            throw new IllegalArgumentException("Vui lòng cung cấp số điện thoại để lưu lượt quay.");
        }

        MinigameGiftcode giftcode = giftcodeRepository.findByCodeIgnoreCaseAndActiveTrue(code.trim())
                .orElseThrow(() -> new IllegalArgumentException("Mã Giftcode không tồn tại hoặc đã hết hạn."));

        if (giftcode.getUsedCount() >= giftcode.getMaxUses()) {
            throw new IllegalStateException("Mã Giftcode này đã hết lượt sử dụng.");
        }

        // Kiem tra xem phone hoac user da tung nhan chua
        if (user != null && redemptionRepository.existsByGiftcodeAndUser(giftcode, user)) {
            throw new IllegalStateException("Bạn đã sử dụng mã sự kiện này rồi!");
        }
        if (redemptionRepository.existsByGiftcodeAndPhone(giftcode, cleanPhone)) {
            throw new IllegalStateException("Số điện thoại này đã nhận thưởng từ mã Giftcode này rồi!");
        }

        // Ghi nhan doi ma
        MinigameGiftcodeRedemption redemption = new MinigameGiftcodeRedemption(
                giftcode, cleanPhone, user, giftcode.getSpinCount()
        );
        redemptionRepository.save(redemption);

        giftcode.setUsedCount(giftcode.getUsedCount() + 1);
        giftcodeRepository.save(giftcode);

        return giftcode.getSpinCount();
    }

    /**
     * Danh sach 15 nguoi trung thuong gan day (co che mask so dien thoai)
     */
    @Transactional(readOnly = true)
    public List<WinnerDTO> getRecentWinners() {
        List<MinigameSpinLog> logs = spinLogRepository.findTop15ByOrderByCreatedAtDesc();
        List<WinnerDTO> dtos = new ArrayList<>();
        for (MinigameSpinLog l : logs) {
            if ("MISS".equalsIgnoreCase(l.getRewardType())) continue; // Khong hien luot quay hut

            String maskedPhone = maskPhone(l.getPhone());
            String userName = (l.getUser() != null && l.getUser().getFullName() != null)
                    ? maskName(l.getUser().getFullName())
                    : "Khách hàng " + maskedPhone;

            dtos.add(new WinnerDTO(
                    userName,
                    maskedPhone,
                    l.getRewardDetail(),
                    l.getRewardType(),
                    l.getCreatedAt()
            ));
        }
        return dtos;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return "098***219";
        return phone.substring(0, 4) + "***" + phone.substring(phone.length() - 3);
    }

    private String maskName(String name) {
        if (name == null || name.isBlank()) return "Khách hàng";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) return parts[0];
        return parts[0] + " *** " + parts[parts.length - 1];
    }

    private void ensureVoucherAvailable(MinigameSlice slice) {
        if (slice.getCouponCode() == null) return;
        Optional<Voucher> opt = voucherRepository.findByCodeIgnoreCase(slice.getCouponCode());
        if (opt.isEmpty()) {
            Voucher v = new Voucher();
            v.setCode(slice.getCouponCode());
            v.setTitle("Voucher Vòng Quay: " + slice.getLabel());
            v.setDescription("Phần thưởng may mắn từ Vòng Quay May Mắn VieShift");
            v.setDiscountType(DiscountType.FIXED_AMOUNT);
            v.setDiscountValue(slice.getRewardValue() != null ? slice.getRewardValue() : new BigDecimal("20000"));
            v.setMinOrderAmount(BigDecimal.ZERO);
            v.setMaxUses(1000);
            v.setCurrentUses(0);
            v.setPublic(true);
            v.setActive(true);
            v.setStartDate(LocalDateTime.now().minusDays(1));
            v.setEndDate(LocalDateTime.now().plusMonths(6));
            voucherRepository.save(v);
        }
    }

    // DTOs
    public static class SpinBalanceDetail {
        private int baseSpins;
        private int completedJobs;
        private int shopOrders;
        private int reviews;
        private int giftcodeSpins;
        private int totalEarned;
        private int usedSpins;
        private int remainingSpins;

        public int getBaseSpins() { return baseSpins; }
        public void setBaseSpins(int baseSpins) { this.baseSpins = baseSpins; }
        public int getCompletedJobs() { return completedJobs; }
        public void setCompletedJobs(int completedJobs) { this.completedJobs = completedJobs; }
        public int getShopOrders() { return shopOrders; }
        public void setShopOrders(int shopOrders) { this.shopOrders = shopOrders; }
        public int getReviews() { return reviews; }
        public void setReviews(int reviews) { this.reviews = reviews; }
        public int getGiftcodeSpins() { return giftcodeSpins; }
        public void setGiftcodeSpins(int giftcodeSpins) { this.giftcodeSpins = giftcodeSpins; }
        public int getTotalEarned() { return totalEarned; }
        public void setTotalEarned(int totalEarned) { this.totalEarned = totalEarned; }
        public int getUsedSpins() { return usedSpins; }
        public void setUsedSpins(int usedSpins) { this.usedSpins = usedSpins; }
        public int getRemainingSpins() { return remainingSpins; }
        public void setRemainingSpins(int remainingSpins) { this.remainingSpins = remainingSpins; }
    }

    public static class SpinResult {
        private Long spinLogId;
        private int sliceIndex;
        private Long sliceId;
        private String sliceLabel;
        private String sliceType;
        private String couponCode;
        private BigDecimal rewardValue;
        private String message;
        private int remainingSpins;
        private String checksum;

        public Long getSpinLogId() { return spinLogId; }
        public void setSpinLogId(Long spinLogId) { this.spinLogId = spinLogId; }
        public int getSliceIndex() { return sliceIndex; }
        public void setSliceIndex(int sliceIndex) { this.sliceIndex = sliceIndex; }
        public Long getSliceId() { return sliceId; }
        public void setSliceId(Long sliceId) { this.sliceId = sliceId; }
        public String getSliceLabel() { return sliceLabel; }
        public void setSliceLabel(String sliceLabel) { this.sliceLabel = sliceLabel; }
        public String getSliceType() { return sliceType; }
        public void setSliceType(String sliceType) { this.sliceType = sliceType; }
        public String getCouponCode() { return couponCode; }
        public void setCouponCode(String couponCode) { this.couponCode = couponCode; }
        public BigDecimal getRewardValue() { return rewardValue; }
        public void setRewardValue(BigDecimal rewardValue) { this.rewardValue = rewardValue; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public int getRemainingSpins() { return remainingSpins; }
        public void setRemainingSpins(int remainingSpins) { this.remainingSpins = remainingSpins; }
        public String getChecksum() { return checksum; }
        public void setChecksum(String checksum) { this.checksum = checksum; }
    }

    public static class WinnerDTO {
        private String customerName;
        private String phone;
        private String rewardDetail;
        private String rewardType;
        private LocalDateTime wonAt;

        public WinnerDTO(String customerName, String phone, String rewardDetail, String rewardType, LocalDateTime wonAt) {
            this.customerName = customerName;
            this.phone = phone;
            this.rewardDetail = rewardDetail;
            this.rewardType = rewardType;
            this.wonAt = wonAt;
        }

        public String getCustomerName() { return customerName; }
        public String getPhone() { return phone; }
        public String getRewardDetail() { return rewardDetail; }
        public String getRewardType() { return rewardType; }
        public LocalDateTime getWonAt() { return wonAt; }
    }
}
