package vn.vieshift.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.vieshift.entity.MinigameSlice;
import vn.vieshift.entity.MinigameSpinLog;
import vn.vieshift.entity.ShopOrder;
import vn.vieshift.entity.User;
import vn.vieshift.service.DataIntegrityService;
import vn.vieshift.service.MinigameService;
import vn.vieshift.service.UserService;

import java.math.BigDecimal;
import java.util.*;

@Controller
public class MinigameController {

    private final MinigameService minigameService;
    private final UserService userService;
    private final DataIntegrityService dataIntegrityService;

    public MinigameController(MinigameService minigameService,
                              UserService userService,
                              DataIntegrityService dataIntegrityService) {
        this.minigameService = minigameService;
        this.userService = userService;
        this.dataIntegrityService = dataIntegrityService;
    }

    private User getCurrentUser(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return null;
        }
        return userService.findByEmail(auth.getName()).orElse(null);
    }

    /**
     * Trang giao dien Vong Quay May Man (Lucky Wheel)
     */
    @GetMapping("/minigame")
    public String showMinigamePage(@RequestParam(required = false) String phone,
                                   Authentication auth,
                                   Model model) {
        User currentUser = getCurrentUser(auth);
        String targetPhone = (phone != null && !phone.isBlank())
                ? phone.trim()
                : (currentUser != null ? currentUser.getPhone() : null);

        List<MinigameSlice> slices = minigameService.getActiveSlices();
        List<MinigameService.WinnerDTO> winners = minigameService.getRecentWinners();
        List<MinigameSpinLog> mySpinHistory = minigameService.getMySpinHistory(currentUser, targetPhone);

        MinigameService.SpinBalanceDetail balance = null;
        if (currentUser != null || targetPhone != null) {
            balance = minigameService.calculateSpins(currentUser, targetPhone);
        }

        model.addAttribute("slices", slices);
        model.addAttribute("recentWinners", winners);
        model.addAttribute("mySpinHistory", mySpinHistory);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("userBalance", balance);

        return "minigame";
    }

    /**
     * Quy doi Qua Hien Vat hoac Voucher trung thuong thanh Tien cong truc tiep vao Vi VieShift
     */
    @PostMapping("/minigame/prizes/{id}/redeem-wallet")
    public String redeemPrizeToWallet(@PathVariable Long id,
                                      Authentication auth,
                                      RedirectAttributes redirectAttributes) {
        User currentUser = getCurrentUser(auth);
        if (currentUser == null) {
            return "redirect:/login";
        }
        try {
            BigDecimal amount = minigameService.redeemPrizeToWallet(id, currentUser);
            redirectAttributes.addFlashAttribute("successMessage",
                    "🎉 Quy đổi thành công! Hệ thống đã cộng trực tiếp +"
                            + String.format("%,.0f", amount) + "đ vào Ví Bảo Đảm VieShift của bạn.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/minigame#my-prize-vault";
    }

    /**
     * Xac nhan dia chi nhan Qua Hien Vat 0d (Tao don hang giao tan nha tai VieShift Mall)
     */
    @PostMapping("/minigame/prizes/{id}/claim-gift")
    public String claimPhysicalGift(@PathVariable Long id,
                                    @RequestParam String recipientName,
                                    @RequestParam String recipientPhone,
                                    @RequestParam String shippingAddress,
                                    Authentication auth,
                                    RedirectAttributes redirectAttributes) {
        User currentUser = getCurrentUser(auth);
        if (currentUser == null) {
            return "redirect:/login";
        }
        try {
            ShopOrder giftOrder = minigameService.claimPhysicalGiftOrder(
                    id, currentUser, recipientName, recipientPhone, shippingAddress);
            redirectAttributes.addFlashAttribute("successMessage",
                    "🎁 Đã tạo đơn giao quà 0đ thành công (#VS-MALL-" + giftOrder.getId()
                            + ")! Bạn có thể theo dõi tiến độ giao quà tại Cửa hàng Vật tư (/shop).");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/minigame#my-prize-vault";
    }

    /**
     * API Kiem tra luot quay theo SDT hoac User hien tai
     */
    @RequestMapping(value = "/api/minigame/check-spins", method = {RequestMethod.GET, RequestMethod.POST})
    @ResponseBody
    public ResponseEntity<Map<String, Object>> checkSpins(
            @RequestParam(required = false) String phone,
            Authentication auth) {
        try {
            User currentUser = getCurrentUser(auth);

            String targetPhone = (phone != null && !phone.isBlank())
                    ? phone.trim()
                    : (currentUser != null ? currentUser.getPhone() : null);

            MinigameService.SpinBalanceDetail balance = minigameService.calculateSpins(currentUser, targetPhone);

            Map<String, Object> resp = new HashMap<>();
            resp.put("success", true);
            resp.put("balance", balance);
            resp.put("phone", targetPhone);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * API Thuc hien luot quay may man
     */
    @PostMapping("/api/minigame/spin")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> spin(
            @RequestParam(required = false) String phone,
            Authentication auth) {
        try {
            User currentUser = getCurrentUser(auth);

            MinigameService.SpinResult result = minigameService.spin(currentUser, phone);

            Map<String, Object> resp = new HashMap<>();
            resp.put("success", true);
            resp.put("result", result);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * API Doi ma Giftcode lay them luot quay
     */
    @PostMapping("/api/minigame/redeem-giftcode")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> redeemGiftcode(
            @RequestParam String code,
            @RequestParam(required = false) String phone,
            Authentication auth) {
        try {
            User currentUser = getCurrentUser(auth);

            int spinsAwarded = minigameService.redeemGiftcode(code, phone, currentUser);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Chúc mừng! Bạn đã nhận thêm " + spinsAwarded + " lượt quay may mắn!",
                    "spinsAwarded", spinsAwarded
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * API Quet kiem tra toan ven CSDL (Anti-Tamper Database Scanner)
     */
    @GetMapping("/api/admin/security/integrity-scan")
    @ResponseBody
    public ResponseEntity<DataIntegrityService.IntegrityScanReport> scanIntegrity() {
        DataIntegrityService.IntegrityScanReport report = dataIntegrityService.scanDatabaseIntegrity();
        return ResponseEntity.ok(report);
    }
}
