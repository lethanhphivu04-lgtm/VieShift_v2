package vn.vieshift.controller;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;
import vn.vieshift.dto.*;
import vn.vieshift.entity.*;
import vn.vieshift.exception.InsufficientBalanceException;
import vn.vieshift.exception.UnauthorizedActionException;
import vn.vieshift.repository.DisputeRepository;
import vn.vieshift.repository.JobCategoryRepository;
import vn.vieshift.repository.ReviewRepository;
import vn.vieshift.repository.UserRepository;
import vn.vieshift.service.DisputeService;
import vn.vieshift.service.JobService;
import vn.vieshift.service.ReviewService;
import vn.vieshift.service.UserService;
import vn.vieshift.service.WalletService;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Controller
@RequestMapping("/customer")
public class CustomerController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final WalletService walletService;
    private final JobService jobService;
    private final ReviewService reviewService;
    private final DisputeService disputeService;
    private final JobCategoryRepository categoryRepository;
    private final ReviewRepository reviewRepository;
    private final DisputeRepository disputeRepository;
    private final vn.vieshift.service.VoucherService voucherService;

    public CustomerController(UserService userService,
                              UserRepository userRepository,
                              PasswordEncoder passwordEncoder,
                              WalletService walletService,
                              JobService jobService,
                              ReviewService reviewService,
                              DisputeService disputeService,
                              JobCategoryRepository categoryRepository,
                              ReviewRepository reviewRepository,
                              DisputeRepository disputeRepository,
                              vn.vieshift.service.VoucherService voucherService) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.walletService = walletService;
        this.jobService = jobService;
        this.reviewService = reviewService;
        this.disputeService = disputeService;
        this.categoryRepository = categoryRepository;
        this.reviewRepository = reviewRepository;
        this.disputeRepository = disputeRepository;
        this.voucherService = voucherService;
    }

    private User getCurrentCustomer(Authentication auth) {
        return userService.getByEmail(auth.getName());
    }

    @GetMapping
    public String dashboard(@RequestParam(value = "tab", defaultValue = "overview") String tab,
                            @RequestParam(value = "catId", required = false) Long catId,
                            @RequestParam(value = "catName", required = false) String catName,
                            @RequestParam(value = "title", required = false) String presetTitle,
                            @RequestParam(value = "viekerName", required = false) String viekerName,
                            Authentication auth,
                            Model model) {
        User customer = getCurrentCustomer(auth);
        Wallet wallet = walletService.getOrCreateWallet(customer);
        List<JobCategory> categories = categoryRepository.findByActiveTrueOrderByNameAsc();
        List<Job> allJobs = jobService.getJobsByCustomer(customer.getId());
        List<WalletTransaction> transactions = walletService.getTransactionHistory(customer.getId());

        List<Job> activeJobs = allJobs.stream()
                .filter(j -> j.getStatus() == JobStatus.OPEN
                        || j.getStatus() == JobStatus.ACCEPTED
                        || j.getStatus() == JobStatus.IN_PROGRESS
                        || j.getStatus() == JobStatus.PENDING_REVIEW
                        || j.getStatus() == JobStatus.DISPUTED)
                .toList();

        CreateJobRequest createJobRequest = new CreateJobRequest();
        Long resolvedCatId = catId;
        if (resolvedCatId == null && catName != null && !catName.isBlank()) {
            resolvedCatId = categories.stream()
                    .filter(c -> c.getName().toLowerCase().contains(catName.trim().toLowerCase()))
                    .map(JobCategory::getId)
                    .findFirst()
                    .orElse(null);
        }
        if (resolvedCatId != null) {
            createJobRequest.setCategoryId(resolvedCatId);
        }
        if (presetTitle != null && !presetTitle.isBlank()) {
            createJobRequest.setTitle(presetTitle.trim());
            String desc = "Yêu cầu thực hiện dịch vụ: " + presetTitle.trim() + ". Thợ mang đầy đủ dụng cụ chuyên dụng.";
            if (viekerName != null && !viekerName.isBlank()) {
                desc += " (Ưu tiên đặt lịch với Tasker: " + viekerName.trim() + ")";
            }
            createJobRequest.setDescription(desc);
            createJobRequest.setAddress("Vinhomes Central Park, P.22, Q. Bình Thạnh, TP.HCM");
        } else if (viekerName != null && !viekerName.isBlank()) {
            createJobRequest.setTitle("Đặt lịch dịch vụ cùng Tasker " + viekerName.trim());
            createJobRequest.setDescription("Ưu tiên chỉ định Tasker " + viekerName.trim() + " hỗ trợ thực hiện công việc tại nhà.");
            createJobRequest.setAddress("Vinhomes Central Park, P.22, Q. Bình Thạnh, TP.HCM");
        }

        List<vn.vieshift.entity.Voucher> publicVouchers = voucherService.getPublicActiveVouchers();

        model.addAttribute("selectedViekerName", viekerName);
        model.addAttribute("publicVouchers", publicVouchers);

        model.addAttribute("customer", customer);
        model.addAttribute("wallet", wallet);
        model.addAttribute("categories", categories);
        model.addAttribute("allJobs", allJobs);
        model.addAttribute("activeJobs", activeJobs);
        model.addAttribute("transactions", transactions);
        model.addAttribute("activeTab", tab);
        model.addAttribute("createJobRequest", createJobRequest);
        model.addAttribute("depositRequest", new DepositRequest());
        model.addAttribute("withdrawRequest", new WithdrawRequest());

        UpdateCustomerProfileRequest updateProfileRequest = new UpdateCustomerProfileRequest(
                customer.getFullName(),
                customer.getPhone(),
                customer.getAddress()
        );
        ChangePasswordRequest changePasswordRequest = new ChangePasswordRequest();
        model.addAttribute("updateProfileRequest", updateProfileRequest);
        model.addAttribute("changePasswordRequest", changePasswordRequest);

        return "customer/dashboard";
    }

    @GetMapping("/jobs/{id}")
    public String jobDetail(@PathVariable("id") Long jobId, Authentication auth, Model model) {
        User customer = getCurrentCustomer(auth);
        Job job = jobService.getJobById(jobId);

        if (!job.getCustomer().getId().equals(customer.getId())) {
            return "redirect:/customer?error=unauthorized";
        }

        Review review = reviewRepository.findByJobId(jobId).orElse(null);
        Dispute dispute = disputeRepository.findByJobId(jobId).orElse(null);
        Wallet wallet = walletService.getOrCreateWallet(customer);

        model.addAttribute("customer", customer);
        model.addAttribute("wallet", wallet);
        model.addAttribute("job", job);
        model.addAttribute("review", review);
        model.addAttribute("dispute", dispute);
        model.addAttribute("reviewRequest", new ReviewRequest());
        model.addAttribute("disputeRequest", new DisputeRequest());

        return "customer/job-detail";
    }

    @PostMapping("/jobs/create")
    public String handleCreateJob(@Valid @ModelAttribute("createJobRequest") CreateJobRequest request,
                                  BindingResult bindingResult,
                                  Authentication auth,
                                  RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Thông tin đăng việc chưa hợp lệ, vui lòng kiểm tra lại");
            return "redirect:/customer?tab=create";
        }

        User customer = getCurrentCustomer(auth);
        try {
            Job job = jobService.createJob(customer.getId(), request);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đặt lịch dịch vụ thành công! Chi phí " + job.getPrice() + " đ được bảo đảm an toàn và chỉ thanh toán khi bạn nghiệm thu hài lòng.");
            return "redirect:/customer?tab=jobs";
        } catch (InsufficientBalanceException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/customer?tab=wallet";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + ex.getMessage());
            return "redirect:/customer?tab=create";
        }
    }

    @PostMapping("/jobs/{id}/approve")
    public String handleApproveJob(@PathVariable("id") Long jobId,
                                   @RequestParam(value = "tipAmount", required = false, defaultValue = "0") BigDecimal tipAmount,
                                   @RequestParam(value = "rating", required = false) Integer rating,
                                   @RequestParam(value = "comment", required = false) String comment,
                                   Authentication auth,
                                   RedirectAttributes redirectAttributes) {
        User customer = getCurrentCustomer(auth);
        try {
            Job job = jobService.approveAndPayJob(customer.getId(), jobId);

            String priceFormatted = java.text.NumberFormat.getInstance(new java.util.Locale("vi", "VN")).format(job.getPrice());
            StringBuilder msg = new StringBuilder("Nghiệm thu thành công! Đã giải ngân " + priceFormatted + " đ tiền công cho thợ.");

            // Xử lý gửi tiền boa (Tip) nếu khách có chọn
            if (tipAmount != null && tipAmount.compareTo(BigDecimal.ZERO) > 0 && job.getVieker() != null) {
                try {
                    walletService.transferTip(customer.getId(), job.getVieker().getId(), job, tipAmount);
                    String tipFormatted = java.text.NumberFormat.getInstance(new java.util.Locale("vi", "VN")).format(tipAmount);
                    msg.append(" Đã gửi kèm ").append(tipFormatted).append(" đ tiền boa khích lệ thợ!");
                } catch (InsufficientBalanceException ex) {
                    msg.append(" (Lưu ý: Không thể gửi tiền boa do ").append(ex.getMessage()).append(")");
                } catch (Exception ex) {
                    msg.append(" (Lưu ý: Chưa thể gửi tiền boa: ").append(ex.getMessage()).append(")");
                }
            }

            // Xử lý lưu đánh giá nếu khách nhập
            if (rating != null && rating >= 1 && rating <= 5) {
                try {
                    ReviewRequest reviewRequest = new ReviewRequest();
                    reviewRequest.setJobId(jobId);
                    reviewRequest.setRating(rating);
                    reviewRequest.setComment((comment != null && !comment.isBlank()) ? comment.trim() : "Dịch vụ rất tốt, thợ nhiệt tình chuyên nghiệp!");
                    reviewService.createReview(customer.getId(), reviewRequest);
                    msg.append(" Đánh giá ").append(rating).append(" sao của bạn đã được ghi nhận.");
                } catch (Exception ignored) {}
            }

            redirectAttributes.addFlashAttribute("successMessage", msg.toString());
            return "redirect:/customer/jobs/" + jobId + "?approved=true";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi nghiệm thu: " + ex.getMessage());
            return "redirect:/customer/jobs/" + jobId;
        }
    }

    @PostMapping("/jobs/{id}/cancel")
    public String handleCancelJob(@PathVariable("id") Long jobId,
                                  Authentication auth,
                                  RedirectAttributes redirectAttributes) {
        User customer = getCurrentCustomer(auth);
        try {
            jobService.cancelJob(customer.getId(), jobId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã hủy công việc thành công. 100% số tiền đã được hoàn lại ví của bạn.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi hủy việc: " + ex.getMessage());
        }
        return "redirect:/customer?tab=jobs";
    }

    @PostMapping("/jobs/{id}/dispute")
    public String handleDisputeJob(@PathVariable("id") Long jobId,
                                   @Valid @ModelAttribute("disputeRequest") DisputeRequest request,
                                   BindingResult bindingResult,
                                   @RequestParam(value = "attachment", required = false) MultipartFile attachment,
                                   Authentication auth,
                                   RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng nhập đầy đủ lý do khiếu nại");
            return "redirect:/customer/jobs/" + jobId;
        }

        User customer = getCurrentCustomer(auth);
        request.setJobId(jobId);
        try {
            disputeService.openDispute(customer.getId(), request, attachment);
            redirectAttributes.addFlashAttribute("successMessage", "Đã gửi yêu cầu hỗ trợ khiếu nại" + (attachment != null && !attachment.isEmpty() ? " kèm tệp bằng chứng" : "") + ". Khoản tiền đang được giữ an toàn để Ban Quản Trị hỗ trợ xử lý công bằng.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi gửi khiếu nại: " + ex.getMessage());
        }
        return "redirect:/customer/jobs/" + jobId;
    }

    @GetMapping("/disputes/{id}/attachment")
    public ResponseEntity<Resource> downloadDisputeAttachment(@PathVariable("id") Long disputeId,
                                                              Authentication auth) {
        if (auth == null) {
            return ResponseEntity.status(401).build();
        }
        User currentUser = getCurrentCustomer(auth);
        Dispute dispute = disputeService.getDisputeById(disputeId);

        boolean isAllowed = dispute.getOpenedBy().getId().equals(currentUser.getId())
                || dispute.getJob().getCustomer().getId().equals(currentUser.getId())
                || (dispute.getJob().getVieker() != null && dispute.getJob().getVieker().getId().equals(currentUser.getId()))
                || currentUser.getRole() == Role.ADMIN;

        if (!isAllowed) {
            return ResponseEntity.status(403).build();
        }

        Path path = disputeService.resolveAttachment(disputeId);
        if (path == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            Resource resource = new UrlResource(path.toUri());
            String contentType = Files.probeContentType(path);
            if (contentType == null) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }
            String originalName = dispute.getAttachmentOriginalName() != null ? dispute.getAttachmentOriginalName() : path.getFileName().toString();
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + originalName + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/profile/update")
    public String updateProfile(@Valid @ModelAttribute("updateProfileRequest") UpdateCustomerProfileRequest request,
                                BindingResult bindingResult,
                                Authentication auth,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", bindingResult.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/customer?tab=profile";
        }

        User customer = getCurrentCustomer(auth);
        customer.setFullName(request.getFullName().trim());
        customer.setPhone(request.getPhone().trim());
        if (request.getAddress() != null) {
            customer.setAddress(request.getAddress().trim());
        }
        userRepository.save(customer);

        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thông tin tài khoản thành công!");
        return "redirect:/customer?tab=profile";
    }

    @PostMapping("/profile/change-password")
    public String changePassword(@Valid @ModelAttribute("changePasswordRequest") ChangePasswordRequest request,
                                 BindingResult bindingResult,
                                 Authentication auth,
                                 RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", bindingResult.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/customer?tab=profile";
        }

        User customer = getCurrentCustomer(auth);
        if (!passwordEncoder.matches(request.getCurrentPassword(), customer.getPassword())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Mật khẩu hiện tại không chính xác");
            return "redirect:/customer?tab=profile";
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Mật khẩu xác nhận không khớp với mật khẩu mới");
            return "redirect:/customer?tab=profile";
        }

        customer.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(customer);

        redirectAttributes.addFlashAttribute("successMessage", "Đổi mật khẩu thành công! Hãy ghi nhớ mật khẩu mới của bạn.");
        return "redirect:/customer?tab=profile";
    }

    @PostMapping("/jobs/{id}/review")
    public String handleReviewJob(@PathVariable("id") Long jobId,
                                  @Valid @ModelAttribute("reviewRequest") ReviewRequest request,
                                  BindingResult bindingResult,
                                  Authentication auth,
                                  RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng chọn số sao đánh giá hợp lệ");
            return "redirect:/customer/jobs/" + jobId;
        }

        User customer = getCurrentCustomer(auth);
        request.setJobId(jobId);
        try {
            reviewService.createReview(customer.getId(), request);
            redirectAttributes.addFlashAttribute("successMessage", "Cảm ơn bạn đã gửi đánh giá cho thợ!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi đánh giá: " + ex.getMessage());
        }
        return "redirect:/customer/jobs/" + jobId;
    }

    @PostMapping("/wallet/withdraw")
    public String handleWithdraw(@Valid @ModelAttribute("withdrawRequest") WithdrawRequest request,
                                 BindingResult bindingResult,
                                 Authentication auth,
                                 RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng nhập đầy đủ thông tin tài khoản ngân hàng để rút tiền");
            return "redirect:/customer?tab=wallet";
        }

        User customer = getCurrentCustomer(auth);
        try {
            walletService.withdraw(customer.getId(), request);
            redirectAttributes.addFlashAttribute("successMessage", "Lệnh rút tiền đã được thực hiện thành công!");
        } catch (InsufficientBalanceException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/customer?tab=wallet";
    }

    @GetMapping("/jobs/{id}/invoice")
    public String viewInvoice(@PathVariable("id") Long jobId,
                              Authentication auth,
                              Model model) {
        User customer = getCurrentCustomer(auth);
        Job job = jobService.getJobById(jobId);

        if (!job.getCustomer().getId().equals(customer.getId()) && customer.getRole() != Role.ADMIN) {
            throw new UnauthorizedActionException("Bạn không có quyền xem hóa đơn của công việc này");
        }

        Review review = reviewRepository.findByJobId(jobId).orElse(null);
        String invoiceNo = String.format("VS-INV-2026-%05d", job.getId());

        // Tìm giao dịch tip nếu có
        BigDecimal tipAmount = BigDecimal.ZERO;
        List<WalletTransaction> txs = walletService.getTransactionHistory(customer.getId());
        for (WalletTransaction tx : txs) {
            if (tx.getJob() != null && tx.getJob().getId().equals(job.getId()) && tx.getType() == TransactionType.TIP_SENT) {
                tipAmount = tx.getAmount();
                break;
            }
        }

        BigDecimal totalPaid = job.getPrice().add(tipAmount);

        model.addAttribute("job", job);
        model.addAttribute("customer", customer);
        model.addAttribute("vieker", job.getVieker());
        model.addAttribute("review", review);
        model.addAttribute("invoiceNo", invoiceNo);
        model.addAttribute("tipAmount", tipAmount);
        model.addAttribute("totalPaid", totalPaid);
        model.addAttribute("issueDate", job.getCompletedAt() != null ? job.getCompletedAt() : job.getCreatedAt());

        return "customer/invoice";
    }
}
