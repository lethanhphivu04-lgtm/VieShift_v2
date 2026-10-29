package vn.vieshift.controller.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.vieshift.entity.DiscountType;
import vn.vieshift.entity.Voucher;
import vn.vieshift.service.VoucherService;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/vouchers")
public class VoucherApiController {

    private final VoucherService voucherService;

    public VoucherApiController(VoucherService voucherService) {
        this.voucherService = voucherService;
    }

    /**
     * API kiểm tra và áp dụng mã voucher ưu đãi (Hỗ trợ cả Query Params và JSON Body)
     */
    @PostMapping("/apply")
    public ResponseEntity<Map<String, Object>> applyVoucher(
            @RequestParam(value = "code", required = false) String codeParam,
            @RequestParam(value = "orderAmount", required = false) BigDecimal orderAmountParam,
            @RequestBody(required = false) Map<String, Object> body) {

        String code = codeParam;
        BigDecimal orderAmount = orderAmountParam;

        if (body != null) {
            if ((code == null || code.isBlank()) && body.containsKey("code") && body.get("code") != null) {
                code = String.valueOf(body.get("code"));
            }
            if (orderAmount == null && body.containsKey("orderAmount") && body.get("orderAmount") != null) {
                try {
                    orderAmount = new BigDecimal(String.valueOf(body.get("orderAmount")));
                } catch (Exception ignored) {}
            }
        }

        if (orderAmount == null) {
            orderAmount = BigDecimal.ZERO;
        }

        Map<String, Object> response = new HashMap<>();

        if (code == null || code.isBlank()) {
            response.put("valid", false);
            response.put("message", "Vui lòng nhập mã ưu đãi.");
            return ResponseEntity.ok(response);
        }

        Optional<Voucher> voucherOpt = voucherService.validateVoucher(code, orderAmount);

        if (voucherOpt.isEmpty()) {
            response.put("valid", false);
            response.put("message", "Mã ưu đãi không tồn tại, đã hết hạn, hết lượt dùng hoặc chưa đạt giá trị đơn tối thiểu.");
            return ResponseEntity.ok(response);
        }

        Voucher v = voucherOpt.get();
        BigDecimal discount = v.calculateDiscount(orderAmount);
        BigDecimal finalAmount = orderAmount.subtract(discount);
        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
            finalAmount = BigDecimal.ZERO;
        }

        response.put("valid", true);
        response.put("code", v.getCode());
        response.put("title", v.getTitle());
        response.put("discountType", v.getDiscountType().name());
        response.put("discountValue", v.getDiscountValue());
        response.put("discountAmount", discount);
        response.put("finalAmount", finalAmount);
        response.put("message", "Áp dụng thành công mã " + v.getCode() + "!");

        return ResponseEntity.ok(response);
    }

    /**
     * Lấy danh sách voucher công khai để khách hàng bấm chọn nhanh
     */
    @GetMapping("/public")
    public ResponseEntity<List<Map<String, Object>>> getPublicVouchers() {
        List<Voucher> list = voucherService.getPublicActiveVouchers();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        List<Map<String, Object>> dtos = list.stream().map(v -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", v.getId());
            m.put("code", v.getCode());
            m.put("title", v.getTitle());
            m.put("description", v.getDescription());
            m.put("discountType", v.getDiscountType().name());
            m.put("discountTypeDisplay", v.getDiscountType().getDisplayName());
            m.put("discountValue", v.getDiscountValue());
            m.put("maxDiscountAmount", v.getMaxDiscountAmount());
            m.put("minOrderAmount", v.getMinOrderAmount());
            m.put("endDate", v.getEndDate() != null ? v.getEndDate().format(dtf) : "Không giới hạn");
            return m;
        }).toList();

        return ResponseEntity.ok(dtos);
    }
}
