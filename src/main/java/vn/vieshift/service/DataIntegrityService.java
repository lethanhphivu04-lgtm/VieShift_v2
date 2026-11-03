package vn.vieshift.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import vn.vieshift.entity.MinigameSpinLog;
import vn.vieshift.entity.Product;
import vn.vieshift.repository.MinigameSpinLogRepository;
import vn.vieshift.repository.ProductRepository;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/**
 * Co che bao mat & Chống Bịp CSDL (Anti-Tamper SQL Protection Engine)
 * Su dung ma hoa HMAC-SHA256 voi Secret Key luu tren Backend de phat hien
 * moi hanh vi sua doi truc tiep gia tien, so luong ton kho, hoac giai thuong trong MySQL/PostgreSQL/pgAdmin.
 */
@Service
public class DataIntegrityService {

    @Value("${vieshift.security.checksum-secret:VIESHIFT_SECRET_TAMPER_KEY_2026}")
    private String secretKey;

    private final ProductRepository productRepository;
    private final MinigameSpinLogRepository spinLogRepository;

    public DataIntegrityService(ProductRepository productRepository, MinigameSpinLogRepository spinLogRepository) {
        this.productRepository = productRepository;
        this.spinLogRepository = spinLogRepository;
    }

    /**
     * Tinh toan HMAC-SHA256 tra ve chuoi hex 64 ky tu
     */
    public String calculateHmac(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hmacBytes);
        } catch (Exception e) {
            throw new RuntimeException("Loi tao ma Checksum HMAC-SHA256", e);
        }
    }

    /**
     * Tinh Checksum cho San pham: ID | Gia | Ton kho | Ten san pham
     */
    public String computeProductChecksum(Product p) {
        if (p == null) return "";
        String data = (p.getId() != null ? p.getId() : "NEW") + "|" +
                (p.getPrice() != null ? p.getPrice().stripTrailingZeros().toPlainString() : "0") + "|" +
                (p.getStock() != null ? p.getStock() : 0) + "|" +
                (p.getName() != null ? p.getName().trim() : "");
        return calculateHmac(data);
    }

    /**
     * Kiem tra tinh toan ven cua San pham bang phep so sanh thoi gian khong doi (Constant-Time MessageDigest.isEqual)
     */
    public boolean verifyProductChecksum(Product p) {
        if (p == null || p.getChecksum() == null || p.getChecksum().isBlank()) {
            return false;
        }
        String expected = computeProductChecksum(p);
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                p.getChecksum().getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * Tinh Checksum cho Luot quay Minigame: ID | Phone | SliceID | RewardType | RewardDetail
     */
    public String computeSpinLogChecksum(MinigameSpinLog l) {
        if (l == null) return "";
        String data = (l.getId() != null ? l.getId() : "NEW") + "|" +
                (l.getPhone() != null ? l.getPhone().trim() : "") + "|" +
                (l.getSlice() != null ? l.getSlice().getId() : 0) + "|" +
                (l.getRewardType() != null ? l.getRewardType().trim() : "") + "|" +
                (l.getRewardDetail() != null ? l.getRewardDetail().trim() : "");
        return calculateHmac(data);
    }

    /**
     * Kiem tra tinh toan ven cua Luot quay Minigame
     */
    public boolean verifySpinLogChecksum(MinigameSpinLog l) {
        if (l == null || l.getChecksum() == null || l.getChecksum().isBlank()) {
            return false;
        }
        String expected = computeSpinLogChecksum(l);
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                l.getChecksum().getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * Quet toan bo CSDL (Tamper Scanner) va tra ve bao cao chi tiet
     */
    public IntegrityScanReport scanDatabaseIntegrity() {
        IntegrityScanReport report = new IntegrityScanReport();
        report.setScannedAt(LocalDateTime.now());

        // 1. Quet bang San pham (shop_products)
        List<Product> products = productRepository.findAll();
        report.setTotalProducts(products.size());
        int validProducts = 0;
        for (Product p : products) {
            if (verifyProductChecksum(p)) {
                validProducts++;
            } else {
                report.getTamperedProducts().add(new TamperedRecord(
                        "Sản phẩm #" + p.getId(),
                        p.getName(),
                        "Giá: " + p.getPrice() + " đ, Kho: " + p.getStock(),
                        p.getChecksum() != null ? p.getChecksum().substring(0, Math.min(12, p.getChecksum().length())) + "..." : "(Trống)",
                        computeProductChecksum(p).substring(0, 12) + "...",
                        "CRITICAL: Dữ liệu giá/kho đã bị sửa trực tiếp qua SQL/pgAdmin!"
                ));
            }
        }
        report.setValidProducts(validProducts);

        // 2. Quet bang Luot quay Minigame (minigame_spin_logs)
        List<MinigameSpinLog> spinLogs = spinLogRepository.findAll();
        report.setTotalSpinLogs(spinLogs.size());
        int validLogs = 0;
        for (MinigameSpinLog log : spinLogs) {
            if (verifySpinLogChecksum(log)) {
                validLogs++;
            } else {
                report.getTamperedSpinLogs().add(new TamperedRecord(
                        "Lượt quay #" + log.getId(),
                        "SĐT: " + log.getPhone(),
                        "Giải thưởng: " + log.getRewardDetail(),
                        log.getChecksum() != null ? log.getChecksum().substring(0, Math.min(12, log.getChecksum().length())) + "..." : "(Trống)",
                        computeSpinLogChecksum(log).substring(0, 12) + "...",
                        "HIGH: Kết quả trúng thưởng đã bị can thiệp trái phép trong CSDL!"
                ));
            }
        }
        report.setValidSpinLogs(validLogs);

        boolean isClean = report.getTamperedProducts().isEmpty() && report.getTamperedSpinLogs().isEmpty();
        report.setSystemSecure(isClean);
        report.setStatusSummary(isClean ? "CSDL TOÀN VẸN 100% — KHÔNG PHÁT HIỆN GIAN LẬN SQL" : "CẢNH BÁO: PHÁT HIỆN GIAN LẬN SỬA ĐỔI CSDL TRỰC TIẾP!");

        return report;
    }

    // DTO bao cao kiem tra toan ven CSDL
    public static class IntegrityScanReport {
        private LocalDateTime scannedAt;
        private boolean systemSecure = true;
        private String statusSummary;
        private int totalProducts = 0;
        private int validProducts = 0;
        private int totalSpinLogs = 0;
        private int validSpinLogs = 0;
        private List<TamperedRecord> tamperedProducts = new ArrayList<>();
        private List<TamperedRecord> tamperedSpinLogs = new ArrayList<>();

        public LocalDateTime getScannedAt() { return scannedAt; }
        public void setScannedAt(LocalDateTime scannedAt) { this.scannedAt = scannedAt; }

        public boolean isSystemSecure() { return systemSecure; }
        public void setSystemSecure(boolean systemSecure) { this.systemSecure = systemSecure; }

        public String getStatusSummary() { return statusSummary; }
        public void setStatusSummary(String statusSummary) { this.statusSummary = statusSummary; }

        public int getTotalProducts() { return totalProducts; }
        public void setTotalProducts(int totalProducts) { this.totalProducts = totalProducts; }

        public int getValidProducts() { return validProducts; }
        public void setValidProducts(int validProducts) { this.validProducts = validProducts; }

        public int getTotalSpinLogs() { return totalSpinLogs; }
        public void setTotalSpinLogs(int totalSpinLogs) { this.totalSpinLogs = totalSpinLogs; }

        public int getValidSpinLogs() { return validSpinLogs; }
        public void setValidSpinLogs(int validSpinLogs) { this.validSpinLogs = validSpinLogs; }

        public List<TamperedRecord> getTamperedProducts() { return tamperedProducts; }
        public List<TamperedRecord> getTamperedSpinLogs() { return tamperedSpinLogs; }
    }

    public static class TamperedRecord {
        private String identifier;
        private String target;
        private String currentData;
        private String currentChecksum;
        private String expectedChecksum;
        private String threatLevel;

        public TamperedRecord(String identifier, String target, String currentData, String currentChecksum, String expectedChecksum, String threatLevel) {
            this.identifier = identifier;
            this.target = target;
            this.currentData = currentData;
            this.currentChecksum = currentChecksum;
            this.expectedChecksum = expectedChecksum;
            this.threatLevel = threatLevel;
        }

        public String getIdentifier() { return identifier; }
        public String getTarget() { return target; }
        public String getCurrentData() { return currentData; }
        public String getCurrentChecksum() { return currentChecksum; }
        public String getExpectedChecksum() { return expectedChecksum; }
        public String getThreatLevel() { return threatLevel; }
    }
}
