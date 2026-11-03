package vn.vieshift.service;

import jakarta.servlet.http.HttpServletRequest;
import vn.vieshift.dto.PaymentResult;
import vn.vieshift.entity.PaymentTransaction;
import vn.vieshift.entity.ShopOrder;
import vn.vieshift.entity.User;

import java.math.BigDecimal;
import java.util.Map;

public interface VnPayService {

    /**
     * Tạo URL thanh toán VNPAY (sandbox) cho giao dịch nạp tiền ví
     */
    String createDepositPaymentUrl(User user, BigDecimal amount, String bankCode, HttpServletRequest request);

    /**
     * Tạo URL thanh toán VNPAY trực tiếp cho Đơn hàng Vật tư VieShift Mall (giống LTW_01)
     */
    String createShopOrderPaymentUrl(ShopOrder order, HttpServletRequest request);

    /**
     * Xử lý callback Return URL từ trình duyệt khách hàng sau khi thanh toán xong trên VNPAY
     *
     * @param params toàn bộ query parameters trả về từ VNPAY
     * @return PaymentResult mang trạng thái giao dịch, thông tin đối soát và số dư ví mới
     */
    PaymentResult handleReturn(Map<String, String> params);

    /**
     * Xử lý IPN (Instant Payment Notification) từ server VNPAY
     *
     * @param params tham số gửi từ server VNPAY
     * @return Map chứa RspCode và Message theo chuẩn VNPAY
     */
    Map<String, String> handleIpn(Map<String, String> params);

    /**
     * Tìm giao dịch theo mã TxnRef
     */
    PaymentTransaction findByTxnRef(String txnRef);
}
