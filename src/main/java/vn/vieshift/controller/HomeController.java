package vn.vieshift.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;
import vn.vieshift.dto.RegisterRequest;
import vn.vieshift.entity.Job;
import vn.vieshift.entity.JobCategory;
import vn.vieshift.entity.JobStatus;
import vn.vieshift.entity.Role;
import vn.vieshift.entity.VerificationStatus;
import vn.vieshift.entity.ViekerProfile;
import vn.vieshift.repository.JobCategoryRepository;
import vn.vieshift.repository.JobRepository;
import vn.vieshift.repository.UserRepository;
import vn.vieshift.repository.ViekerProfileRepository;
import vn.vieshift.service.UserService;

import java.util.List;

@Controller
public class HomeController {

    private final JobCategoryRepository categoryRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final ViekerProfileRepository profileRepository;
    private final UserService userService;

    public HomeController(JobCategoryRepository categoryRepository,
                          JobRepository jobRepository,
                          UserRepository userRepository,
                          ViekerProfileRepository profileRepository,
                          UserService userService) {
        this.categoryRepository = categoryRepository;
        this.jobRepository = jobRepository;
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.userService = userService;
    }

    @GetMapping("/")
    public String index(Model model) {
        List<JobCategory> categories = categoryRepository.findByActiveTrueOrderByNameAsc();
        List<ViekerProfile> verifiedViekers = profileRepository.findByVerificationStatusOrderByCreatedAtDesc(VerificationStatus.APPROVED);
        List<Job> openJobs = jobRepository.findByStatusOrderByCreatedAtDesc(JobStatus.OPEN);

        model.addAttribute("categories", categories);
        model.addAttribute("verifiedViekers", verifiedViekers);
        model.addAttribute("openJobs", openJobs);
        model.addAttribute("totalJobs", jobRepository.count());
        model.addAttribute("totalUsers", userRepository.count());
        return "index";
    }

    @GetMapping("/explore")
    public String explore(@RequestParam(value = "q", required = false) String q,
                          @RequestParam(value = "catId", required = false) Long catId,
                          @RequestParam(value = "catName", required = false) String catName,
                          Model model) {
        List<JobCategory> categories = categoryRepository.findByActiveTrueOrderByNameAsc();
        Long resolvedCatId = catId;
        if (resolvedCatId == null && catName != null && !catName.isBlank()) {
            resolvedCatId = categories.stream()
                    .filter(c -> c.getName().toLowerCase().contains(catName.trim().toLowerCase()))
                    .map(JobCategory::getId)
                    .findFirst()
                    .orElse(null);
        }

        final Long filterCatId = resolvedCatId;
        final String keyword = (q != null && !q.isBlank()) ? q.trim().toLowerCase() : null;

        List<Job> openJobs = jobRepository.findByStatusOrderByCreatedAtDesc(JobStatus.OPEN).stream()
                .filter(j -> filterCatId == null || j.getCategory().getId().equals(filterCatId))
                .filter(j -> keyword == null
                        || j.getTitle().toLowerCase().contains(keyword)
                        || j.getDescription().toLowerCase().contains(keyword)
                        || j.getAddress().toLowerCase().contains(keyword)
                        || j.getCategory().getName().toLowerCase().contains(keyword))
                .toList();

        List<ViekerProfile> verifiedViekers = profileRepository.findByVerificationStatusOrderByCreatedAtDesc(VerificationStatus.APPROVED).stream()
                .filter(vp -> keyword == null
                        || vp.getUser().getFullName().toLowerCase().contains(keyword)
                        || (vp.getSkillsDescription() != null && vp.getSkillsDescription().toLowerCase().contains(keyword))
                        || (vp.getOperatingZone() != null && vp.getOperatingZone().toLowerCase().contains(keyword)))
                .toList();

        model.addAttribute("categories", categories);
        model.addAttribute("openJobs", openJobs);
        model.addAttribute("verifiedViekers", verifiedViekers);
        model.addAttribute("selectedCatId", filterCatId);
        model.addAttribute("q", q);
        return "explore";
    }

    @GetMapping("/book-task")
    public String bookTask(@RequestParam(value = "catId", required = false) Long catId,
                           @RequestParam(value = "catName", required = false) String catName,
                           @RequestParam(value = "title", required = false) String title,
                           @RequestParam(value = "viekerName", required = false) String viekerName,
                           Authentication auth,
                           HttpServletRequest request) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/customer")
                .queryParam("tab", "create");
        if (catId != null) {
            builder.queryParam("catId", catId);
        }
        if (catName != null && !catName.isBlank()) {
            builder.queryParam("catName", catName);
        }
        if (title != null && !title.isBlank()) {
            builder.queryParam("title", title);
        }
        if (viekerName != null && !viekerName.isBlank()) {
            builder.queryParam("viekerName", viekerName);
        }
        String targetUrl = builder.build().encode().toUriString();

        boolean isAuthenticated = auth != null && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getName());

        if (!isAuthenticated) {
            // Khách chưa đăng nhập: Lưu đích đến vào session để sau khi đăng nhập chuyển tiếp đến đây
            HttpSession session = request.getSession(true);
            session.setAttribute("REDIRECT_AFTER_LOGIN", targetUrl);
            return "redirect:/login";
        }

        boolean isCustomer = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CUSTOMER"));
        if (!isCustomer) {
            return "redirect:/explore";
        }

        return "redirect:" + targetUrl;
    }

    @GetMapping("/login")
    public String login(Authentication auth, HttpServletRequest request, Model model) {
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            return "redirect:/";
        }
        HttpSession session = request.getSession(false);
        if (session != null) {
            Object oauth2Error = session.getAttribute("OAUTH2_ERROR");
            if (oauth2Error != null) {
                model.addAttribute("oauth2Error", oauth2Error.toString());
                session.removeAttribute("OAUTH2_ERROR");
            }
        }
        return "login";
    }

    @GetMapping("/register")
    public String registerForm(Model model, Authentication auth) {
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            return "redirect:/";
        }
        if (!model.containsAttribute("registerRequest")) {
            model.addAttribute("registerRequest", new RegisterRequest());
        }
        return "register";
    }

    @PostMapping("/register")
    public String handleRegister(@Valid @ModelAttribute("registerRequest") RegisterRequest request,
                                 BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes) {
        request.setRole(Role.CUSTOMER);
        if (bindingResult.hasErrors()) {
            return "register";
        }

        try {
            userService.register(request);
            redirectAttributes.addFlashAttribute("successMessage", "Đăng ký tài khoản Khách hàng thành công! Vui lòng đăng nhập để bắt đầu.");
            return "redirect:/login";
        } catch (IllegalArgumentException ex) {
            bindingResult.rejectValue("email", "error.email", ex.getMessage());
            return "register";
        }
    }

    @GetMapping("/terms")
    public String terms(Model model) {
        model.addAttribute("activeSection", "terms");
        return "legal/terms";
    }

    @GetMapping("/privacy")
    public String privacy(Model model) {
        model.addAttribute("activeSection", "privacy");
        return "legal/terms";
    }

    @GetMapping("/about")
    public String about(Model model) {
        return "info/about";
    }

    @GetMapping("/partners")
    public String partners(Model model) {
        return "info/partners";
    }

    @GetMapping("/guarantee")
    public String guarantee(Model model) {
        return "info/guarantee";
    }

    @GetMapping("/handbook")
    public String handbook(Model model) {
        return "info/handbook";
    }

    @GetMapping("/mobile-app")
    public String mobileApp(Model model) {
        return "info/mobile-app";
    }

    @org.springframework.beans.factory.annotation.Value("${vieshift.google.client-id:}")
    private String googleClientId;

    @GetMapping("/oauth2/authorization/google")
    public String googleOauthFallback(RedirectAttributes redirectAttributes) {
        if (googleClientId == null || googleClientId.isBlank()) {
            redirectAttributes.addFlashAttribute("googleNotice",
                    "Tính năng Đăng nhập bằng Google đã sẵn sàng tích hợp. Quản trị viên vui lòng cấu hình GOOGLE_CLIENT_ID & GOOGLE_CLIENT_SECRET trong application.properties để kích hoạt OAuth2.");
        }
        return "redirect:/login";
    }
}
