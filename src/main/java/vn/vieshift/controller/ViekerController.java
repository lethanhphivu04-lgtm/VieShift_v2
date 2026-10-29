package vn.vieshift.controller;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.vieshift.dto.DisputeRequest;
import vn.vieshift.dto.ViekerProfileUpdateRequest;
import vn.vieshift.dto.WithdrawRequest;
import vn.vieshift.entity.*;
import vn.vieshift.exception.ProfileNotQualifiedException;
import vn.vieshift.repository.JobCategoryRepository;
import vn.vieshift.repository.ReviewRepository;
import vn.vieshift.repository.ViekerProfileRepository;
import vn.vieshift.service.DisputeService;
import vn.vieshift.service.JobService;
import vn.vieshift.service.UserService;
import vn.vieshift.service.ViekerOnboardingService;
import vn.vieshift.service.ViekerProfileService;
import vn.vieshift.service.WalletService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.UUID;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/vieker")
public class ViekerController {

    private final UserService userService;
    private final ViekerProfileService profileService;
    private final ViekerOnboardingService onboardingService;
    private final WalletService walletService;
    private final JobService jobService;
    private final DisputeService disputeService;
    private final ReviewRepository reviewRepository;
    private final JobCategoryRepository categoryRepository;
    private final ViekerProfileRepository profileRepository;

    public ViekerController(UserService userService,
                            ViekerProfileService profileService,
                            ViekerOnboardingService onboardingService,
                            WalletService walletService,
                            JobService jobService,
                            DisputeService disputeService,
                            ReviewRepository reviewRepository,
                            JobCategoryRepository categoryRepository,
                            ViekerProfileRepository profileRepository) {
        this.userService = userService;
        this.profileService = profileService;
        this.onboardingService = onboardingService;
        this.walletService = walletService;
        this.jobService = jobService;
        this.disputeService = disputeService;
        this.reviewRepository = reviewRepository;
        this.categoryRepository = categoryRepository;
        this.profileRepository = profileRepository;
    }

    private User getCurrentVieker(Authentication auth) {
        return userService.getByEmail(auth.getName());
    }

    @GetMapping
    public String dashboard(@RequestParam(value = "tab", defaultValue = "overview") String tab,
                            @RequestParam(value = "catId", required = false) Long catId,
                            Authentication auth,
                            Model model) {
        User vieker = getCurrentVieker(auth);
        ViekerProfile profile = profileService.getProfileByUserId(vieker.getId());
        String onboardingToken = onboardingService.ensureToken(profile);
        if (profile.getVerificationStatus() == VerificationStatus.DRAFT
                || profile.getVerificationStatus() == VerificationStatus.NEEDS_UPDATE) {
            return "redirect:/onboarding/" + onboardingToken;
        }
        Wallet wallet = walletService.getOrCreateWallet(vieker);

        List<JobCategory> categories = categoryRepository.findByActiveTrueOrderByNameAsc();
        List<Job> availableJobs = jobService.getAvailableJobsForMarketplace(catId);
        List<Job> myJobs = jobService.getJobsByVieker(vieker.getId());
        List<Review> myReviews = reviewRepository.findByViekerIdOrderByCreatedAtDesc(vieker.getId());
        List<WalletTransaction> transactions = walletService.getTransactionHistory(vieker.getId());

        List<Job> activeJobs = myJobs.stream()
                .filter(j -> j.getStatus() == JobStatus.ACCEPTED
                        || j.getStatus() == JobStatus.IN_PROGRESS
                        || j.getStatus() == JobStatus.PENDING_REVIEW
                        || j.getStatus() == JobStatus.DISPUTED)
                .toList();

        ViekerProfileUpdateRequest profileUpdateRequest = new ViekerProfileUpdateRequest();
        profileUpdateRequest.setIdCardNumber(profile.getIdCardNumber());
        profileUpdateRequest.setAvatarUrl(profile.getAvatarUrl());
        profileUpdateRequest.setSkillsDescription(profile.getSkillsDescription());
        profileUpdateRequest.setExperienceYears(profile.getExperienceYears());
        profileUpdateRequest.setOperatingZone(profile.getOperatingZone());

        model.addAttribute("vieker", vieker);
        model.addAttribute("profile", profile);
        model.addAttribute("onboardingToken", onboardingToken);
        model.addAttribute("wallet", wallet);
        model.addAttribute("categories", categories);
        model.addAttribute("availableJobs", availableJobs);
        model.addAttribute("myJobs", myJobs);
        model.addAttribute("activeJobs", activeJobs);
        model.addAttribute("myReviews", myReviews);
        model.addAttribute("transactions", transactions);
        model.addAttribute("activeTab", tab);
        model.addAttribute("selectedCatId", catId);
        model.addAttribute("profileUpdateRequest", profileUpdateRequest);
        model.addAttribute("withdrawRequest", new WithdrawRequest());

        return "vieker/dashboard";
    }

    @PostMapping("/jobs/{id}/accept")
    public String handleAcceptJob(@PathVariable("id") Long jobId,
                                  Authentication auth,
                                  RedirectAttributes redirectAttributes) {
        User vieker = getCurrentVieker(auth);
        try {
            jobService.acceptJob(vieker.getId(), jobId);
            redirectAttributes.addFlashAttribute("successMessage", "Chúc mừng! Bạn đã nhận công việc thành công. Hãy liên hệ với khách hàng và chuẩn bị tới điểm làm.");
            return "redirect:/vieker?tab=my-jobs";
        } catch (ProfileNotQualifiedException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/vieker?tab=profile";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể nhận việc: " + ex.getMessage());
            return "redirect:/vieker?tab=marketplace";
        }
    }

    @PostMapping("/jobs/{id}/start")
    public String handleStartJob(@PathVariable("id") Long jobId,
                                 Authentication auth,
                                 RedirectAttributes redirectAttributes) {
        User vieker = getCurrentVieker(auth);
        try {
            jobService.startJob(vieker.getId(), jobId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật trạng thái: Bạn đang thực hiện công việc.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + ex.getMessage());
        }
        return "redirect:/vieker?tab=my-jobs";
    }

    @PostMapping("/jobs/{id}/complete")
    public String handleCompleteJob(@PathVariable("id") Long jobId,
                                    Authentication auth,
                                    RedirectAttributes redirectAttributes) {
        User vieker = getCurrentVieker(auth);
        try {
            jobService.reportCompleteJob(vieker.getId(), jobId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã gửi thông báo hoàn thành đến khách hàng. Thu nhập sẽ tự động được cộng vào ví của bạn ngay khi khách nghiệm thu!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + ex.getMessage());
        }
        return "redirect:/vieker?tab=my-jobs";
    }

    @PostMapping("/jobs/{id}/dispute")
    public String handleDisputeJob(@PathVariable("id") Long jobId,
                                   @Valid @ModelAttribute("disputeRequest") DisputeRequest request,
                                   BindingResult bindingResult,
                                   Authentication auth,
                                   RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng nhập đầy đủ lý do khiếu nại");
            return "redirect:/vieker?tab=my-jobs";
        }

        User vieker = getCurrentVieker(auth);
        request.setJobId(jobId);
        try {
            disputeService.openDispute(vieker.getId(), request);
            redirectAttributes.addFlashAttribute("successMessage", "Đã gửi yêu cầu khiếu nại lên Ban Quản Trị để được hỗ trợ xử lý quyền lợi cho bạn.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + ex.getMessage());
        }
        return "redirect:/vieker?tab=my-jobs";
    }

    @PostMapping("/profile/update")
    public String handleUpdateProfile(@Valid @ModelAttribute("profileUpdateRequest") ViekerProfileUpdateRequest request,
                                      BindingResult bindingResult,
                                      Authentication auth,
                                      RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Thông tin hồ sơ chưa hợp lệ, vui lòng điền đầy đủ");
            return "redirect:/vieker?tab=profile";
        }

        User vieker = getCurrentVieker(auth);
        profileService.updateProfile(vieker.getId(), request);
        redirectAttributes.addFlashAttribute("successMessage", "Hồ sơ đã được gửi! Ban Quản Trị sẽ kiểm tra và phê duyệt trong thời gian sớm nhất.");
        return "redirect:/vieker?tab=profile";
    }

    @PostMapping("/wallet/withdraw")
    public String handleWithdraw(@Valid @ModelAttribute("withdrawRequest") WithdrawRequest request,
                                 BindingResult bindingResult,
                                 Authentication auth,
                                 RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng nhập đầy đủ thông tin tài khoản ngân hàng để rút tiền");
            return "redirect:/vieker?tab=wallet";
        }

        User vieker = getCurrentVieker(auth);
        try {
            walletService.withdraw(vieker.getId(), request);
            redirectAttributes.addFlashAttribute("successMessage", "Lệnh rút tiền thành công! Hệ thống đang chuyển tiền vào tài khoản ngân hàng của bạn.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/vieker?tab=wallet";
    }

    @GetMapping("/jobs/{id}/income-detail")
    @ResponseBody
    public ResponseEntity<?> getJobIncomeDetail(@PathVariable("id") Long jobId, Authentication auth) {
        User vieker = getCurrentVieker(auth);
        Job job = jobService.getJobById(jobId);
        if (job.getVieker() == null || !job.getVieker().getId().equals(vieker.getId())) {
            return ResponseEntity.status(403).body(Map.of("error", "Bạn không có quyền xem thông tin thu nhập của đơn hàng này"));
        }

        BigDecimal tipAmount = BigDecimal.ZERO;
        BigDecimal allowance = BigDecimal.ZERO;
        List<WalletTransaction> txs = walletService.getTransactionHistory(vieker.getId());
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        List<Map<String, Object>> relatedTxs = new ArrayList<>();

        for (WalletTransaction tx : txs) {
            if (tx.getJob() != null && tx.getJob().getId().equals(job.getId())) {
                Map<String, Object> t = new HashMap<>();
                t.put("id", tx.getId());
                t.put("type", tx.getType().getDescription());
                t.put("amount", tx.getAmount());
                t.put("sign", tx.getType().getSign());
                t.put("createdAt", tx.getCreatedAt() != null ? tx.getCreatedAt().format(dtf) : "");
                t.put("balanceAfter", tx.getBalanceAfter());
                relatedTxs.add(t);

                if (tx.getType() == TransactionType.TIP_RECEIVED) {
                    tipAmount = tipAmount.add(tx.getAmount());
                }
            }
        }

        BigDecimal totalEarnings = job.getNetAmount().add(tipAmount).add(allowance);

        Map<String, Object> data = new HashMap<>();
        data.put("jobId", job.getId());
        data.put("title", job.getTitle());
        data.put("customerName", job.getCustomer().getFullName());
        data.put("customerPhone", job.getCustomer().getPhone());
        data.put("address", job.getAddress());
        data.put("status", job.getStatus().name());
        data.put("statusDisplayName", job.getStatus().getDisplayName());
        data.put("price", job.getPrice());
        data.put("platformFee", job.getPlatformFee());
        data.put("netAmount", job.getNetAmount());
        data.put("tipAmount", tipAmount);
        data.put("allowance", allowance);
        data.put("totalEarnings", totalEarnings);
        data.put("transactions", relatedTxs);

        return ResponseEntity.ok(data);
    }

    @PostMapping("/profile/request-category")
    public String handleRequestCategory(@RequestParam("categoryName") String categoryName,
                                        @RequestParam(value = "experienceYears", defaultValue = "1") Integer experienceYears,
                                        @RequestParam(value = "skillsDescription", required = false) String skillsDescription,
                                        @RequestParam(value = "certificate", required = false) MultipartFile certificate,
                                        Authentication auth,
                                        RedirectAttributes redirectAttributes) {
        User vieker = getCurrentVieker(auth);
        ViekerProfile profile = profileService.getProfileByUserId(vieker.getId());

        if (profile.canPerformCategory(categoryName)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn đã được cấp phép hoạt động ngành nghề '" + categoryName + "' rồi!");
            return "redirect:/vieker?tab=profile";
        }

        String certPath = null;
        if (certificate != null && !certificate.isEmpty()) {
            try {
                Path certDir = Paths.get("uploads/certificates").toAbsolutePath().normalize();
                if (!Files.exists(certDir)) {
                    Files.createDirectories(certDir);
                }
                String cleanName = StringUtils.cleanPath(certificate.getOriginalFilename() != null ? certificate.getOriginalFilename() : "cert.jpg");
                String storedName = "cert_" + vieker.getId() + "_" + UUID.randomUUID().toString().substring(0, 8) + "_" + cleanName;
                Path target = certDir.resolve(storedName).normalize();
                Files.copy(certificate.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
                certPath = "/uploads/certificates/" + storedName;
            } catch (IOException e) {
                redirectAttributes.addFlashAttribute("errorMessage", "Lỗi tải lên tệp chứng chỉ: " + e.getMessage());
                return "redirect:/vieker?tab=profile";
            }
        }

        profile.setPendingCategoryRequest(categoryName);
        profile.setPendingCategoryYears(experienceYears);
        profile.setPendingCategoryDesc(skillsDescription);
        if (certPath != null) {
            profile.setPendingCategoryCertPath(certPath);
        }
        profile.setPendingCategoryRequestedAt(LocalDateTime.now());
        profileRepository.save(profile);

        redirectAttributes.addFlashAttribute("successMessage", "Yêu cầu mở rộng chuyên môn ngành '" + categoryName + "' đã được gửi thành công và đang chờ Ban Quản Trị phê duyệt!");
        return "redirect:/vieker?tab=profile";
    }

    @PostMapping("/profile/update-radius")
    public String handleUpdateRadius(@RequestParam("serviceRadiusKm") Integer serviceRadiusKm,
                                     @RequestParam(value = "redirectTab", defaultValue = "profile") String redirectTab,
                                     Authentication auth,
                                     RedirectAttributes redirectAttributes) {
        User vieker = getCurrentVieker(auth);
        ViekerProfile profile = profileService.getProfileByUserId(vieker.getId());
        profile.setServiceRadiusKm(serviceRadiusKm);
        profileRepository.save(profile);

        redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật bán kính hoạt động phục vụ thành " + serviceRadiusKm + " km thành công!");
        return "redirect:/vieker?tab=" + redirectTab;
    }
}
