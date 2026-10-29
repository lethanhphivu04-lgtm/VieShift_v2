package vn.vieshift.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.vieshift.dto.ViekerSignupRequest;
import vn.vieshift.entity.*;
import vn.vieshift.exception.ResourceNotFoundException;
import vn.vieshift.repository.UserRepository;
import vn.vieshift.repository.ViekerProfileRepository;
import vn.vieshift.repository.WalletRepository;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Quy trình trở thành đối tác VieKer:
 * 1. Đăng ký tài khoản -> trạng thái DRAFT, gửi email chứa link hoàn thiện hồ sơ.
 * 2. Mở link -> điền thông tin + tải giấy tờ (chân dung, CCCD 2 mặt, lý lịch tư pháp, bằng lái).
 * 3. Nộp hồ sơ -> PENDING (chờ duyệt).
 * 4. Admin duyệt: APPROVED / NEEDS_UPDATE (yêu cầu bổ sung) / REJECTED - kèm email thông báo.
 */
@Service
@Transactional
public class ViekerOnboardingService {

    /** key -> tên hiển thị của từng loại giấy tờ bắt buộc */
    public static final Map<String, String> DOCUMENTS = new LinkedHashMap<>();
    static {
        DOCUMENTS.put("portrait", "Ảnh chân dung");
        DOCUMENTS.put("idFront", "CCCD mặt trước");
        DOCUMENTS.put("idBack", "CCCD mặt sau");
        DOCUMENTS.put("criminalRecord", "Lý lịch tư pháp");
        DOCUMENTS.put("driverLicense", "Bằng lái xe");
    }

    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp", "application/pdf");
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final java.time.format.DateTimeFormatter EXPIRE_FORMATTER =
            java.time.format.DateTimeFormatter.ofPattern("HH:mm - dd/MM/yyyy");

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final ViekerProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final Path uploadRoot;

    public ViekerOnboardingService(UserRepository userRepository,
                                   WalletRepository walletRepository,
                                   ViekerProfileRepository profileRepository,
                                   PasswordEncoder passwordEncoder,
                                   EmailService emailService,
                                   @Value("${vieshift.kyc-upload-dir}") String uploadDir) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.profileRepository = profileRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    // ========================= BƯỚC 1: ĐĂNG KÝ =========================

    public ViekerProfile signup(ViekerSignupRequest req) {
        String email = req.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email này đã được sử dụng trên hệ thống. Vui lòng đăng nhập hoặc dùng email khác.");
        }
        String fullName = (req.getLastName().trim() + " " + req.getFirstName().trim()).trim();
        User user = userRepository.save(new User(email, passwordEncoder.encode(req.getPassword()),
                fullName, req.getPhone().trim(), Role.VIEKER));
        walletRepository.save(new Wallet(user));

        ViekerProfile profile = new ViekerProfile(user);
        profile.setVerificationStatus(VerificationStatus.DRAFT);
        profile.setOperatingZone(req.getArea());
        profile.setPrimaryCategory(req.getCategory());
        profile.setOnboardingToken(newToken());
        // Thời hạn sử dụng cố định: 24 giờ kể từ lúc tạo
        profile.setTokenExpiresAt(LocalDateTime.now().plusHours(24));
        profileRepository.save(profile);

        String expireStr = profile.getTokenExpiresAt().format(EXPIRE_FORMATTER);
        emailService.send(email, "VieShift - Hoàn thiện hồ sơ đối tác VieKer của bạn", "vieker-welcome",
                Map.of("name", req.getFirstName().trim(),
                        "link", onboardingLink(profile),
                        "expireTime", expireStr));
        return profile;
    }

    public String resendOnboardingEmail(String token) {
        ViekerProfile profile = profileRepository.findByOnboardingToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ với mã xác nhận"));
        
        // Cấp token mới và gia hạn 24 giờ
        profile.setOnboardingToken(newToken());
        profile.setTokenExpiresAt(LocalDateTime.now().plusHours(24));
        profileRepository.save(profile);

        String expireStr = profile.getTokenExpiresAt().format(EXPIRE_FORMATTER);
        emailService.send(profile.getUser().getEmail(), "VieShift - Hoàn thiện hồ sơ đối tác VieKer của bạn", "vieker-welcome",
                Map.of("name", profile.getUser().getFullName(),
                        "link", onboardingLink(profile),
                        "expireTime", expireStr));
        return profile.getOnboardingToken();
    }

    // ========================= TRA CỨU =========================

    @Transactional(readOnly = true)
    public ViekerProfile getByToken(String token) {
        return profileRepository.findByOnboardingToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Đường dẫn không hợp lệ hoặc đã hết hạn"));
    }

    @Transactional(readOnly = true)
    public ViekerProfile getById(Long id) {
        return profileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ ID: " + id));
    }

    /** Đảm bảo hồ sơ có token (dữ liệu cũ có thể chưa có) và trả về token. */
    public String ensureToken(ViekerProfile profile) {
        if (profile.getOnboardingToken() == null || profile.isTokenExpired()) {
            profile.setOnboardingToken(newToken());
            profile.setTokenExpiresAt(LocalDateTime.now().plusHours(24));
            profileRepository.save(profile);
        }
        return profile.getOnboardingToken();
    }

    public String onboardingLink(ViekerProfile profile) {
        return emailService.getBaseUrl() + "/onboarding/" + profile.getOnboardingToken();
    }

    // ========================= BƯỚC 2-3: NỘP HỒ SƠ =========================

    public void submit(String token, String idCardNumber, LocalDate dob, String address,
                       String driverLicenseNumber, String skills, Integer experienceYears,
                       Map<String, MultipartFile> files) {
        ViekerProfile p = getByToken(token);
        if (!p.isEditable()) {
            throw new IllegalStateException("Hồ sơ đã được nộp và đang trong quá trình xét duyệt.");
        }

        if (idCardNumber == null || !idCardNumber.trim().matches("\\d{12}")) {
            throw new IllegalArgumentException("Số CCCD phải gồm đúng 12 chữ số.");
        }
        if (dob == null || dob.isAfter(LocalDate.now().minusYears(18))) {
            throw new IllegalArgumentException("Đối tác VieKer phải đủ 18 tuổi trở lên.");
        }
        if (address == null || address.isBlank() || skills == null || skills.isBlank()) {
            throw new IllegalArgumentException("Vui lòng điền đầy đủ địa chỉ thường trú và mô tả kỹ năng.");
        }

        // Kiểm tra & lưu từng giấy tờ: bắt buộc nếu chưa có, hoặc nếu admin yêu cầu bổ sung
        for (String key : DOCUMENTS.keySet()) {
            MultipartFile f = files.get(key);
            boolean hasNew = f != null && !f.isEmpty();
            boolean required = currentPath(p, key) == null || p.needsDoc(key);
            if (!hasNew && required) {
                throw new IllegalArgumentException("Vui lòng tải lên: " + DOCUMENTS.get(key));
            }
            if (hasNew) {
                setPath(p, key, store(p.getId(), key, f));
            }
        }

        p.setIdCardNumber(idCardNumber.trim());
        p.setDateOfBirth(dob);
        p.setPermanentAddress(address.trim());
        p.setDriverLicenseNumber(driverLicenseNumber != null ? driverLicenseNumber.trim() : null);
        p.setSkillsDescription(skills.trim());
        p.setExperienceYears(experienceYears != null ? Math.max(0, experienceYears) : 0);
        p.setVerificationStatus(VerificationStatus.PENDING);
        p.setDocsToUpdate(null);
        p.setSubmittedAt(LocalDateTime.now());
        profileRepository.save(p);

        emailService.send(p.getUser().getEmail(), "VieShift - Đã nhận hồ sơ đối tác của bạn", "vieker-submitted",
                Map.of("name", p.getUser().getFullName(), "link", onboardingLink(p)));
    }

    // ========================= BƯỚC 4: ADMIN DUYỆT =========================

    public void approve(Long profileId) {
        ViekerProfile p = getById(profileId);
        p.setVerificationStatus(VerificationStatus.APPROVED);
        p.setRejectionReason(null);
        p.setDocsToUpdate(null);
        p.setReviewedAt(LocalDateTime.now());
        profileRepository.save(p);
        emailService.send(p.getUser().getEmail(), "Chúc mừng! Hồ sơ VieKer của bạn đã được phê duyệt", "vieker-approved",
                Map.of("name", p.getUser().getFullName(), "link", emailService.getBaseUrl() + "/login"));
    }

    public void requestUpdate(Long profileId, List<String> docs, String note) {
        if ((docs == null || docs.isEmpty()) && (note == null || note.isBlank())) {
            throw new IllegalArgumentException("Vui lòng chọn giấy tờ cần bổ sung hoặc ghi chú nội dung cần thay đổi.");
        }
        ViekerProfile p = getById(profileId);
        List<String> valid = docs == null ? List.of() : docs.stream().filter(DOCUMENTS::containsKey).toList();
        p.setVerificationStatus(VerificationStatus.NEEDS_UPDATE);
        p.setDocsToUpdate(String.join(",", valid));
        p.setRejectionReason(note != null ? note.trim() : null);
        p.setReviewedAt(LocalDateTime.now());
        profileRepository.save(p);

        List<String> docNames = valid.stream().map(DOCUMENTS::get).toList();
        emailService.send(p.getUser().getEmail(), "VieShift - Hồ sơ của bạn cần bổ sung thông tin", "vieker-needs-update",
                Map.of("name", p.getUser().getFullName(), "docs", docNames,
                        "note", note == null ? "" : note.trim(), "link", onboardingLink(p)));
    }

    public void reject(Long profileId, String reason) {
        ViekerProfile p = getById(profileId);
        String r = (reason == null || reason.isBlank()) ? "Hồ sơ chưa đáp ứng tiêu chuẩn đối tác của VieShift." : reason.trim();
        p.setVerificationStatus(VerificationStatus.REJECTED);
        p.setRejectionReason(r);
        p.setReviewedAt(LocalDateTime.now());
        profileRepository.save(p);
        emailService.send(p.getUser().getEmail(), "VieShift - Kết quả xét duyệt hồ sơ đối tác", "vieker-rejected",
                Map.of("name", p.getUser().getFullName(), "reason", r));
    }

    // ========================= FILE KYC =========================

    /** Trả về đường dẫn file giấy tờ (chỉ dùng cho admin). */
    @Transactional(readOnly = true)
    public Path resolveDocument(Long profileId, String key) {
        String rel = currentPath(getById(profileId), key);
        if (rel == null) throw new ResourceNotFoundException("Chưa có giấy tờ: " + key);
        Path path = uploadRoot.resolve(rel).normalize();
        if (!path.startsWith(uploadRoot) || !Files.exists(path)) {
            throw new ResourceNotFoundException("Không tìm thấy tệp giấy tờ");
        }
        return path;
    }

    private String store(Long profileId, String key, MultipartFile file) {
        String type = file.getContentType();
        if (type == null || !ALLOWED_TYPES.contains(type)) {
            throw new IllegalArgumentException(DOCUMENTS.get(key) + ": chỉ chấp nhận ảnh JPG, PNG, WEBP hoặc PDF.");
        }
        if (file.getSize() > 10L * 1024 * 1024) {
            throw new IllegalArgumentException(DOCUMENTS.get(key) + ": dung lượng tối đa 10MB.");
        }
        String ext = switch (type) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "application/pdf" -> ".pdf";
            default -> ".jpg";
        };
        String rel = profileId + "/" + key + "-" + System.currentTimeMillis() + ext;
        Path target = uploadRoot.resolve(rel).normalize();
        try (InputStream in = file.getInputStream()) {
            Files.createDirectories(target.getParent());
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Không thể lưu tệp, vui lòng thử lại.");
        }
        return rel;
    }

    public static String currentPath(ViekerProfile p, String key) {
        return switch (key) {
            case "portrait" -> p.getPortraitPath();
            case "idFront" -> p.getIdFrontPath();
            case "idBack" -> p.getIdBackPath();
            case "criminalRecord" -> p.getCriminalRecordPath();
            case "driverLicense" -> p.getDriverLicensePath();
            default -> null;
        };
    }

    private static void setPath(ViekerProfile p, String key, String path) {
        switch (key) {
            case "portrait" -> p.setPortraitPath(path);
            case "idFront" -> p.setIdFrontPath(path);
            case "idBack" -> p.setIdBackPath(path);
            case "criminalRecord" -> p.setCriminalRecordPath(path);
            case "driverLicense" -> p.setDriverLicensePath(path);
            default -> { }
        }
    }

    private static String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
