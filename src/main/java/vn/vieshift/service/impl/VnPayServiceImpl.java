package vn.vieshift.service.impl;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.vieshift.config.VnPayConfig;
import vn.vieshift.dto.PaymentResult;
import vn.vieshift.entity.PaymentStatus;
import vn.vieshift.entity.PaymentTransaction;
import vn.vieshift.entity.User;
import vn.vieshift.repository.PaymentTransactionRepository;
import vn.vieshift.service.VnPayService;
import vn.vieshift.service.WalletService;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class VnPayServiceImpl implements VnPayService {

    private static final Logger log = LoggerFactory.getLogger(VnPayServiceImpl.class);
    private static final DateTimeFormatter VNP_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final VnPayConfig vnPayConfig;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final WalletService walletService;

    public VnPayServiceImpl(VnPayConfig vnPayConfig,
                            PaymentTransactionRepository paymentTransactionRepository,
                            WalletService walletService) {
        this.vnPayConfig = vnPayConfig;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.walletService = walletService;
    }

    @Override
    @Transactional
    public String createDepositPaymentUrl(User user, BigDecimal amount, String bankCode, HttpServletRequest request) {
        if (amount == null || amount.compareTo(BigDecimal.valueOf(10000)) < 0) {
            throw new IllegalArgumentException("Số tiền nạp tối thiểu là 10.000 VNĐ");
        }

        LocalDateTime now = LocalDateTime.now(VIETNAM_ZONE);
        String vnpCreateDate = now.format(VNP_DATE_FORMATTER);
        String vnpExpireDate = now.plusMinutes(15).format(VNP_DATE_FORMATTER);

        // Tạo txnRef duy nhất: yyyyMMddHHmmss + 3 số ngẫu nhiên (17 ký tự)
        String txnRef = now.format(DateTimeFormatter.ofPattern("yyMMddHHmmss")) + String.format("%04d", new Random().nextInt(10000));

        // Lưu thông tin giao dịch khởi tạo ở trạng thái PENDING
        String orderInfo = "Nap tien vi VieShift " + txnRef;
        PaymentTransaction transaction = new PaymentTransaction(txnRef, user, amount, orderInfo, bankCode);
        paymentTransactionRepository.save(transaction);

        // Chuẩn bị các tham số gửi sang cổng VNPAY
        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_Version", "2.1.0");
        vnpParams.put("vnp_Command", "pay");
        vnpParams.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        // Số tiền VNPAY nhân 100
        long vnpAmount = amount.multiply(BigDecimal.valueOf(100)).longValue();
        vnpParams.put("vnp_Amount", String.valueOf(vnpAmount));
        vnpParams.put("vnp_CurrCode", "VND");
        vnpParams.put("vnp_TxnRef", txnRef);
        vnpParams.put("vnp_OrderInfo", orderInfo);
        vnpParams.put("vnp_OrderType", "topup");
        vnpParams.put("vnp_Locale", "vn");
        vnpParams.put("vnp_ReturnUrl", vnPayConfig.getReturnUrl());
        vnpParams.put("vnp_IpAddr", VnPayConfig.getIpAddress(request));
        vnpParams.put("vnp_CreateDate", vnpCreateDate);
        vnpParams.put("vnp_ExpireDate", vnpExpireDate);

        if (bankCode != null && !bankCode.isBlank() && !"ALL".equalsIgnoreCase(bankCode)) {
            vnpParams.put("vnp_BankCode", bankCode.trim());
        }

        // Sắp xếp tham số theo thứ tự ASCII
        List<String> fieldNames = new ArrayList<>(vnpParams.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();

        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = vnpParams.get(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                try {
                    // hashData
                    hashData.append(fieldName).append('=').append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                    // query
                    query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()))
                            .append('=')
                            .append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                } catch (Exception e) {
                    log.error("Lỗi URL encode tham số VNPAY: {}", fieldName, e);
                }
                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }

        String secureHash = VnPayConfig.hmacSHA512(vnPayConfig.getHashSecret(), hashData.toString());
        String paymentUrl = vnPayConfig.getPayUrl() + "?" + query.toString() + "&vnp_SecureHash=" + secureHash;

        log.info("Khởi tạo VNPAY payment url cho User #{} - Số tiền: {} VND - TxnRef: {}", user.getId(), amount, txnRef);
        return paymentUrl;
    }

    @Override
    @Transactional
    public PaymentResult handleReturn(Map<String, String> params) {
        log.info("Nhận callback Return URL từ VNPAY: {}", params);

        Map<String, String> fields = new HashMap<>(params);
        String vnpSecureHash = fields.remove("vnp_SecureHash");
        fields.remove("vnp_SecureHashType");

        String calculatedHash = VnPayConfig.hashAllFields(fields, vnPayConfig.getHashSecret());
        String txnRef = params.get("vnp_TxnRef");
        String responseCode = params.get("vnp_ResponseCode");
        String transactionNo = params.get("vnp_TransactionNo");
        String bankCode = params.get("vnp_BankCode");
        String orderInfo = params.get("vnp_OrderInfo");
        String amountStr = params.get("vnp_Amount");
        String payDateStr = params.get("vnp_PayDate");

        BigDecimal amount = BigDecimal.ZERO;
        if (amountStr != null && !amountStr.isBlank()) {
            try {
                amount = new BigDecimal(amountStr).divide(BigDecimal.valueOf(100));
            } catch (Exception ignored) {}
        }

        LocalDateTime payDate = LocalDateTime.now();
        if (payDateStr != null && payDateStr.length() == 14) {
            try {
                payDate = LocalDateTime.parse(payDateStr, VNP_DATE_FORMATTER);
            } catch (Exception ignored) {}
        }

        // Kiểm tra chữ ký bảo mật
        if (vnpSecureHash == null || !vnpSecureHash.equalsIgnoreCase(calculatedHash)) {
            log.warn("Chữ ký VNPAY Return URL không hợp lệ! txnRef={}", txnRef);
            return new PaymentResult(false, txnRef, amount, orderInfo, bankCode, transactionNo,
                    responseCode != null ? responseCode : "97",
                    "Chữ ký bảo mật không khớp hoặc giao dịch không hợp lệ", payDate);
        }

        PaymentTransaction transaction = paymentTransactionRepository.findByTxnRef(txnRef).orElse(null);
        if (transaction == null) {
            log.warn("Không tìm thấy giao dịch với mã txnRef={}", txnRef);
            return new PaymentResult(false, txnRef, amount, orderInfo, bankCode, transactionNo,
                    "01", "Không tìm thấy đơn hàng trong hệ thống", payDate);
        }

        transaction.setVnpTransactionNo(transactionNo);
        transaction.setResponseCode(responseCode);
        if (bankCode != null && !bankCode.isBlank()) {
            transaction.setBankCode(bankCode);
        }

        boolean isSuccess = "00".equals(responseCode);
        String messageDescription = VnPayConfig.getResponseDescription(responseCode);

        if (isSuccess) {
            if (transaction.getStatus() != PaymentStatus.SUCCESS) {
                transaction.setStatus(PaymentStatus.SUCCESS);
                paymentTransactionRepository.save(transaction);

                // Cộng tiền vào ví khách hàng
                walletService.deposit(
                        transaction.getUser().getId(),
                        transaction.getAmount(),
                        "Nạp tiền qua cổng VNPAY #" + txnRef + " (" + (bankCode != null ? bankCode : "NCB") + ")"
                );
                log.info("Giao dịch VNPAY thành công, đã cộng {} VND vào ví User #{}", transaction.getAmount(), transaction.getUser().getId());
            }
        } else if ("24".equals(responseCode)) {
            if (transaction.getStatus() != PaymentStatus.SUCCESS) {
                transaction.setStatus(PaymentStatus.CANCELLED);
                paymentTransactionRepository.save(transaction);
            }
        } else {
            if (transaction.getStatus() != PaymentStatus.SUCCESS) {
                transaction.setStatus(PaymentStatus.FAILED);
                paymentTransactionRepository.save(transaction);
            }
        }

        return new PaymentResult(isSuccess, txnRef, transaction.getAmount(), orderInfo,
                bankCode, transactionNo, responseCode, messageDescription, payDate);
    }

    @Override
    @Transactional
    public Map<String, String> handleIpn(Map<String, String> params) {
        log.info("Nhận IPN webhook từ VNPAY: {}", params);
        Map<String, String> response = new HashMap<>();

        Map<String, String> fields = new HashMap<>(params);
        String vnpSecureHash = fields.remove("vnp_SecureHash");
        fields.remove("vnp_SecureHashType");

        String calculatedHash = VnPayConfig.hashAllFields(fields, vnPayConfig.getHashSecret());
        String txnRef = params.get("vnp_TxnRef");
        String responseCode = params.get("vnp_ResponseCode");
        String transactionNo = params.get("vnp_TransactionNo");
        String bankCode = params.get("vnp_BankCode");
        String amountStr = params.get("vnp_Amount");

        if (vnpSecureHash == null || !vnpSecureHash.equalsIgnoreCase(calculatedHash)) {
            log.warn("IPN Checksum không hợp lệ! txnRef={}", txnRef);
            response.put("RspCode", "97");
            response.put("Message", "Invalid Checksum");
            return response;
        }

        PaymentTransaction transaction = paymentTransactionRepository.findByTxnRef(txnRef).orElse(null);
        if (transaction == null) {
            response.put("RspCode", "01");
            response.put("Message", "Order not found");
            return response;
        }

        // Kiểm tra số tiền khớp
        if (amountStr != null) {
            long receivedAmount = Long.parseLong(amountStr);
            long expectedAmount = transaction.getAmount().multiply(BigDecimal.valueOf(100)).longValue();
            if (receivedAmount != expectedAmount) {
                response.put("RspCode", "04");
                response.put("Message", "Invalid Amount");
                return response;
            }
        }

        // Kiểm tra nếu đơn đã xử lý thành công trước đó (idempotency)
        if (transaction.getStatus() == PaymentStatus.SUCCESS) {
            response.put("RspCode", "02");
            response.put("Message", "Order already confirmed");
            return response;
        }

        transaction.setVnpTransactionNo(transactionNo);
        transaction.setResponseCode(responseCode);
        if (bankCode != null && !bankCode.isBlank()) {
            transaction.setBankCode(bankCode);
        }

        if ("00".equals(responseCode)) {
            transaction.setStatus(PaymentStatus.SUCCESS);
            paymentTransactionRepository.save(transaction);

            // Nạp tiền vào ví
            walletService.deposit(
                    transaction.getUser().getId(),
                    transaction.getAmount(),
                    "Nạp tiền qua cổng VNPAY IPN #" + txnRef
            );
            log.info("IPN: Đã xác nhận thành công và cộng tiền cho User #{}", transaction.getUser().getId());
        } else if ("24".equals(responseCode)) {
            transaction.setStatus(PaymentStatus.CANCELLED);
            paymentTransactionRepository.save(transaction);
        } else {
            transaction.setStatus(PaymentStatus.FAILED);
            paymentTransactionRepository.save(transaction);
        }

        response.put("RspCode", "00");
        response.put("Message", "Confirm Success");
        return response;
    }

    @Override
    public PaymentTransaction findByTxnRef(String txnRef) {
        return paymentTransactionRepository.findByTxnRef(txnRef).orElse(null);
    }
}
