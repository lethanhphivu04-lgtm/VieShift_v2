package vn.vieshift.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.vieshift.dto.PaymentResult;
import vn.vieshift.entity.PaymentStatus;
import vn.vieshift.entity.PaymentTransaction;
import vn.vieshift.entity.User;
import vn.vieshift.entity.Wallet;
import vn.vieshift.repository.PaymentTransactionRepository;
import vn.vieshift.service.UserService;
import vn.vieshift.service.VnPayService;
import vn.vieshift.service.WalletService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Random;

@Controller
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private final VnPayService vnPayService;
    private final UserService userService;
    private final WalletService walletService;
    private final PaymentTransactionRepository paymentTransactionRepository;

    public PaymentController(VnPayService vnPayService,
                             UserService userService,
                             WalletService walletService,
                             PaymentTransactionRepository paymentTransactionRepository) {
        this.vnPayService = vnPayService;
        this.userService = userService;
        this.walletService = walletService;
        this.paymentTransactionRepository = paymentTransactionRepository;
    }

    /**
     * Nạp tiền mô phỏng Sandbox (Bypass lỗi VNPAY Gateway ngoại vi)
     */
    @PostMapping("/customer/wallet/vnpay-simulate")
    public String simulateVnPayDeposit(@RequestParam("amount") BigDecimal amount,
                                       Authentication auth,
                                       RedirectAttributes redirectAttributes) {
        if (auth == null) {
            return "redirect:/login";
        }

        try {
            User customer = userService.getByEmail(auth.getName());
            if (amount == null || amount.compareTo(BigDecimal.valueOf(10000)) < 0) {
                redirectAttributes.addFlashAttribute("errorMessage", "Số tiền nạp tối thiểu là 10.000 VNĐ");
                return "redirect:/customer?tab=wallet";
            }

            String txnRef = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHHmmss")) + String.format("%04d", new Random().nextInt(10000));
            PaymentTransaction transaction = new PaymentTransaction(txnRef, customer, amount, "Nạp tiền ví VieShift qua VNPAY Sandbox (Mô phỏng)", "NCB");
            transaction.setStatus(PaymentStatus.SUCCESS);
            transaction.setVnpTransactionNo("MOCK" + System.currentTimeMillis());
            transaction.setResponseCode("00");
            paymentTransactionRepository.save(transaction);

            // Nạp tiền vào ví
            walletService.deposit(customer.getId(), amount, "Nạp tiền ví VieShift qua VNPAY Sandbox (Thử nghiệm thành công)");

            log.info("Mô phỏng thanh toán VNPAY thành công cho User #{} - Số tiền: {} VND - TxnRef: {}", customer.getId(), amount, txnRef);
            return "redirect:/payment/vnpay-return?mock=true&vnp_TxnRef=" + txnRef;
        } catch (Exception ex) {
            log.error("Lỗi mô phỏng nạp tiền: ", ex);
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi nạp tiền thử nghiệm: " + ex.getMessage());
            return "redirect:/customer?tab=wallet";
        }
    }

    /**
     * Tạo URL và chuyển hướng người dùng sang cổng thanh toán VNPAY
     */
    @PostMapping("/customer/wallet/vnpay-deposit")
    public String createVnPayDeposit(@RequestParam("amount") BigDecimal amount,
                                     @RequestParam(value = "bankCode", required = false) String bankCode,
                                     Authentication auth,
                                     HttpServletRequest request,
                                     RedirectAttributes redirectAttributes) {
        if (auth == null) {
            return "redirect:/login";
        }

        try {
            User customer = userService.getByEmail(auth.getName());
            if (amount == null || amount.compareTo(BigDecimal.valueOf(10000)) < 0) {
                redirectAttributes.addFlashAttribute("errorMessage", "Số tiền nạp tối thiểu là 10.000 VNĐ");
                return "redirect:/customer?tab=wallet";
            }

            String paymentUrl = vnPayService.createDepositPaymentUrl(customer, amount, bankCode, request);
            log.info("Chuyển hướng User #{} sang cổng VNPAY: {}", customer.getId(), paymentUrl);
            return "redirect:" + paymentUrl;
        } catch (Exception ex) {
            log.error("Lỗi khởi tạo thanh toán VNPAY: ", ex);
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể tạo liên kết thanh toán VNPAY: " + ex.getMessage());
            return "redirect:/customer?tab=wallet";
        }
    }

    /**
     * Nhận kết quả phản hồi từ VNPAY qua Return URL (trên trình duyệt)
     */
    @GetMapping("/payment/vnpay-return")
    public String paymentReturn(@RequestParam Map<String, String> allParams, Model model) {
        log.info("Khách hàng quay về từ VNPAY với params: {}", allParams);

        if ("true".equalsIgnoreCase(allParams.get("mock"))) {
            String txnRef = allParams.get("vnp_TxnRef");
            PaymentTransaction tx = paymentTransactionRepository.findByTxnRef(txnRef).orElse(null);
            PaymentResult mockResult = new PaymentResult(
                    true,
                    txnRef,
                    tx != null ? tx.getAmount() : BigDecimal.ZERO,
                    "Nạp tiền ví VieShift qua VNPAY Sandbox",
                    "NCB",
                    tx != null ? tx.getVnpTransactionNo() : "MOCK123456",
                    "00",
                    "Giao dịch nạp tiền thành công (Mô phỏng Sandbox)",
                    LocalDateTime.now()
            );
            model.addAttribute("result", mockResult);
            model.addAttribute("transaction", tx);
            if (tx != null && tx.getUser() != null) {
                Wallet wallet = walletService.getOrCreateWallet(tx.getUser());
                model.addAttribute("wallet", wallet);
                model.addAttribute("customer", tx.getUser());
            }
            return "payment/result";
        }

        PaymentResult result = vnPayService.handleReturn(allParams);
        PaymentTransaction transaction = vnPayService.findByTxnRef(result.getTxnRef());

        model.addAttribute("result", result);
        model.addAttribute("transaction", transaction);

        if (transaction != null && transaction.getUser() != null) {
            Wallet wallet = walletService.getOrCreateWallet(transaction.getUser());
            model.addAttribute("wallet", wallet);
            model.addAttribute("customer", transaction.getUser());
        }

        return "payment/result";
    }

    /**
     * Webhook IPN từ máy chủ VNPAY
     */
    @GetMapping("/payment/vnpay-ipn")
    @ResponseBody
    public ResponseEntity<Map<String, String>> paymentIpn(@RequestParam Map<String, String> allParams) {
        log.info("VNPAY gọi IPN webhook với params: {}", allParams);
        Map<String, String> response = vnPayService.handleIpn(allParams);
        return ResponseEntity.ok(response);
    }
}
