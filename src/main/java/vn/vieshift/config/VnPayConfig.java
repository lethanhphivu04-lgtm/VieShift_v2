package vn.vieshift.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Configuration
public class VnPayConfig {

    @Value("${vieshift.vnpay.tmn-code:}")
    private String tmnCode;

    @Value("${vieshift.vnpay.hash-secret:}")
    private String hashSecret;

    @Value("${vieshift.vnpay.url:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}")
    private String payUrl;

    @Value("${vieshift.vnpay.return-url:http://localhost:8080/payment/vnpay-return}")
    private String returnUrl;

    public String getTmnCode() {
        return tmnCode;
    }

    public String getHashSecret() {
        return hashSecret;
    }

    public String getPayUrl() {
        return payUrl;
    }

    public String getReturnUrl() {
        return returnUrl;
    }

    /**
     * Tạo mã băm HMAC-SHA512 chuẩn VNPAY
     */
    public static String hmacSHA512(final String key, final String data) {
        try {
            if (key == null || data == null) {
                throw new NullPointerException("Key hoặc Data rỗng");
            }
            final Mac hmac512 = Mac.getInstance("HmacSHA512");
            byte[] hmacKeyBytes = key.getBytes(StandardCharsets.UTF_8);
            final SecretKeySpec secretKey = new SecretKeySpec(hmacKeyBytes, "HmacSHA512");
            hmac512.init(secretKey);
            byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
            byte[] result = hmac512.doFinal(dataBytes);
            StringBuilder sb = new StringBuilder(2 * result.length);
            for (byte b : result) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception ex) {
            return "";
        }
    }

    /**
     * Sắp xếp các trường và tạo chữ ký hash theo chuẩn VNPAY 2.1.0
     */
    public static String hashAllFields(Map<String, String> fields, String secretKey) {
        List<String> fieldNames = new ArrayList<>(fields.keySet());
        Collections.sort(fieldNames);
        StringBuilder sb = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = fields.get(fieldName);
            if ((fieldValue != null) && (!fieldValue.isEmpty())) {
                sb.append(fieldName);
                sb.append("=");
                try {
                    sb.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                } catch (Exception e) {
                    sb.append(fieldValue);
                }
                if (itr.hasNext()) {
                    sb.append("&");
                }
            }
        }
        return hmacSHA512(secretKey, sb.toString());
    }

    /**
     * Lấy IP Client thực tế của người dùng
     */
    public static String getIpAddress(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-FORWARDED-FOR");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }
        if (ipAddress != null && ipAddress.contains(",")) {
            ipAddress = ipAddress.split(",")[0].trim();
        }
        if ("0:0:0:0:0:0:0:1".equals(ipAddress) || "localhost".equalsIgnoreCase(ipAddress)) {
            ipAddress = "127.0.0.1";
        }
        return (ipAddress != null && !ipAddress.isBlank()) ? ipAddress : "127.0.0.1";
    }

    /**
     * Diễn giải mã phản hồi VNPAY sang tiếng Việt
     */
    public static String getResponseDescription(String responseCode) {
        if (responseCode == null) return "Không có thông tin phản hồi";
        return switch (responseCode) {
            case "00" -> "Giao dịch thành công";
            case "07" -> "Trừ tiền thành công. Giao dịch bị nghi ngờ (liên quan tới lừa đảo, giao dịch bất thường).";
            case "09" -> "Thẻ/Tài khoản chưa đăng ký dịch vụ Internet Banking tại ngân hàng.";
            case "10" -> "Khách hàng xác thực thông tin thẻ/tài khoản không đúng quá 3 lần.";
            case "11" -> "Đã hết hạn chờ thanh toán. Vui lòng thực hiện lại giao dịch.";
            case "12" -> "Thẻ/Tài khoản của bạn đã bị khóa.";
            case "13" -> "Quý khách nhập sai mật khẩu xác thực OTP. Vui lòng thực hiện lại.";
            case "24" -> "Giao dịch đã bị hủy bởi người dùng.";
            case "51" -> "Tài khoản của quý khách không đủ số dư để thực hiện giao dịch.";
            case "65" -> "Tài khoản của quý khách đã vượt quá hạn mức giao dịch trong ngày.";
            case "75" -> "Ngân hàng thanh toán đang bảo trì hệ thống.";
            case "79" -> "Quý khách nhập sai mật khẩu thanh toán quá số lần quy định.";
            default -> "Giao dịch không thành công (Mã phản hồi: " + responseCode + ").";
        };
    }
}
