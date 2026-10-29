package vn.vieshift.service;

import vn.vieshift.entity.Voucher;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface VoucherService {

    /**
     * Kiểm tra tính hợp lệ và trả về voucher đã tính số tiền giảm
     */
    Optional<Voucher> validateVoucher(String code, BigDecimal orderAmount);

    /**
     * Lấy danh sách voucher công khai đang hoạt động (dành cho người dùng chọn 1-chạm)
     */
    List<Voucher> getPublicActiveVouchers();

    /**
     * Lấy tất cả voucher cho quản trị viên
     */
    List<Voucher> getAllVouchers();

    /**
     * Tạo mới voucher
     */
    Voucher createVoucher(Voucher voucher);

    /**
     * Bật/Tắt chế độ công khai (isPublic: true = hiện cho người dùng chọn, false = ẩn, khách phải tự nhập code)
     */
    Voucher togglePublic(Long voucherId);

    /**
     * Bật/Tắt trạng thái hoạt động (active)
     */
    Voucher toggleActive(Long voucherId);

    /**
     * Ghi nhận 1 lượt sử dụng voucher khi đơn hàng tạo thành công
     */
    void recordVoucherUsage(String code);

    /**
     * Xóa voucher
     */
    void deleteVoucher(Long voucherId);
}
