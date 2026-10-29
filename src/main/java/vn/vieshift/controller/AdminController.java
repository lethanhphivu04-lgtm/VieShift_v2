package vn.vieshift.controller;

import jakarta.validation.Valid;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.vieshift.dto.CategoryRequest;
import vn.vieshift.dto.ResolveDisputeRequest;
import vn.vieshift.entity.*;
import vn.vieshift.repository.JobCategoryRepository;
import vn.vieshift.repository.JobRepository;
import vn.vieshift.repository.NotificationRepository;
import vn.vieshift.repository.UserRepository;
import vn.vieshift.repository.WalletRepository;
import vn.vieshift.service.DashboardService;
import vn.vieshift.service.DisputeService;
import vn.vieshift.service.JobService;
import vn.vieshift.service.UserService;
import vn.vieshift.service.ViekerOnboardingService;
import vn.vieshift.service.ViekerProfileService;
import vn.vieshift.service.WalletService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final DashboardService dashboardService;
    private final ViekerProfileService profileService;
    private final ViekerOnboardingService onboardingService;
    private final DisputeService disputeService;
    private final UserService userService;
    private final WalletService walletService;
    private final JobService jobService;
    private final JobCategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final JobRepository jobRepository;
    private final vn.vieshift.repository.WalletTransactionRepository transactionRepository;
    private final vn.vieshift.repository.ViekerProfileRepository profileRepository;
    private final vn.vieshift.service.NotificationService notificationService;
    private final vn.vieshift.service.VoucherService voucherService;
    private final WalletRepository walletRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationRepository notificationRepository;

    public AdminController(DashboardService dashboardService,
                           ViekerProfileService profileService,
                           ViekerOnboardingService onboardingService,
                           DisputeService disputeService,
                           UserService userService,
                           WalletService walletService,
                           JobService jobService,
                           JobCategoryRepository categoryRepository,
                           UserRepository userRepository,
                           JobRepository jobRepository,
                           vn.vieshift.repository.WalletTransactionRepository transactionRepository,
                           vn.vieshift.repository.ViekerProfileRepository profileRepository,
                           vn.vieshift.service.NotificationService notificationService,
                           vn.vieshift.service.VoucherService voucherService,
                           WalletRepository walletRepository,
                           PasswordEncoder passwordEncoder,
                           NotificationRepository notificationRepository) {
        this.dashboardService = dashboardService;
        this.profileService = profileService;
        this.onboardingService = onboardingService;
        this.disputeService = disputeService;
        this.userService = userService;
        this.walletService = walletService;
        this.jobService = jobService;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.jobRepository = jobRepository;
        this.transactionRepository = transactionRepository;
        this.profileRepository = profileRepository;
        this.notificationService = notificationService;
        this.voucherService = voucherService;
        this.walletRepository = walletRepository;
        this.passwordEncoder = passwordEncoder;
        this.notificationRepository = notificationRepository;
    }

    private User getCurrentAdmin(Authentication auth) {
        return userService.getByEmail(auth.getName());
    }

    @GetMapping
    public String dashboard(@RequestParam(value = "tab", defaultValue = "overview") String tab,
                            Authentication auth,
                            Model model) {
        User admin = getCurrentAdmin(auth);
        Map<String, Object> stats = dashboardService.getAdminStatistics();

        List<ViekerProfile> allViekers = profileService.getAllProfiles();
        List<ViekerProfile> pendingViekers = allViekers.stream()
                .filter(p -> p.getVerificationStatus() == VerificationStatus.PENDING
                        || p.getVerificationStatus() == VerificationStatus.NEEDS_UPDATE)
                .toList();
        List<Dispute> openDisputes = disputeService.getDisputesByStatus(DisputeStatus.OPEN);
        List<Dispute> allDisputes = disputeService.getAllDisputes();
        List<JobCategory> categories = categoryRepository.findAll();
        List<User> users = userRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        long customerCount = userRepository.countByRole(Role.CUSTOMER);
        long viekerCount = userRepository.countByRole(Role.VIEKER);
        long adminCount = userRepository.countByRole(Role.ADMIN);
        List<WalletTransaction> transactions = walletService.getRecentTransactionsAll();
        List<Job> allJobs = jobRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        List<ViekerProfile> pendingCategoryProfiles = profileRepository.findByPendingCategoryRequestIsNotNullOrderByPendingCategoryRequestedAtDesc();
        List<vn.vieshift.entity.Voucher> vouchers = voucherService.getAllVouchers();

        model.addAttribute("admin", admin);
        model.addAttribute("stats", stats);
        model.addAttribute("pendingViekers", pendingViekers);
        model.addAttribute("pendingCategoryProfiles", pendingCategoryProfiles);
        model.addAttribute("allViekers", allViekers);
        model.addAttribute("kycDocuments", ViekerOnboardingService.DOCUMENTS);
        model.addAttribute("openDisputes", openDisputes);
        model.addAttribute("allDisputes", allDisputes);
        model.addAttribute("categories", categories);
        model.addAttribute("users", users);
        model.addAttribute("customerCount", customerCount);
        model.addAttribute("viekerCount", viekerCount);
        model.addAttribute("adminCount", adminCount);
        model.addAttribute("transactions", transactions);
        model.addAttribute("allJobs", allJobs);
        model.addAttribute("vouchers", vouchers);
        model.addAttribute("activeTab", tab);
        model.addAttribute("categoryRequest", new CategoryRequest());
        model.addAttribute("resolveDisputeRequest", new ResolveDisputeRequest());

        return "admin/dashboard";
    }

    @PostMapping("/viekers/{id}/approve")
    public String approveVieker(@PathVariable("id") Long profileId, RedirectAttributes redirectAttributes) {
        try {
            onboardingService.approve(profileId);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã phê duyệt hồ sơ đối tác VieKer và gửi email thông báo thành công!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + ex.getMessage());
        }
        return "redirect:/admin?tab=viekers";
    }

    @PostMapping("/viekers/{id}/request-update")
    public String requestUpdateVieker(@PathVariable("id") Long profileId,
                                      @RequestParam(value = "docs", required = false) List<String> docs,
                                      @RequestParam(value = "note", required = false) String note,
                                      RedirectAttributes redirectAttributes) {
        try {
            onboardingService.requestUpdate(profileId, docs, note);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã gửi yêu cầu bổ sung / chỉnh sửa giấy tờ qua email cho đối tác VieKer!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + ex.getMessage());
        }
        return "redirect:/admin?tab=viekers";
    }

    @PostMapping("/viekers/{id}/reject")
    public String rejectVieker(@PathVariable("id") Long profileId,
                               @RequestParam(value = "reason", required = false) String reason,
                               RedirectAttributes redirectAttributes) {
        try {
            onboardingService.reject(profileId, reason);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã từ chối hồ sơ đối tác VieKer và gửi email thông báo kết quả.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + ex.getMessage());
        }
        return "redirect:/admin?tab=viekers";
    }

    @PostMapping("/viekers/{id}/approve-category")
    public String approveCategory(@PathVariable("id") Long profileId, RedirectAttributes redirectAttributes) {
        try {
            ViekerProfile profile = profileRepository.findById(profileId)
                    .orElseThrow(() -> new vn.vieshift.exception.ResourceNotFoundException("Không tìm thấy hồ sơ Vieker"));
            String requestedCat = profile.getPendingCategoryRequest();
            if (requestedCat != null && !requestedCat.isBlank()) {
                if (profile.getAdditionalCategories() == null || profile.getAdditionalCategories().isBlank()) {
                    profile.setAdditionalCategories(requestedCat.trim());
                } else {
                    profile.setAdditionalCategories(profile.getAdditionalCategories() + ", " + requestedCat.trim());
                }
            }
            profile.setPendingCategoryRequest(null);
            profile.setPendingCategoryYears(null);
            profile.setPendingCategoryDesc(null);
            profile.setPendingCategoryCertPath(null);
            profile.setPendingCategoryRequestedAt(null);
            profileRepository.save(profile);

            try {
                notificationService.notifyUser(
                        profile.getUser(),
                        "Yêu cầu mở rộng chuyên môn được duyệt!",
                        "Chúc mừng! Ngành nghề '" + requestedCat + "' đã được Admin phê duyệt. Bạn có thể nhận công việc ngành này ngay.",
                        "/vieker?tab=marketplace",
                        NotificationType.SYSTEM
                );
            } catch (Exception ignored) {}

            redirectAttributes.addFlashAttribute("successMessage", "Đã phê duyệt chuyên môn '" + requestedCat + "' cho đối tác thành công!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + ex.getMessage());
        }
        return "redirect:/admin?tab=viekers";
    }

    @PostMapping("/viekers/{id}/reject-category")
    public String rejectCategory(@PathVariable("id") Long profileId,
                                 @RequestParam(value = "reason", defaultValue = "Giấy tờ hoặc kinh nghiệm chưa đạt chuẩn") String reason,
                                 RedirectAttributes redirectAttributes) {
        try {
            ViekerProfile profile = profileRepository.findById(profileId)
                    .orElseThrow(() -> new vn.vieshift.exception.ResourceNotFoundException("Không tìm thấy hồ sơ Vieker"));
            String requestedCat = profile.getPendingCategoryRequest();
            profile.setPendingCategoryRequest(null);
            profile.setPendingCategoryYears(null);
            profile.setPendingCategoryDesc(null);
            profile.setPendingCategoryCertPath(null);
            profile.setPendingCategoryRequestedAt(null);
            profileRepository.save(profile);

            try {
                notificationService.notifyUser(
                        profile.getUser(),
                        "Yêu cầu mở rộng chuyên môn chưa được duyệt",
                        "Yêu cầu bổ sung ngành '" + requestedCat + "' chưa được chấp thuận. Lý do: " + reason,
                        "/vieker?tab=profile",
                        NotificationType.SYSTEM
                );
            } catch (Exception ignored) {}

            redirectAttributes.addFlashAttribute("successMessage", "Đã từ chối yêu cầu mở rộng chuyên môn của đối tác.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + ex.getMessage());
        }
        return "redirect:/admin?tab=viekers";
    }

    @GetMapping("/viekers/{id}/doc/{key}")
    public ResponseEntity<Resource> viewViekerDocument(@PathVariable("id") Long profileId,
                                                       @PathVariable("key") String key) {
        try {
            Path path = onboardingService.resolveDocument(profileId, key);
            String contentType = Files.probeContentType(path);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + path.getFileName().toString() + "\"")
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(new FileSystemResource(path));
        } catch (Exception ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/disputes/resolve")
    public String resolveDispute(@Valid @ModelAttribute("resolveDisputeRequest") ResolveDisputeRequest request,
                                 BindingResult bindingResult,
                                 Authentication auth,
                                 RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng nhập lý do giải quyết của Ban Quản Trị");
            return "redirect:/admin?tab=disputes";
        }

        User admin = getCurrentAdmin(auth);
        try {
            disputeService.resolveDispute(admin.getId(), request);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xử lý khiếu nại và hoàn tất thanh toán thành công!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi xử lý: " + ex.getMessage());
        }
        return "redirect:/admin?tab=disputes";
    }

    @PostMapping("/categories/save")
    public String saveCategory(@Valid @ModelAttribute("categoryRequest") CategoryRequest request,
                               BindingResult bindingResult,
                               RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Dữ liệu danh mục không hợp lệ");
            return "redirect:/admin?tab=categories";
        }

        JobCategory category;
        if (request.getId() != null) {
            category = categoryRepository.findById(request.getId())
                    .orElse(new JobCategory());
        } else {
            category = new JobCategory();
        }

        category.setName(request.getName().trim());
        category.setDescription(request.getDescription() != null ? request.getDescription().trim() : "");
        category.setIcon(request.getIcon() != null && !request.getIcon().isBlank() ? request.getIcon().trim() : "bi-tools");
        category.setPricingMode(request.getPricingMode());
        category.setBaseRate(request.getBaseRate());
        category.setUnitName(request.getUnitName().trim());
        category.setActive(request.isActive());

        categoryRepository.save(category);
        redirectAttributes.addFlashAttribute("successMessage", "Đã lưu thông tin danh mục dịch vụ!");
        return "redirect:/admin?tab=categories";
    }

    @PostMapping("/categories/{id}/toggle")
    public String toggleCategoryStatus(@PathVariable("id") Long catId, RedirectAttributes redirectAttributes) {
        try {
            categoryRepository.findById(catId).ifPresent(c -> {
                c.setActive(!c.isActive());
                categoryRepository.save(c);
                String statusText = c.isActive() ? "Kích hoạt" : "Tạm ngưng";
                redirectAttributes.addFlashAttribute("successMessage", "Đã chuyển trạng thái ngành nghề '" + c.getName() + "' sang: " + statusText);
            });
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + ex.getMessage());
        }
        return "redirect:/admin?tab=categories";
    }

    @PostMapping("/categories/{id}/delete")
    public String deleteCategory(@PathVariable("id") Long catId, RedirectAttributes redirectAttributes) {
        try {
            long jobCount = jobRepository.countByCategoryId(catId);
            if (jobCount > 0) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Không thể xóa vì đã có " + jobCount + " đơn hàng liên kết với ngành nghề này. Bạn có thể chọn 'Tạm ngưng' để ẩn dịch vụ mà vẫn bảo toàn dữ liệu.");
                return "redirect:/admin?tab=categories";
            }
            categoryRepository.deleteById(catId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa ngành nghề thành công khỏi hệ thống!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa ngành nghề: " + ex.getMessage());
        }
        return "redirect:/admin?tab=categories";
    }

    @PostMapping("/users/{id}/toggle")
    public String toggleUserStatus(@PathVariable("id") Long userId, RedirectAttributes redirectAttributes) {
        try {
            userService.toggleUserStatus(userId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật trạng thái hoạt động của tài khoản.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + ex.getMessage());
        }
        return "redirect:/admin?tab=users";
    }

    @PostMapping("/users/{id}/edit")
    public String editUser(@PathVariable("id") Long userId,
                           @RequestParam("fullName") String fullName,
                           @RequestParam(value = "phone", required = false) String phone,
                           @RequestParam("email") String email,
                           @RequestParam(value = "newPassword", required = false) String newPassword,
                           RedirectAttributes redirectAttributes) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new vn.vieshift.exception.ResourceNotFoundException("Không tìm thấy người dùng"));
            String trimmedEmail = email.trim().toLowerCase();
            if (!trimmedEmail.equalsIgnoreCase(user.getEmail()) && userRepository.existsByEmail(trimmedEmail)) {
                redirectAttributes.addFlashAttribute("errorMessage", "Email '" + trimmedEmail + "' đã tồn tại trên hệ thống!");
                return "redirect:/admin?tab=users";
            }
            user.setFullName(fullName.trim());
            user.setPhone(phone != null && !phone.isBlank() ? phone.trim() : null);
            user.setEmail(trimmedEmail);
            if (newPassword != null && !newPassword.isBlank()) {
                user.setPassword(passwordEncoder.encode(newPassword.trim()));
            }
            userRepository.save(user);
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật thông tin thành viên '" + user.getFullName() + "' thành công!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi cập nhật người dùng: " + ex.getMessage());
        }
        return "redirect:/admin?tab=users";
    }

    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable("id") Long userId, RedirectAttributes redirectAttributes) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new vn.vieshift.exception.ResourceNotFoundException("Không tìm thấy người dùng"));
            if (user.getRole() == Role.ADMIN) {
                redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa tài khoản Quản trị viên hệ thống!");
                return "redirect:/admin?tab=users";
            }
            long jobCount = jobRepository.countByCustomerId(userId) + jobRepository.countByViekerId(userId);
            if (jobCount > 0) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Không thể xóa thành viên '" + user.getFullName() + "' vì đã có " + jobCount +
                        " đơn hàng liên quan trong hệ thống. Để bảo toàn chứng từ kế toán và lịch sử hoạt động, vui lòng chọn 'Khóa tài khoản'.");
                return "redirect:/admin?tab=users";
            }
            Wallet wallet = walletService.getWalletByUserId(userId);
            if (wallet != null) {
                List<WalletTransaction> txs = transactionRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getId());
                if (!txs.isEmpty()) {
                    redirectAttributes.addFlashAttribute("errorMessage",
                            "Không thể xóa thành viên '" + user.getFullName() + "' vì đã có lịch sử giao dịch ví. Vui lòng chọn 'Khóa tài khoản'.");
                    return "redirect:/admin?tab=users";
                }
                walletRepository.delete(wallet);
            }
            if (user.getRole() == Role.VIEKER) {
                profileRepository.findByUserId(userId).ifPresent(profileRepository::delete);
            }
            notificationRepository.deleteByUserId(userId);
            userRepository.delete(user);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa vĩnh viễn tài khoản '" + user.getFullName() + "' thành công!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi xóa tài khoản: " + ex.getMessage());
        }
        return "redirect:/admin?tab=users";
    }

    @GetMapping("/api/users/{id}/history")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getUserHistory(@PathVariable("id") Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        Map<String, Object> data = new HashMap<>();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        data.put("id", user.getId());
        data.put("fullName", user.getFullName());
        data.put("email", user.getEmail());
        data.put("phone", user.getPhone() != null ? user.getPhone() : "Chưa cập nhật");
        data.put("role", user.getRole().name());
        data.put("roleDisplayName", user.getRole().getDisplayName());
        data.put("active", user.isActive());
        data.put("createdAt", user.getCreatedAt() != null ? user.getCreatedAt().format(dtf) : "-");

        Wallet wallet = walletService.getWalletByUserId(userId);
        if (wallet != null) {
            data.put("walletBalance", wallet.getBalance());
            data.put("walletHeldAmount", wallet.getHeldAmount());
            List<WalletTransaction> allTx = transactionRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getId());
            List<Map<String, Object>> txList = new ArrayList<>();
            int limitTx = Math.min(allTx.size(), 8);
            for (int i = 0; i < limitTx; i++) {
                WalletTransaction tx = allTx.get(i);
                Map<String, Object> m = new HashMap<>();
                m.put("id", tx.getId());
                m.put("typeName", tx.getType().getDescription());
                m.put("typeSign", tx.getType().getSign());
                m.put("amount", tx.getAmount());
                m.put("balanceAfter", tx.getBalanceAfter());
                m.put("description", tx.getDescription());
                m.put("createdAt", tx.getCreatedAt().format(dtf));
                txList.add(m);
            }
            data.put("transactions", txList);
        } else {
            data.put("walletBalance", java.math.BigDecimal.ZERO);
            data.put("walletHeldAmount", java.math.BigDecimal.ZERO);
            data.put("transactions", List.of());
        }

        if (user.getRole() == Role.VIEKER) {
            profileRepository.findByUserId(userId).ifPresent(p -> {
                data.put("primaryCategory", p.getPrimaryCategory() != null ? p.getPrimaryCategory() : "Chưa thiết lập");
                data.put("experienceYears", p.getExperienceYears() != null ? p.getExperienceYears() : 0);
                data.put("ratingAvg", p.getRatingAvg() != null ? p.getRatingAvg() : 5.0);
                data.put("ratingCount", p.getRatingCount() != null ? p.getRatingCount() : 0);
                data.put("totalJobsCompleted", p.getTotalJobsCompleted() != null ? p.getTotalJobsCompleted() : 0);
                data.put("operatingZone", p.getOperatingZone() != null ? p.getOperatingZone() : "Toàn thành phố");
            });
            List<Job> viekerJobs = jobRepository.findByViekerIdOrderByCreatedAtDesc(userId);
            List<Map<String, Object>> jobsList = new ArrayList<>();
            int limitJobs = Math.min(viekerJobs.size(), 8);
            for (int i = 0; i < limitJobs; i++) {
                Job j = viekerJobs.get(i);
                Map<String, Object> jm = new HashMap<>();
                jm.put("id", j.getId());
                jm.put("title", j.getTitle());
                jm.put("categoryName", j.getCategory() != null ? j.getCategory().getName() : "");
                jm.put("price", j.getPrice());
                jm.put("statusDisplayName", j.getStatus().getDisplayName());
                jm.put("statusBadgeClass", j.getStatus().getBadgeClass());
                jm.put("createdAt", j.getCreatedAt().format(dtf));
                jm.put("roleInJob", "Đối tác nhận việc");
                jobsList.add(jm);
            }
            data.put("jobs", jobsList);
            data.put("totalJobs", viekerJobs.size());
        } else {
            List<Job> customerJobs = jobRepository.findByCustomerIdOrderByCreatedAtDesc(userId);
            List<Map<String, Object>> jobsList = new ArrayList<>();
            int limitJobs = Math.min(customerJobs.size(), 8);
            for (int i = 0; i < limitJobs; i++) {
                Job j = customerJobs.get(i);
                Map<String, Object> jm = new HashMap<>();
                jm.put("id", j.getId());
                jm.put("title", j.getTitle());
                jm.put("categoryName", j.getCategory() != null ? j.getCategory().getName() : "");
                jm.put("price", j.getPrice());
                jm.put("statusDisplayName", j.getStatus().getDisplayName());
                jm.put("statusBadgeClass", j.getStatus().getBadgeClass());
                jm.put("createdAt", j.getCreatedAt().format(dtf));
                jm.put("roleInJob", "Khách đặt việc");
                jobsList.add(jm);
            }
            data.put("jobs", jobsList);
            data.put("totalJobs", customerJobs.size());
        }

        return ResponseEntity.ok(data);
    }

    @PostMapping("/vouchers/create")
    public String createVoucher(@RequestParam("code") String code,
                                @RequestParam("title") String title,
                                @RequestParam(value = "description", required = false) String description,
                                @RequestParam("discountType") vn.vieshift.entity.DiscountType discountType,
                                @RequestParam("discountValue") java.math.BigDecimal discountValue,
                                @RequestParam(value = "maxDiscountAmount", required = false) java.math.BigDecimal maxDiscountAmount,
                                @RequestParam(value = "minOrderAmount", required = false) java.math.BigDecimal minOrderAmount,
                                @RequestParam(value = "maxUses", required = false) Integer maxUses,
                                @RequestParam(value = "endDate", required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime endDate,
                                @RequestParam(value = "isPublic", defaultValue = "false") boolean isPublic,
                                RedirectAttributes redirectAttributes) {
        try {
            vn.vieshift.entity.Voucher v = new vn.vieshift.entity.Voucher();
            v.setCode(code.trim().toUpperCase());
            v.setTitle(title.trim());
            v.setDescription(description);
            v.setDiscountType(discountType);
            v.setDiscountValue(discountValue);
            v.setMaxDiscountAmount(maxDiscountAmount);
            v.setMinOrderAmount(minOrderAmount != null ? minOrderAmount : java.math.BigDecimal.ZERO);
            v.setMaxUses(maxUses);
            v.setEndDate(endDate);
            v.setPublic(isPublic);
            voucherService.createVoucher(v);
            redirectAttributes.addFlashAttribute("successMessage", "Đã tạo thành công voucher ưu đãi " + v.getCode());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể tạo voucher: " + ex.getMessage());
        }
        return "redirect:/admin?tab=vouchers";
    }

    @PostMapping("/vouchers/{id}/toggle-public")
    public String toggleVoucherPublic(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            vn.vieshift.entity.Voucher v = voucherService.togglePublic(id);
            String status = v.isPublic() ? "Công khai (Khách hàng có thể chọn 1-chạm)" : "Ẩn (Khách hàng phải tự nhập mã)";
            redirectAttributes.addFlashAttribute("successMessage", "Đã chuyển chế độ hiển thị mã " + v.getCode() + " sang: " + status);
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + ex.getMessage());
        }
        return "redirect:/admin?tab=vouchers";
    }

    @PostMapping("/vouchers/{id}/toggle-active")
    public String toggleVoucherActive(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            vn.vieshift.entity.Voucher v = voucherService.toggleActive(id);
            String status = v.isActive() ? "Đang kích hoạt" : "Đã tạm dừng";
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật trạng thái mã " + v.getCode() + ": " + status);
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + ex.getMessage());
        }
        return "redirect:/admin?tab=vouchers";
    }

    @PostMapping("/vouchers/{id}/delete")
    public String deleteVoucher(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            voucherService.deleteVoucher(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa voucher thành công.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi xóa voucher: " + ex.getMessage());
        }
        return "redirect:/admin?tab=vouchers";
    }

    @GetMapping("/export/transactions")
    public void exportTransactions(jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        String filename = "vieshift_sao_ke_vi_" + java.time.LocalDate.now() + ".csv";
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

        java.io.PrintWriter writer = response.getWriter();
        // UTF-8 BOM for Microsoft Excel compatibility
        writer.write('\uFEFF');

        writer.println("\"Mã GD\",\"ID Ví\",\"Chủ Tài Khoản\",\"Email\",\"Loại Giao Dịch\",\"Số Tiền (đ)\",\"Số Dư Sau GD (đ)\",\"Thời Gian\",\"Nội Dung\"");

        List<vn.vieshift.entity.WalletTransaction> txs = transactionRepository.findAllByOrderByCreatedAtDesc();
        java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

        for (vn.vieshift.entity.WalletTransaction tx : txs) {
            String userName = tx.getWallet() != null && tx.getWallet().getUser() != null ? tx.getWallet().getUser().getFullName() : "";
            String userEmail = tx.getWallet() != null && tx.getWallet().getUser() != null ? tx.getWallet().getUser().getEmail() : "";
            String txType = tx.getType() != null ? tx.getType().getDisplayName() : "";
            String time = tx.getCreatedAt() != null ? tx.getCreatedAt().format(dtf) : "";
            String desc = tx.getDescription() != null ? tx.getDescription().replace("\"", "\"\"") : "";

            writer.println(String.format("\"#%d\",\"#%d\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"",
                    tx.getId(),
                    tx.getWallet() != null ? tx.getWallet().getId() : 0,
                    userName.replace("\"", "\"\""),
                    userEmail.replace("\"", "\"\""),
                    txType,
                    tx.getAmount(),
                    tx.getBalanceAfter(),
                    time,
                    desc
            ));
        }
        writer.flush();
    }

    @GetMapping("/export/jobs")
    public void exportJobs(jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        String filename = "vieshift_bao_cao_don_hang_" + java.time.LocalDate.now() + ".csv";
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

        java.io.PrintWriter writer = response.getWriter();
        // UTF-8 BOM for Microsoft Excel compatibility
        writer.write('\uFEFF');

        writer.println("\"Mã Đơn\",\"Tiêu Đề\",\"Ngành Nghề\",\"Khách Hàng\",\"SĐT Khách\",\"Thợ Nhận Việc\",\"SĐT Thợ\",\"Địa Chỉ\",\"Tổng Tiền (đ)\",\"Phí Sàn (đ)\",\"Thực Nhận Thợ (đ)\",\"Trạng Thái\",\"Thời Gian Tạo\",\"Thời Gian Nghiệm Thu\"");

        List<vn.vieshift.entity.Job> jobs = jobRepository.findAll();
        java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

        for (vn.vieshift.entity.Job j : jobs) {
            String custName = j.getCustomer() != null ? j.getCustomer().getFullName() : "";
            String custPhone = j.getCustomer() != null && j.getCustomer().getPhone() != null ? j.getCustomer().getPhone() : "";
            String viekerName = j.getVieker() != null ? j.getVieker().getFullName() : "Chưa có";
            String viekerPhone = j.getVieker() != null && j.getVieker().getPhone() != null ? j.getVieker().getPhone() : "-";
            String catName = j.getCategory() != null ? j.getCategory().getName() : "";
            String status = j.getStatus() != null ? j.getStatus().getDisplayName() : "";
            String created = j.getCreatedAt() != null ? j.getCreatedAt().format(dtf) : "";
            String completed = j.getCompletedAt() != null ? j.getCompletedAt().format(dtf) : "-";

            writer.println(String.format("\"#%d\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"",
                    j.getId(),
                    j.getTitle().replace("\"", "\"\""),
                    catName.replace("\"", "\"\""),
                    custName.replace("\"", "\"\""),
                    custPhone,
                    viekerName.replace("\"", "\"\""),
                    viekerPhone,
                    j.getAddress().replace("\"", "\"\""),
                    j.getPrice(),
                    j.getPlatformFee(),
                    j.getNetAmount(),
                    status,
                    created,
                    completed
            ));
        }
        writer.flush();
    }
}
