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
    private final vn.vieshift.service.ShopService shopService;
    private final vn.vieshift.service.MinigameService minigameService;
    private final vn.vieshift.service.DataIntegrityService dataIntegrityService;
    private final vn.vieshift.service.StaffPermissionService staffPermissionService;

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
                           NotificationRepository notificationRepository,
                           vn.vieshift.service.ShopService shopService,
                           vn.vieshift.service.MinigameService minigameService,
                           vn.vieshift.service.DataIntegrityService dataIntegrityService,
                           vn.vieshift.service.StaffPermissionService staffPermissionService) {
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
        this.shopService = shopService;
        this.minigameService = minigameService;
        this.dataIntegrityService = dataIntegrityService;
        this.staffPermissionService = staffPermissionService;
    }

    private User getCurrentAdmin(Authentication auth) {
        return userService.getByEmail(auth.getName());
    }

    private boolean checkCanCreate(User user, RedirectAttributes ra) {
        if (user.getRole() == Role.ADMIN) return true;
        StaffPermission p = staffPermissionService.getPermissionByUserId(user.getId());
        if (p == null || !p.isCanCreate()) {
            ra.addFlashAttribute("errorMessage", "Tài khoản Nhân viên không có quyền THÊM MỚI dữ liệu! Giới hạn bởi phân quyền.");
            return false;
        }
        return true;
    }

    private boolean checkCanEdit(User user, RedirectAttributes ra) {
        if (user.getRole() == Role.ADMIN) return true;
        StaffPermission p = staffPermissionService.getPermissionByUserId(user.getId());
        if (p == null || !p.isCanEdit()) {
            ra.addFlashAttribute("errorMessage", "Tài khoản Nhân viên không có quyền CHỈNH SỬA dữ liệu! Giới hạn bởi phân quyền.");
            return false;
        }
        return true;
    }

    private boolean checkCanDelete(User user, RedirectAttributes ra) {
        if (user.getRole() == Role.ADMIN) return true;
        StaffPermission p = staffPermissionService.getPermissionByUserId(user.getId());
        if (p == null || !p.isCanDelete()) {
            ra.addFlashAttribute("errorMessage", "Tài khoản Nhân viên không có quyền XÓA dữ liệu! Giới hạn bởi phân quyền của Quản trị viên.");
            return false;
        }
        return true;
    }

    @GetMapping
    public String dashboard(@RequestParam(value = "tab", defaultValue = "overview") String tab,
                            @RequestParam(value = "shopSubTab", defaultValue = "products") String shopSubTab,
                            @RequestParam(value = "err", required = false) String err,
                            Authentication auth,
                            Model model) {
        User admin = getCurrentAdmin(auth);
        boolean isSuperAdmin = (admin.getRole() == Role.ADMIN);
        StaffPermission staffPerm = isSuperAdmin ? null : staffPermissionService.getOrCreatePermission(admin);

        // Kiểm tra phân quyền truy cập tab cho Nhân viên (STAFF)
        if (!isSuperAdmin && staffPerm != null) {
            if ("staff".equals(tab)) {
                return "redirect:/admin?tab=overview&err=" + java.net.URLEncoder.encode("Chỉ Quản trị viên tối cao (Admin) mới có quyền truy cập mục Phân quyền nhân viên!", java.nio.charset.StandardCharsets.UTF_8);
            }
            if ("finance".equals(tab) && !staffPerm.isCanManageFinance()) {
                return "redirect:/admin?tab=overview&err=" + java.net.URLEncoder.encode("Tài khoản Nhân viên không có quyền truy cập Dòng tiền & Ví sàn!", java.nio.charset.StandardCharsets.UTF_8);
            }
            if ("users".equals(tab) && !staffPerm.isCanManageUsers()) {
                return "redirect:/admin?tab=overview&err=" + java.net.URLEncoder.encode("Tài khoản Nhân viên không có quyền truy cập module Người dùng!", java.nio.charset.StandardCharsets.UTF_8);
            }
            if ("categories".equals(tab) && !staffPerm.isCanManageServices()) {
                return "redirect:/admin?tab=overview&err=" + java.net.URLEncoder.encode("Tài khoản Nhân viên không có quyền truy cập Ngành nghề dịch vụ!", java.nio.charset.StandardCharsets.UTF_8);
            }
            if ("viekers".equals(tab) && !staffPerm.isCanApproveVieker()) {
                return "redirect:/admin?tab=overview&err=" + java.net.URLEncoder.encode("Tài khoản Nhân viên không có quyền duyệt hồ sơ Đối tác VieKer!", java.nio.charset.StandardCharsets.UTF_8);
            }
            if ("disputes".equals(tab) && !staffPerm.isCanResolveDispute()) {
                return "redirect:/admin?tab=overview&err=" + java.net.URLEncoder.encode("Tài khoản Nhân viên không có quyền Xử lý khiếu nại!", java.nio.charset.StandardCharsets.UTF_8);
            }
            if ("shop".equals(tab) && !staffPerm.isCanManageShop()) {
                return "redirect:/admin?tab=overview&err=" + java.net.URLEncoder.encode("Tài khoản Nhân viên không có quyền quản lý VieShift Mall & Kho hàng!", java.nio.charset.StandardCharsets.UTF_8);
            }
            if ("minigame".equals(tab) && !staffPerm.isCanManageMinigame()) {
                return "redirect:/admin?tab=overview&err=" + java.net.URLEncoder.encode("Tài khoản Nhân viên không có quyền quản lý Vòng quay & Voucher!", java.nio.charset.StandardCharsets.UTF_8);
            }
        }

        if (err != null && !err.isBlank()) {
            model.addAttribute("errorMessage", err);
        }

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
        long staffCount = userRepository.countByRole(Role.STAFF);
        List<WalletTransaction> transactions = walletService.getRecentTransactionsAll();
        List<Job> allJobs = jobRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        List<ViekerProfile> pendingCategoryProfiles = profileRepository.findByPendingCategoryRequestIsNotNullOrderByPendingCategoryRequestedAtDesc();
        List<vn.vieshift.entity.Voucher> vouchers = voucherService.getAllVouchers();

        List<User> staffUsers = staffPermissionService.getAllStaffUsers();
        Map<Long, StaffPermission> staffPermMap = new HashMap<>();
        for (User su : staffUsers) {
            staffPermMap.put(su.getId(), staffPermissionService.getPermissionByUserId(su.getId()));
        }

        model.addAttribute("admin", admin);
        model.addAttribute("isSuperAdmin", isSuperAdmin);
        model.addAttribute("staffPermission", staffPerm);
        model.addAttribute("staffUsers", staffUsers);
        model.addAttribute("staffPermMap", staffPermMap);
        model.addAttribute("staffCount", staffCount);
        model.addAttribute("shopSubTab", shopSubTab);

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
        model.addAttribute("shopProducts", shopService.getAllProducts());
        model.addAttribute("trashedProducts", shopService.getTrashedProducts());
        model.addAttribute("stockReceipts", shopService.getAllStockReceipts());
        model.addAttribute("productReviews", shopService.getAllProductReviews());
        model.addAttribute("shopOrders", shopService.getAllOrders());
        model.addAttribute("shopRevenue", shopService.getTotalShopRevenue());
        model.addAttribute("minigameSlices", minigameService.getAllSlices());
        model.addAttribute("minigameGiftcodes", minigameService.getAllGiftcodes());
        model.addAttribute("minigameSpinLogs", minigameService.getRecentSpinLogs());
        model.addAttribute("integrityReport", dataIntegrityService.scanDatabaseIntegrity());
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
    public String deleteCategory(@PathVariable("id") Long catId, Authentication auth, RedirectAttributes redirectAttributes) {
        if (!checkCanDelete(getCurrentAdmin(auth), redirectAttributes)) {
            return "redirect:/admin?tab=categories";
        }
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
    public String deleteUser(@PathVariable("id") Long userId, Authentication auth, RedirectAttributes redirectAttributes) {
        if (!checkCanDelete(getCurrentAdmin(auth), redirectAttributes)) {
            return "redirect:/admin?tab=users";
        }
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
    public String deleteVoucher(@PathVariable("id") Long id, Authentication auth, RedirectAttributes redirectAttributes) {
        if (!checkCanDelete(getCurrentAdmin(auth), redirectAttributes)) {
            return "redirect:/admin?tab=vouchers";
        }
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

    @PostMapping("/shop/products/create")
    public String createShopProduct(@RequestParam("name") String name,
                                    @RequestParam("category") String category,
                                    @RequestParam(value = "description", required = false) String description,
                                    @RequestParam("price") java.math.BigDecimal price,
                                    @RequestParam(value = "originalPrice", required = false) java.math.BigDecimal originalPrice,
                                    @RequestParam(value = "stockQuantity", defaultValue = "50") Integer stockQuantity,
                                    @RequestParam(value = "unit", defaultValue = "Bộ") String unit,
                                    @RequestParam(value = "imageUrl", required = false) String imageUrl,
                                    @RequestParam(value = "relatedServiceCategory", required = false) String relatedServiceCategory,
                                    RedirectAttributes redirectAttributes) {
        try {
            Product p = shopService.createProduct(name, category, description, price, originalPrice, stockQuantity, unit, imageUrl, relatedServiceCategory);
            redirectAttributes.addFlashAttribute("successMessage", "Đã thêm sản phẩm '" + p.getName() + "' vào VieShift Mall thành công!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi thêm sản phẩm: " + ex.getMessage());
        }
        return "redirect:/admin?tab=shop";
    }

    @PostMapping("/shop/products/{id}/toggle")
    public String toggleShopProduct(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            Product p = shopService.toggleProductActive(id);
            String st = p.isActive() ? "Đang kinh doanh" : "Tạm ẩn";
            redirectAttributes.addFlashAttribute("successMessage", "Đã chuyển trạng thái sản phẩm '" + p.getName() + "' sang: " + st);
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + ex.getMessage());
        }
        return "redirect:/admin?tab=shop";
    }

    @PostMapping("/shop/products/{id}/update")
    public String updateShopProduct(@PathVariable("id") Long id,
                                    @RequestParam("name") String name,
                                    @RequestParam("category") String category,
                                    @RequestParam(value = "description", required = false) String description,
                                    @RequestParam("price") java.math.BigDecimal price,
                                    @RequestParam(value = "originalPrice", required = false) java.math.BigDecimal originalPrice,
                                    @RequestParam(value = "stockQuantity", defaultValue = "50") Integer stockQuantity,
                                    @RequestParam(value = "unit", defaultValue = "Bộ") String unit,
                                    @RequestParam(value = "imageUrl", required = false) String imageUrl,
                                    @RequestParam(value = "relatedServiceCategory", required = false) String relatedServiceCategory,
                                    RedirectAttributes redirectAttributes) {
        try {
            Product p = shopService.updateProduct(id, name, category, description, price, originalPrice, stockQuantity, unit, imageUrl, relatedServiceCategory);
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật thông tin sản phẩm #" + p.getId() + " (" + p.getName() + ") thành công!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi cập nhật sản phẩm: " + ex.getMessage());
        }
        return "redirect:/admin?tab=shop";
    }

    @PostMapping("/shop/products/{id}/delete")
    public String deleteShopProduct(@PathVariable("id") Long id, Authentication auth, RedirectAttributes redirectAttributes) {
        if (!checkCanDelete(getCurrentAdmin(auth), redirectAttributes)) {
            return "redirect:/admin?tab=shop";
        }
        try {
            shopService.deleteProduct(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa sản phẩm #" + id + " khỏi cửa hàng thành công!");
        } catch (IllegalStateException ise) {
            redirectAttributes.addFlashAttribute("warningMessage", ise.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi xóa sản phẩm: " + ex.getMessage());
        }
        return "redirect:/admin?tab=shop";
    }

    @GetMapping(value = "/api/shop/products/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> getShopProductDetails(@PathVariable("id") Long id) {
        try {
            Product p = shopService.getProductById(id);
            Map<String, Object> data = new HashMap<>();
            data.put("id", p.getId());
            data.put("name", p.getName());
            data.put("category", p.getCategory());
            data.put("description", p.getDescription());
            data.put("price", p.getPrice());
            data.put("originalPrice", p.getOriginalPrice());
            data.put("stockQuantity", p.getStockQuantity());
            data.put("unit", p.getUnit());
            data.put("imageUrl", p.getImageUrl());
            data.put("relatedServiceCategory", p.getRelatedServiceCategory());
            data.put("soldCount", p.getSoldCount());
            data.put("active", p.isActive());
            return ResponseEntity.ok(data);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping(value = "/api/shop/orders/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> getShopOrderDetails(@PathVariable("id") Long id) {
        try {
            ShopOrder o = shopService.getOrderById(id);
            Map<String, Object> data = new HashMap<>();
            data.put("id", o.getId());
            data.put("orderCode", "VS-MALL-" + o.getId());
            data.put("customerName", o.getRecipientName() != null ? o.getRecipientName() : (o.getCustomer() != null ? o.getCustomer().getFullName() : "N/A"));
            data.put("customerPhone", o.getRecipientPhone() != null ? o.getRecipientPhone() : (o.getCustomer() != null ? o.getCustomer().getPhone() : "N/A"));
            data.put("shippingAddress", o.getShippingAddress());
            data.put("itemsSummary", o.getItemsSummary());
            data.put("subtotalAmount", o.getSubtotalAmount() != null ? o.getSubtotalAmount() : o.getTotalAmount());
            data.put("discountAmount", o.getDiscountAmount() != null ? o.getDiscountAmount() : java.math.BigDecimal.ZERO);
            data.put("shippingFee", o.getShippingFee() != null ? o.getShippingFee() : java.math.BigDecimal.ZERO);
            data.put("shippingMethodName", o.getShippingMethodName() != null ? o.getShippingMethodName() : "Giao hàng tiêu chuẩn");
            data.put("voucherCode", o.getVoucherCode());
            data.put("paymentMethod", o.getPaymentMethodDisplayName());
            data.put("installationMode", o.getInstallationMode());
            data.put("totalAmount", o.getTotalAmount());
            data.put("totalItems", o.getTotalItems());
            data.put("orderType", o.getOrderType());
            data.put("status", o.getStatus());
            data.put("statusDisplayName", o.getStatusDisplayName());
            data.put("notes", o.getNotes());
            data.put("createdAt", o.getCreatedAt() != null ? o.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) : "");
            data.put("relatedJobId", o.getRelatedJob() != null ? o.getRelatedJob().getId() : null);
            return ResponseEntity.ok(data);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/shop/orders/{id}/status")
    public String updateShopOrderStatus(@PathVariable("id") Long id,
                                        @RequestParam("status") String status,
                                        RedirectAttributes redirectAttributes) {
        try {
            ShopOrder order = shopService.updateOrderStatus(id, status);
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật trạng thái đơn vật tư #VS-MALL-" + order.getId() + " sang: " + order.getStatusDisplayName());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi cập nhật đơn hàng: " + ex.getMessage());
        }
        return "redirect:/admin?tab=shop";
    }

    @PostMapping("/shop/stock-receipts/create")
    public String createStockReceipt(@RequestParam("supplierName") String supplierName,
                                     @RequestParam("productId") Long productId,
                                     @RequestParam(value = "importQuantity", defaultValue = "10") int importQuantity,
                                     @RequestParam(value = "importUnitPrice", defaultValue = "50000") java.math.BigDecimal importUnitPrice,
                                     @RequestParam(value = "paidAmount", defaultValue = "0") java.math.BigDecimal paidAmount,
                                     @RequestParam(value = "notes", required = false) String notes,
                                     Authentication auth,
                                     RedirectAttributes redirectAttributes) {
        try {
            User admin = getCurrentAdmin(auth);
            StockReceipt receipt = shopService.createStockReceipt(
                    productId, supplierName, importQuantity, importUnitPrice, paidAmount, notes,
                    admin != null ? admin.getFullName() : "Admin"
            );
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã lập phiếu nhập kho " + receipt.getReceiptCode() + " (+ " + importQuantity + " " + receipt.getProductName() + ") và cập nhật tồn kho thành công!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi lập phiếu nhập kho: " + ex.getMessage());
        }
        return "redirect:/admin?tab=shop";
    }

    @PostMapping("/shop/stock-receipts/{id}/pay-debt")
    public String payStockReceiptDebt(@PathVariable("id") Long id,
                                      @RequestParam("payAmount") java.math.BigDecimal payAmount,
                                      RedirectAttributes redirectAttributes) {
        try {
            StockReceipt receipt = shopService.paySupplierDebt(id, payAmount);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã thanh toán công nợ cho phiếu " + receipt.getReceiptCode() + ". Trạng thái hiện tại: " + receipt.getPaymentStatusDisplayName());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi thanh toán công nợ: " + ex.getMessage());
        }
        return "redirect:/admin?tab=shop";
    }

    @PostMapping("/shop/trash/{id}/restore")
    public String restoreTrashedProduct(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            Product p = shopService.restoreProduct(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã khôi phục sản phẩm '" + p.getName() + "' từ Thùng Rác trở lại kệ hàng thành công!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khôi phục sản phẩm: " + ex.getMessage());
        }
        return "redirect:/admin?tab=shop";
    }

    @PostMapping("/shop/trash/{id}/force-delete")
    public String forceDeleteTrashedProduct(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            shopService.forceDeleteProduct(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa vĩnh viễn sản phẩm #" + id + " khỏi hệ thống!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi xóa vĩnh viễn: " + ex.getMessage());
        }
        return "redirect:/admin?tab=shop";
    }

    @PostMapping("/shop/reviews/{id}/reply")
    public String replyProductReview(@PathVariable("id") Long id,
                                     @RequestParam(value = "adminReply", required = false) String adminReply,
                                     @RequestParam(value = "visible", defaultValue = "true") Boolean visible,
                                     RedirectAttributes redirectAttributes) {
        try {
            shopService.replyProductReview(id, adminReply, visible);
            redirectAttributes.addFlashAttribute("successMessage", "Đã lưu phản hồi của Cửa hàng cho đánh giá #" + id + " thành công!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi phản hồi đánh giá: " + ex.getMessage());
        }
        return "redirect:/admin?tab=shop";
    }

    @PostMapping("/shop/reviews/{id}/delete")
    public String deleteProductReview(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            shopService.deleteProductReview(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa đánh giá #" + id + " thành công!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi xóa đánh giá: " + ex.getMessage());
        }
        return "redirect:/admin?tab=shop";
    }

    @PostMapping("/minigame/slices/{id}/update")
    public String updateMinigameSlice(@PathVariable("id") Long id,
                                      @RequestParam("label") String label,
                                      @RequestParam("winRate") Integer winRate,
                                      @RequestParam(value = "couponCode", required = false) String couponCode,
                                      @RequestParam(value = "rewardValue", required = false) java.math.BigDecimal rewardValue,
                                      RedirectAttributes redirectAttributes) {
        try {
            MinigameSlice s = minigameService.updateSlice(id, label, winRate, couponCode, rewardValue);
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật ô giải thưởng '" + s.getLabel() + "' (Tỷ lệ: " + s.getWinRate() + "%) thành công!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi cập nhật ô vòng quay: " + ex.getMessage());
        }
        return "redirect:/admin?tab=minigame";
    }

    @PostMapping("/minigame/giftcodes/create")
    public String createMinigameGiftcode(@RequestParam("code") String code,
                                         @RequestParam(value = "spinCount", defaultValue = "1") int spinCount,
                                         @RequestParam(value = "maxUses", defaultValue = "500") int maxUses,
                                         RedirectAttributes redirectAttributes) {
        try {
            MinigameGiftcode g = minigameService.createGiftcode(code, spinCount, maxUses);
            redirectAttributes.addFlashAttribute("successMessage", "Đã lưu mã sự kiện Giftcode '" + g.getCode() + "' (+ " + g.getSpinCount() + " lượt quay) thành công!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi tạo Giftcode: " + ex.getMessage());
        }
        return "redirect:/admin?tab=minigame";
    }

    @PostMapping("/minigame/giftcodes/{id}/delete")
    public String deleteMinigameGiftcode(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            minigameService.deleteGiftcode(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa mã Giftcode thành công!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi xóa Giftcode: " + ex.getMessage());
        }
        return "redirect:/admin?tab=minigame";
    }

    @PostMapping("/staff/create")
    public String createStaffAccount(@RequestParam("email") String email,
                                     @RequestParam("password") String password,
                                     @RequestParam("fullName") String fullName,
                                     @RequestParam(value = "phone", required = false) String phone,
                                     @RequestParam(value = "department", defaultValue = "Ban Vận Hành & CSKH") String department,
                                     @RequestParam(value = "canCreate", defaultValue = "false") boolean canCreate,
                                     @RequestParam(value = "canEdit", defaultValue = "false") boolean canEdit,
                                     @RequestParam(value = "canDelete", defaultValue = "false") boolean canDelete,
                                     @RequestParam(value = "canUsers", defaultValue = "false") boolean canUsers,
                                     @RequestParam(value = "canServices", defaultValue = "false") boolean canServices,
                                     @RequestParam(value = "canVieker", defaultValue = "false") boolean canVieker,
                                     @RequestParam(value = "canDispute", defaultValue = "false") boolean canDispute,
                                     @RequestParam(value = "canShop", defaultValue = "false") boolean canShop,
                                     @RequestParam(value = "canFinance", defaultValue = "false") boolean canFinance,
                                     @RequestParam(value = "canMinigame", defaultValue = "false") boolean canMinigame,
                                     Authentication auth,
                                     RedirectAttributes ra) {
        User currentUser = getCurrentAdmin(auth);
        if (currentUser.getRole() != Role.ADMIN) {
            ra.addFlashAttribute("errorMessage", "Chỉ Quản trị viên tối cao mới được tạo tài khoản nhân viên!");
            return "redirect:/admin?tab=overview";
        }
        try {
            staffPermissionService.createStaff(email, password, fullName, phone, department,
                    canCreate, canEdit, canDelete, canUsers, canServices, canVieker,
                    canDispute, canShop, canFinance, canMinigame);
            ra.addFlashAttribute("successMessage", "Đã tạo tài khoản nhân viên '" + email + "' và thiết lập phân quyền thành công!");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", "Lỗi tạo nhân viên: " + ex.getMessage());
        }
        return "redirect:/admin?tab=staff";
    }

    @PostMapping("/staff/{id}/permissions")
    public String updateStaffPermissions(@PathVariable("id") Long staffUserId,
                                         @RequestParam(value = "department", required = false) String department,
                                         @RequestParam(value = "canCreate", defaultValue = "false") boolean canCreate,
                                         @RequestParam(value = "canEdit", defaultValue = "false") boolean canEdit,
                                         @RequestParam(value = "canDelete", defaultValue = "false") boolean canDelete,
                                         @RequestParam(value = "canUsers", defaultValue = "false") boolean canUsers,
                                         @RequestParam(value = "canServices", defaultValue = "false") boolean canServices,
                                         @RequestParam(value = "canVieker", defaultValue = "false") boolean canVieker,
                                         @RequestParam(value = "canDispute", defaultValue = "false") boolean canDispute,
                                         @RequestParam(value = "canShop", defaultValue = "false") boolean canShop,
                                         @RequestParam(value = "canFinance", defaultValue = "false") boolean canFinance,
                                         @RequestParam(value = "canMinigame", defaultValue = "false") boolean canMinigame,
                                         Authentication auth,
                                         RedirectAttributes ra) {
        User currentUser = getCurrentAdmin(auth);
        if (currentUser.getRole() != Role.ADMIN) {
            ra.addFlashAttribute("errorMessage", "Chỉ Quản trị viên tối cao mới được chỉnh sửa phân quyền nhân viên!");
            return "redirect:/admin?tab=overview";
        }
        try {
            staffPermissionService.updatePermission(staffUserId, department,
                    canCreate, canEdit, canDelete, canUsers, canServices, canVieker,
                    canDispute, canShop, canFinance, canMinigame);
            ra.addFlashAttribute("successMessage", "Đã cập nhật ma trận phân quyền cho nhân viên thành công!");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", "Lỗi cập nhật phân quyền: " + ex.getMessage());
        }
        return "redirect:/admin?tab=staff";
    }
}
