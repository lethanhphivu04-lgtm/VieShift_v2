package vn.vieshift.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.vieshift.entity.Product;
import vn.vieshift.entity.ProductReview;
import vn.vieshift.entity.Role;
import vn.vieshift.entity.ShopOrder;
import vn.vieshift.entity.User;
import vn.vieshift.entity.Voucher;
import vn.vieshift.entity.Wallet;
import vn.vieshift.exception.InsufficientBalanceException;
import vn.vieshift.exception.UnauthorizedActionException;
import vn.vieshift.service.DataIntegrityService;
import vn.vieshift.service.GhnShippingService;
import vn.vieshift.service.ShopService;
import vn.vieshift.service.UserService;
import vn.vieshift.service.VnPayService;
import vn.vieshift.service.VoucherService;
import vn.vieshift.service.WalletService;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/shop")
public class ShopController {

    private final ShopService shopService;
    private final UserService userService;
    private final WalletService walletService;
    private final VoucherService voucherService;
    private final DataIntegrityService dataIntegrityService;
    private final GhnShippingService ghnShippingService;
    private final VnPayService vnPayService;

    public ShopController(ShopService shopService,
                          UserService userService,
                          WalletService walletService,
                          VoucherService voucherService,
                          DataIntegrityService dataIntegrityService,
                          GhnShippingService ghnShippingService,
                          VnPayService vnPayService) {
        this.shopService = shopService;
        this.userService = userService;
        this.walletService = walletService;
        this.voucherService = voucherService;
        this.dataIntegrityService = dataIntegrityService;
        this.ghnShippingService = ghnShippingService;
        this.vnPayService = vnPayService;
    }

    @GetMapping
    public String storefront(@RequestParam(value = "category", required = false) String category,
                             @RequestParam(value = "q", required = false) String query,
                             @RequestParam(value = "sort", required = false, defaultValue = "popular") String sort,
                             @RequestParam(value = "priceRange", required = false, defaultValue = "ALL") String priceRange,
                             @RequestParam(value = "trackCode", required = false) String trackCode,
                             @RequestParam(value = "trackPhone", required = false) String trackPhone,
                             HttpServletRequest request,
                             Authentication auth,
                             Model model) {
        request.getSession(true);
        List<Product> products = shopService.searchProducts(category, query, sort, priceRange);
        List<String> categories = shopService.getCategories();
        List<Voucher> publicVouchers = voucherService.getPublicActiveVouchers();

        model.addAttribute("products", products);
        model.addAttribute("categories", categories);
        model.addAttribute("publicVouchers", publicVouchers);
        model.addAttribute("selectedCategory", category != null && !category.isBlank() ? category : "ALL");
        model.addAttribute("searchQuery", query != null ? query : "");
        model.addAttribute("selectedSort", sort != null ? sort : "popular");
        model.addAttribute("selectedPriceRange", priceRange != null ? priceRange : "ALL");

        if (trackCode != null && !trackCode.isBlank()) {
            model.addAttribute("trackCode", trackCode.trim());
            model.addAttribute("trackPhone", trackPhone != null ? trackPhone.trim() : "");
            Optional<ShopOrder> tracked = shopService.findOrderForTracking(trackCode, trackPhone);
            if (tracked.isPresent()) {
                model.addAttribute("trackedOrder", tracked.get());
            } else {
                model.addAttribute("trackingError", "Không tìm thấy đơn hàng khớp với Mã đơn và Số điện thoại đã nhập.");
            }
        }

        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            try {
                User currentUser = userService.getByEmail(auth.getName());
                Wallet wallet = walletService.getOrCreateWallet(currentUser);
                List<ShopOrder> myOrders = shopService.getCustomerOrders(currentUser.getId());
                model.addAttribute("currentUser", currentUser);
                model.addAttribute("wallet", wallet);
                model.addAttribute("myOrders", myOrders);
            } catch (Exception ignored) {
                model.addAttribute("myOrders", Collections.emptyList());
            }
        } else {
            model.addAttribute("myOrders", Collections.emptyList());
        }

        return "shop";
    }

    // =========================================================================
    // API GIAO HÀNG NHANH (GHN) & ĐỊNH VỊ GPS REVERSE GEOCODE (TỪ LTW_01)
    // =========================================================================

    @GetMapping(value = "/api/ghn/provinces", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> getGhnProvinces() {
        return ResponseEntity.ok(Map.of("success", true, "data", ghnShippingService.getProvinces()));
    }

    @GetMapping(value = "/api/ghn/districts", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> getGhnDistricts(@RequestParam("provinceId") int provinceId) {
        return ResponseEntity.ok(Map.of("success", true, "data", ghnShippingService.getDistricts(provinceId)));
    }

    @GetMapping(value = "/api/ghn/wards", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> getGhnWards(@RequestParam("districtId") int districtId) {
        return ResponseEntity.ok(Map.of("success", true, "data", ghnShippingService.getWards(districtId)));
    }

    @GetMapping(value = "/api/ghn/calculate-fee", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> calculateGhnFee(@RequestParam("districtId") int districtId,
                                             @RequestParam("wardCode") String wardCode,
                                             @RequestParam(value = "totalItems", defaultValue = "1") int totalItems,
                                             @RequestParam(value = "subtotal", defaultValue = "0") BigDecimal subtotal,
                                             @RequestParam(value = "serviceTypeId", defaultValue = "2") int serviceTypeId) {
        return ResponseEntity.ok(ghnShippingService.calculateShippingFee(districtId, wardCode, totalItems, subtotal, serviceTypeId));
    }

    @GetMapping(value = "/api/geocode/reverse", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> reverseGeocode(@RequestParam("lat") double lat,
                                            @RequestParam("lon") double lon) {
        return ResponseEntity.ok(ghnShippingService.reverseGeocode(lat, lon));
    }

    // =========================================================================
    // CHI TIẾT SẢN PHẨM & ĐÁNH GIÁ NHẬN LƯỢT QUAY MINIGAME
    // =========================================================================

    @GetMapping("/products/{id}")
    public String productDetail(@PathVariable("id") Long id,
                                HttpServletRequest request,
                                Authentication auth,
                                Model model) {
        request.getSession(true);
        Product product = shopService.getProductById(id);
        List<Product> relatedProducts = shopService.searchProducts(product.getCategory(), null).stream()
                .filter(p -> !p.getId().equals(id))
                .limit(4)
                .toList();

        List<ProductReview> reviews = shopService.getProductReviews(id);
        double avgRating = reviews.isEmpty()
                ? 5.0
                : Math.round(reviews.stream().mapToInt(ProductReview::getRating).average().orElse(5.0) * 10.0) / 10.0;
        boolean checksumVerified = dataIntegrityService.verifyProductChecksum(product);

        model.addAttribute("product", product);
        model.addAttribute("relatedProducts", relatedProducts);
        model.addAttribute("reviews", reviews);
        model.addAttribute("avgRating", avgRating);
        model.addAttribute("reviewCount", reviews.size());
        model.addAttribute("checksumVerified", checksumVerified);

        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            try {
                User currentUser = userService.getByEmail(auth.getName());
                Wallet wallet = walletService.getOrCreateWallet(currentUser);
                model.addAttribute("currentUser", currentUser);
                model.addAttribute("wallet", wallet);
            } catch (Exception ignored) {}
        }
        return "shop-product-detail";
    }

    @PostMapping("/products/{id}/reviews")
    public String submitReview(@PathVariable("id") Long id,
                               @RequestParam("reviewerName") String reviewerName,
                               @RequestParam(value = "reviewerPhone", required = false) String reviewerPhone,
                               @RequestParam(value = "rating", defaultValue = "5") Integer rating,
                               @RequestParam("comment") String comment,
                               RedirectAttributes redirectAttributes) {
        try {
            ProductReview saved = shopService.addProductReview(id, reviewerName, reviewerPhone, rating, comment);
            String verifiedText = saved.isVerifiedPurchase() ? " (Đã xác thực mua hàng ✓)" : "";
            redirectAttributes.addFlashAttribute("successMessage",
                    "Cảm ơn bạn đã gửi đánh giá" + verifiedText + "! 🎡 Bạn nhận được mã Giftcode REVIEWVIP (+1 lượt quay tại Vòng Quay May Mắn)!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể gửi đánh giá: " + ex.getMessage());
        }
        return "redirect:/shop/products/" + id + "#reviews-section";
    }

    // =========================================================================
    // THANH TOÁN ĐƠN HÀNG VẬT TƯ (HỖ TRỢ VÍ VIESHIFT, COD & VNPAY + GHN API)
    // =========================================================================

    @GetMapping("/checkout-now")
    public String checkoutNow(@RequestParam("productId") Long productId,
                              @RequestParam(value = "qty", defaultValue = "1") Integer qty,
                              HttpServletRequest request,
                              Authentication auth,
                              RedirectAttributes redirectAttributes,
                              Model model) {
        request.getSession(true);
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng đăng nhập để thanh toán đơn hàng vật tư.");
            return "redirect:/login";
        }

        int quantity = Math.max(1, qty != null ? qty : 1);
        User currentUser = userService.getByEmail(auth.getName());
        Wallet wallet = walletService.getOrCreateWallet(currentUser);
        Product product = shopService.getProductById(productId);
        List<Voucher> publicVouchers = voucherService.getPublicActiveVouchers();

        BigDecimal subtotalAmount = product.getPrice().multiply(BigDecimal.valueOf(quantity));
        BigDecimal shippingFee = subtotalAmount.compareTo(ShopService.FREESHIP_THRESHOLD) >= 0
                ? BigDecimal.ZERO
                : ShopService.STANDARD_SHIPPING_FEE;
        BigDecimal totalAmount = subtotalAmount.add(shippingFee);

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("wallet", wallet);
        model.addAttribute("checkoutProducts", List.of(product));
        model.addAttribute("initialQty", quantity);
        model.addAttribute("subtotalAmount", subtotalAmount);
        model.addAttribute("shippingFee", shippingFee);
        model.addAttribute("totalAmount", totalAmount);
        model.addAttribute("publicVouchers", publicVouchers);

        return "shop-checkout";
    }

    @PostMapping("/checkout")
    public String checkout(@RequestParam(value = "productIds", required = false) List<Long> productIds,
                           @RequestParam(value = "quantities", required = false) List<Integer> quantities,
                           @RequestParam(value = "recipientName", required = false) String recipientName,
                           @RequestParam("shippingAddress") String shippingAddress,
                           @RequestParam(value = "recipientPhone", required = false) String recipientPhone,
                           @RequestParam(value = "notes", required = false) String notes,
                           @RequestParam(value = "paymentMethod", defaultValue = "WALLET") String paymentMethod,
                           @RequestParam(value = "voucherCode", required = false) String voucherCode,
                           @RequestParam(value = "installationMode", defaultValue = "SELF_INSTALL") String installationMode,
                           @RequestParam(value = "ghnDistrictId", required = false) Integer ghnDistrictId,
                           @RequestParam(value = "ghnWardCode", required = false) String ghnWardCode,
                           HttpServletRequest request,
                           Authentication auth,
                           RedirectAttributes redirectAttributes) {
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng đăng nhập để đặt mua vật tư trên VieShift Mall.");
            return "redirect:/login";
        }

        User user = userService.getByEmail(auth.getName());
        try {
            ShopOrder order = shopService.createStandaloneOrder(
                    user, productIds, quantities, recipientName, shippingAddress, recipientPhone,
                    notes, paymentMethod, voucherCode, installationMode, ghnDistrictId, ghnWardCode
            );

            if ("VNPAY".equalsIgnoreCase(order.getPaymentMethod())) {
                String vnpayUrl = vnPayService.createShopOrderPaymentUrl(order, request);
                return "redirect:" + vnpayUrl;
            }

            String formattedTotal = NumberFormat.getInstance(new Locale("vi", "VN")).format(order.getTotalAmount());
            String payNote = "COD".equalsIgnoreCase(order.getPaymentMethod())
                    ? "Thanh toán khi nhận hàng (COD: " + formattedTotal + " đ)"
                    : "Đã thanh toán " + formattedTotal + " đ từ Ví VieShift";
            String voucherNote = (order.getVoucherCode() != null)
                    ? " [Áp dụng mã " + order.getVoucherCode() + " giảm -" + NumberFormat.getInstance(new Locale("vi", "VN")).format(order.getDiscountAmount()) + " đ]"
                    : "";

            redirectAttributes.addFlashAttribute("successMessage",
                    "Đặt hàng thành công (Mã đơn #VS-MALL-" + order.getId() + ")! " + payNote + voucherNote
                            + " • Vận chuyển: " + order.getShippingMethodName() + " đến: " + order.getShippingAddress());
            return "redirect:/shop#my-orders";
        } catch (InsufficientBalanceException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage() + " (Hoặc bạn có thể chọn Thanh toán VNPAY Online / Tiền mặt COD).");
            return "redirect:/customer?tab=wallet";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể đặt hàng: " + ex.getMessage());
            return "redirect:/shop";
        }
    }

    /**
     * Thanh toán lại qua cổng VNPAY cho các đơn hàng PENDING_VNPAY trong vòng 30 phút (giống LTW_01 repay_vnpay)
     */
    @GetMapping("/orders/{id}/repay-vnpay")
    public String repayVnPay(@PathVariable("id") Long orderId,
                             HttpServletRequest request,
                             Authentication auth,
                             RedirectAttributes redirectAttributes) {
        if (auth == null || !auth.isAuthenticated()) {
            return "redirect:/login";
        }
        try {
            ShopOrder order = shopService.getOrderById(orderId);
            if (!"PENDING_VNPAY".equalsIgnoreCase(order.getStatus())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Đơn hàng này đã được thanh toán hoặc đã hủy.");
                return "redirect:/shop#my-orders";
            }
            if (Duration.between(order.getCreatedAt(), LocalDateTime.now()).toMinutes() > 30) {
                shopService.updateOrderStatus(orderId, "CANCELLED");
                redirectAttributes.addFlashAttribute("errorMessage", "Đã quá thời hạn 30 phút thanh toán VNPAY. Đơn hàng #VS-MALL-" + orderId + " đã tự động hủy.");
                return "redirect:/shop#my-orders";
            }
            String paymentUrl = vnPayService.createShopOrderPaymentUrl(order, request);
            return "redirect:" + paymentUrl;
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể tạo liên kết VNPAY: " + ex.getMessage());
            return "redirect:/shop#my-orders";
        }
    }

    @PostMapping("/orders/{id}/cancel")
    public String cancelOrder(@PathVariable("id") Long orderId,
                              Authentication auth,
                              RedirectAttributes redirectAttributes) {
        if (auth == null || !auth.isAuthenticated()) {
            return "redirect:/login";
        }
        User user = userService.getByEmail(auth.getName());
        try {
            List<ShopOrder> myOrders = shopService.getCustomerOrders(user.getId());
            ShopOrder target = myOrders.stream()
                    .filter(o -> o.getId().equals(orderId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng của bạn."));

            if (!"PAID".equalsIgnoreCase(target.getStatus())
                    && !"PENDING_COD".equalsIgnoreCase(target.getStatus())
                    && !"PENDING_VNPAY".equalsIgnoreCase(target.getStatus())) {
                throw new IllegalStateException("Chỉ có thể hủy đơn hàng khi đang ở trạng thái Chờ xử lý / Chờ giao.");
            }

            boolean wasPaid = "PAID".equalsIgnoreCase(target.getStatus());
            shopService.updateOrderStatus(orderId, "CANCELLED");

            String msg = wasPaid
                    ? "Đã hủy đơn hàng #VS-MALL-" + orderId + " và hoàn tiền 100% vào Ví VieShift của bạn!"
                    : "Đã hủy đơn hàng #VS-MALL-" + orderId + " thành công!";
            redirectAttributes.addFlashAttribute("successMessage", msg);
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi hủy đơn: " + ex.getMessage());
        }
        return "redirect:/shop#my-orders";
    }

    @PostMapping("/orders/{id}/confirm-received")
    public String confirmReceived(@PathVariable("id") Long orderId,
                                  Authentication auth,
                                  RedirectAttributes redirectAttributes) {
        if (auth == null || !auth.isAuthenticated()) {
            return "redirect:/login";
        }
        User user = userService.getByEmail(auth.getName());
        try {
            List<ShopOrder> myOrders = shopService.getCustomerOrders(user.getId());
            ShopOrder target = myOrders.stream()
                    .filter(o -> o.getId().equals(orderId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng của bạn."));

            if ("CANCELLED".equalsIgnoreCase(target.getStatus()) || "COMPLETED".equalsIgnoreCase(target.getStatus())) {
                throw new IllegalStateException("Đơn hàng này đã hoàn tất hoặc đã hủy.");
            }

            shopService.updateOrderStatus(orderId, "COMPLETED");
            redirectAttributes.addFlashAttribute("successMessage",
                    "Cảm ơn bạn đã xác nhận nhận hàng (#VS-MALL-" + orderId + ")! Bảo hành chính hãng 1 đổi 1 đã được kích hoạt.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi xác nhận đơn: " + ex.getMessage());
        }
        return "redirect:/shop#my-orders";
    }

    @GetMapping("/orders/{id}/invoice")
    public String orderInvoice(@PathVariable("id") Long orderId,
                               Authentication auth,
                               Model model) {
        if (auth == null || !auth.isAuthenticated()) {
            return "redirect:/login";
        }
        User user = userService.getByEmail(auth.getName());
        ShopOrder order = shopService.getOrderById(orderId);

        if (!order.getCustomer().getId().equals(user.getId()) && user.getRole() != Role.ADMIN) {
            throw new UnauthorizedActionException("Bạn không có quyền xem hóa đơn của đơn hàng này!");
        }

        model.addAttribute("order", order);
        return "shop-invoice";
    }
}
