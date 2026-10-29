package vn.vieshift.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.vieshift.entity.Voucher;
import vn.vieshift.repository.VoucherRepository;
import vn.vieshift.service.VoucherService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class VoucherServiceImpl implements VoucherService {

    private final VoucherRepository voucherRepository;

    public VoucherServiceImpl(VoucherRepository voucherRepository) {
        this.voucherRepository = voucherRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Voucher> validateVoucher(String code, BigDecimal orderAmount) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        Optional<Voucher> opt = voucherRepository.findByCodeIgnoreCase(code.trim());
        if (opt.isPresent()) {
            Voucher v = opt.get();
            if (v.isValidForOrder(orderAmount)) {
                return Optional.of(v);
            }
        }
        return Optional.empty();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Voucher> getPublicActiveVouchers() {
        LocalDateTime now = LocalDateTime.now();
        return voucherRepository.findByActiveTrueAndIsPublicTrueOrderByCreatedAtDesc().stream()
                .filter(v -> (v.getStartDate() == null || !now.isBefore(v.getStartDate()))
                          && (v.getEndDate() == null || !now.isAfter(v.getEndDate()))
                          && (v.getMaxUses() == null || v.getCurrentUses() < v.getMaxUses()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Voucher> getAllVouchers() {
        return voucherRepository.findAllByOrderByCreatedAtDesc();
    }

    @Override
    public Voucher createVoucher(Voucher voucher) {
        if (voucher.getCode() != null) {
            voucher.setCode(voucher.getCode().trim().toUpperCase());
        }
        if (voucher.getMinOrderAmount() == null) {
            voucher.setMinOrderAmount(BigDecimal.ZERO);
        }
        if (voucher.getCurrentUses() == null) {
            voucher.setCurrentUses(0);
        }
        return voucherRepository.save(voucher);
    }

    @Override
    public Voucher togglePublic(Long voucherId) {
        Voucher v = voucherRepository.findById(voucherId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy voucher ID: " + voucherId));
        v.setPublic(!v.isPublic());
        return voucherRepository.save(v);
    }

    @Override
    public Voucher toggleActive(Long voucherId) {
        Voucher v = voucherRepository.findById(voucherId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy voucher ID: " + voucherId));
        v.setActive(!v.isActive());
        return voucherRepository.save(v);
    }

    @Override
    public void recordVoucherUsage(String code) {
        if (code == null || code.isBlank()) return;
        voucherRepository.findByCodeIgnoreCase(code.trim()).ifPresent(v -> {
            v.setCurrentUses(v.getCurrentUses() + 1);
            voucherRepository.save(v);
        });
    }

    @Override
    public void deleteVoucher(Long voucherId) {
        voucherRepository.deleteById(voucherId);
    }
}
