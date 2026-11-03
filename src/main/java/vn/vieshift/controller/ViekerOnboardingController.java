package vn.vieshift.controller;

import jakarta.validation.Valid;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.vieshift.dto.ViekerSignupRequest;
import vn.vieshift.entity.ViekerProfile;
import vn.vieshift.repository.JobCategoryRepository;
import vn.vieshift.service.EmailService;
import vn.vieshift.service.ViekerOnboardingService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@Controller
public class ViekerOnboardingController {

    private final ViekerOnboardingService onboardingService;
    private final JobCategoryRepository categoryRepository;
    private final EmailService emailService;

    public ViekerOnboardingController(ViekerOnboardingService onboardingService,
                                      JobCategoryRepository categoryRepository,
                                      EmailService emailService) {
        this.onboardingService = onboardingService;
        this.categoryRepository = categoryRepository;
        this.emailService = emailService;
    }

    @GetMapping("/become-vieker")
    public String becomeVieker(@RequestParam(value = "modal", defaultValue = "false") boolean openModal,
                               Model model) {
        model.addAttribute("categories", categoryRepository.findByActiveTrueOrderByNameAsc());
        if (!model.containsAttribute("signupRequest")) {
            model.addAttribute("signupRequest", new ViekerSignupRequest());
        }
        model.addAttribute("openModal", openModal);
        return "vieker/become-vieker";
    }

    @PostMapping("/become-vieker")
    public String handleSignup(@Valid @ModelAttribute("signupRequest") ViekerSignupRequest request,
                               BindingResult bindingResult,
                               Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", categoryRepository.findByActiveTrueOrderByNameAsc());
            model.addAttribute("openModal", true);
            return "vieker/become-vieker";
        }

        try {
            ViekerProfile profile = onboardingService.signup(request);
            return "redirect:/become-vieker/check-email?token=" + profile.getOnboardingToken();
        } catch (IllegalArgumentException ex) {
            bindingResult.rejectValue("email", "error.email", ex.getMessage());
            model.addAttribute("categories", categoryRepository.findByActiveTrueOrderByNameAsc());
            model.addAttribute("openModal", true);
            return "vieker/become-vieker";
        }
    }

    @GetMapping("/become-vieker/check-email")
    public String checkEmail(@RequestParam("token") String token, Model model) {
        ViekerProfile profile = onboardingService.getByToken(token);
        model.addAttribute("profile", profile);
        model.addAttribute("onboardingUrl", "/onboarding/" + profile.getOnboardingToken());
        model.addAttribute("fullOnboardingUrl", onboardingService.onboardingLink(profile));
        model.addAttribute("smtpConfigured", emailService.isEnabled());
        return "vieker/check-email";
    }

    @PostMapping("/become-vieker/resend-email")
    public String resendEmail(@RequestParam("token") String token, RedirectAttributes redirectAttributes) {
        String newToken = onboardingService.resendOnboardingEmail(token);
        redirectAttributes.addFlashAttribute("successMessage",
                "Đã gửi lại đường dẫn hoàn thiện hồ sơ mới (có hiệu lực 24 giờ) đến hộp thư của bạn!");
        return "redirect:/become-vieker/check-email?token=" + newToken;
    }

    @GetMapping("/onboarding/{token}")
    public String onboardingForm(@PathVariable("token") String token, Model model) {
        ViekerProfile profile = onboardingService.getByToken(token);
        if (profile.isTokenExpired()) {
            model.addAttribute("profile", profile);
            return "vieker/token-expired";
        }
        model.addAttribute("profile", profile);
        model.addAttribute("user", profile.getUser());
        model.addAttribute("documents", ViekerOnboardingService.DOCUMENTS);
        return "vieker/onboarding";
    }

    @PostMapping("/onboarding/{token}")
    public String submitOnboarding(@PathVariable("token") String token,
                                   @RequestParam("idCardNumber") String idCardNumber,
                                   @RequestParam(value = "dateOfBirth", required = false)
                                   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateOfBirth,
                                   @RequestParam("permanentAddress") String permanentAddress,
                                   @RequestParam(value = "driverLicenseNumber", required = false) String driverLicenseNumber,
                                   @RequestParam("skillsDescription") String skillsDescription,
                                   @RequestParam(value = "experienceYears", defaultValue = "1") Integer experienceYears,
                                   @RequestParam(value = "portrait", required = false) MultipartFile portrait,
                                   @RequestParam(value = "idFront", required = false) MultipartFile idFront,
                                   @RequestParam(value = "idBack", required = false) MultipartFile idBack,
                                   @RequestParam(value = "criminalRecord", required = false) MultipartFile criminalRecord,
                                   @RequestParam(value = "driverLicense", required = false) MultipartFile driverLicense,
                                   RedirectAttributes redirectAttributes) {
        ViekerProfile profile = onboardingService.getByToken(token);
        if (profile.isTokenExpired()) {
            return "redirect:/onboarding/" + token;
        }

        Map<String, MultipartFile> files = new LinkedHashMap<>();
        files.put("portrait", portrait);
        files.put("idFront", idFront);
        files.put("idBack", idBack);
        files.put("criminalRecord", criminalRecord);
        files.put("driverLicense", driverLicense);

        try {
            onboardingService.submit(token, idCardNumber, dateOfBirth, permanentAddress,
                    driverLicenseNumber, skillsDescription, experienceYears, files);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Hồ sơ đối tác của bạn đã được nộp thành công và đang chờ Ban Quản Trị thẩm định!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/onboarding/" + token;
    }

    @GetMapping("/onboarding/{token}/doc/{key}")
    public ResponseEntity<Resource> viewOwnDocument(@PathVariable("token") String token,
                                                    @PathVariable("key") String key) {
        try {
            ViekerProfile profile = onboardingService.getByToken(token);
            Path path = onboardingService.resolveDocument(profile.getId(), key);
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
}
