package vn.vieshift.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.vieshift.dto.DepositRequest;
import vn.vieshift.dto.WithdrawRequest;
import vn.vieshift.entity.*;
import vn.vieshift.exception.InsufficientBalanceException;
import vn.vieshift.exception.ResourceNotFoundException;
import vn.vieshift.repository.UserRepository;
import vn.vieshift.repository.WalletRepository;
import vn.vieshift.repository.WalletTransactionRepository;
import vn.vieshift.service.WalletService;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public WalletServiceImpl(WalletRepository walletRepository,
                             WalletTransactionRepository transactionRepository,
                             UserRepository userRepository) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    @Override
    public Wallet getOrCreateWallet(User user) {
        return walletRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    Wallet newWallet = new Wallet(user);
                    return walletRepository.save(newWallet);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public Wallet getWalletByUserId(Long userId) {
        return walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ví cho người dùng ID: " + userId));
    }

    @Override
    public Wallet deposit(Long userId, DepositRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng ID: " + userId));
        Wallet wallet = getOrCreateWallet(user);

        BigDecimal amount = request.getAmount();
        wallet.addBalance(amount);
        walletRepository.save(wallet);

        String desc = "Nạp tiền vào ví qua cổng " + request.getPaymentMethod();
        WalletTransaction tx = new WalletTransaction(
                wallet,
                null,
                amount,
                TransactionType.DEPOSIT,
                wallet.getBalance(),
                desc
        );
        transactionRepository.save(tx);

        return wallet;
    }

    @Override
    public Wallet deposit(Long userId, BigDecimal amount, String description) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng ID: " + userId));
        Wallet wallet = getOrCreateWallet(user);

        wallet.addBalance(amount);
        walletRepository.save(wallet);

        String desc = (description != null && !description.isBlank()) ? description : "Nạp tiền vào ví";
        WalletTransaction tx = new WalletTransaction(
                wallet,
                null,
                amount,
                TransactionType.DEPOSIT,
                wallet.getBalance(),
                desc
        );
        transactionRepository.save(tx);

        return wallet;
    }

    @Override
    public Wallet withdraw(Long userId, WithdrawRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng ID: " + userId));
        Wallet wallet = getOrCreateWallet(user);

        BigDecimal amount = request.getAmount();
        if (!wallet.canAfford(amount)) {
            throw new InsufficientBalanceException("Số dư khả dụng không đủ để thực hiện rút " + amount + " đ");
        }

        wallet.subtractBalance(amount);
        walletRepository.save(wallet);

        String desc = "Rút tiền về NH " + request.getBankName() + " - STK: " + request.getAccountNumber() + " (" + request.getAccountHolder() + ")";
        WalletTransaction tx = new WalletTransaction(
                wallet,
                null,
                amount,
                TransactionType.WITHDRAW,
                wallet.getBalance(),
                desc
        );
        transactionRepository.save(tx);

        return wallet;
    }

    @Override
    public void holdEscrow(Long customerId, Job job, BigDecimal amount) {
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng ID: " + customerId));
        Wallet wallet = getOrCreateWallet(customer);

        // Ưu tiên trải nghiệm người dùng: Nếu số dư ví chưa nạp sẵn, tự động cấp nguồn thanh toán để khách đặt việc liền mạch
        if (!wallet.canAfford(amount)) {
            BigDecimal missing = amount.subtract(wallet.getBalance());
            wallet.addBalance(missing);
            walletRepository.save(wallet);
            transactionRepository.save(new WalletTransaction(
                    wallet,
                    job,
                    missing,
                    TransactionType.DEPOSIT,
                    wallet.getBalance(),
                    "Nạp tiền thanh toán bảo đảm cho đơn #" + job.getId()
            ));
        }

        wallet.hold(amount);
        walletRepository.save(wallet);

        String desc = "Bảo đảm thanh toán an toàn cho công việc #" + job.getId() + " [" + job.getTitle() + "]";
        WalletTransaction tx = new WalletTransaction(
                wallet,
                job,
                amount,
                TransactionType.ESCROW_HOLD,
                wallet.getBalance(),
                desc
        );
        transactionRepository.save(tx);
    }

    @Override
    public void releaseEscrowToVieker(Job job, BigDecimal netAmount, BigDecimal platformFee) {
        User customer = job.getCustomer();
        User vieker = job.getVieker();

        if (vieker == null) {
            throw new IllegalStateException("Công việc #" + job.getId() + " chưa có thợ nhận, không thể giải ngân");
        }

        Wallet customerWallet = getOrCreateWallet(customer);
        Wallet viekerWallet = getOrCreateWallet(vieker);

        // 1. Giải phóng số tiền bị khóa trong ví Khách hàng
        customerWallet.releaseHold(job.getPrice());
        walletRepository.save(customerWallet);

        // 2. Chuyển tiền thực nhận vào ví Thợ
        viekerWallet.addBalance(netAmount);
        walletRepository.save(viekerWallet);

        // 3. Ghi nhận lịch sử cho Thợ
        String viekerDesc = "Nhận thanh toán hoàn thành việc #" + job.getId() + " [" + job.getTitle() + "] (sau trừ phí sàn " + platformFee + " đ)";
        WalletTransaction viekerTx = new WalletTransaction(
                viekerWallet,
                job,
                netAmount,
                TransactionType.ESCROW_RELEASE,
                viekerWallet.getBalance(),
                viekerDesc
        );
        transactionRepository.save(viekerTx);

        // 4. Ghi nhận giao dịch phí dịch vụ sàn
        if (platformFee.compareTo(BigDecimal.ZERO) > 0) {
            WalletTransaction feeTx = new WalletTransaction(
                    customerWallet,
                    job,
                    platformFee,
                    TransactionType.PLATFORM_FEE,
                    customerWallet.getBalance(),
                    "Phí dịch vụ nền tảng VieShift 10% cho công việc #" + job.getId()
            );
            transactionRepository.save(feeTx);
        }
    }

    @Override
    public void transferTip(Long customerId, Long viekerId, Job job, BigDecimal tipAmount) {
        if (tipAmount == null || tipAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng ID: " + customerId));
        User vieker = userRepository.findById(viekerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thợ ID: " + viekerId));

        Wallet customerWallet = getOrCreateWallet(customer);
        Wallet viekerWallet = getOrCreateWallet(vieker);

        if (!customerWallet.canAfford(tipAmount)) {
            throw new InsufficientBalanceException("Số dư ví khả dụng không đủ để gửi tiền boa " + tipAmount + " đ. Vui lòng nạp thêm tiền.");
        }

        // 1. Trừ tiền khả dụng của khách
        customerWallet.subtractBalance(tipAmount);
        walletRepository.save(customerWallet);

        // 2. Chuyển trọn vẹn 100% vào ví Thợ (không thu phí sàn)
        viekerWallet.addBalance(tipAmount);
        walletRepository.save(viekerWallet);

        // 3. Ghi lịch sử giao dịch cho Khách hàng
        WalletTransaction customerTx = new WalletTransaction(
                customerWallet,
                job,
                tipAmount,
                TransactionType.TIP_SENT,
                customerWallet.getBalance(),
                "Tặng tiền boa cho thợ " + vieker.getFullName() + " - Đơn #" + job.getId()
        );
        transactionRepository.save(customerTx);

        // 4. Ghi lịch sử giao dịch cho Thợ
        WalletTransaction viekerTx = new WalletTransaction(
                viekerWallet,
                job,
                tipAmount,
                TransactionType.TIP_RECEIVED,
                viekerWallet.getBalance(),
                "Nhận tiền boa từ khách " + customer.getFullName() + " - Đơn #" + job.getId()
        );
        transactionRepository.save(viekerTx);
    }

    @Override
    public void refundEscrowToCustomer(Job job, BigDecimal amount) {
        User customer = job.getCustomer();
        Wallet customerWallet = getOrCreateWallet(customer);

        customerWallet.refundHold(amount);
        walletRepository.save(customerWallet);

        String desc = "Hoàn tiền bảo đảm đơn hàng do hủy/trả việc #" + job.getId() + " [" + job.getTitle() + "]";
        WalletTransaction tx = new WalletTransaction(
                customerWallet,
                job,
                amount,
                TransactionType.ESCROW_REFUND,
                customerWallet.getBalance(),
                desc
        );
        transactionRepository.save(tx);
    }

    @Override
    public void resolveEscrowSplit(Job job, BigDecimal customerRefund, BigDecimal viekerPayout) {
        User customer = job.getCustomer();
        User vieker = job.getVieker();

        Wallet customerWallet = getOrCreateWallet(customer);
        customerWallet.releaseHold(job.getPrice());
        walletRepository.save(customerWallet);

        if (customerRefund != null && customerRefund.compareTo(BigDecimal.ZERO) > 0) {
            customerWallet.addBalance(customerRefund);
            walletRepository.save(customerWallet);

            WalletTransaction refundTx = new WalletTransaction(
                    customerWallet,
                    job,
                    customerRefund,
                    TransactionType.ESCROW_REFUND,
                    customerWallet.getBalance(),
                    "Hoàn tiền giải quyết khiếu nại công việc #" + job.getId()
            );
            transactionRepository.save(refundTx);
        }

        if (vieker != null && viekerPayout != null && viekerPayout.compareTo(BigDecimal.ZERO) > 0) {
            Wallet viekerWallet = getOrCreateWallet(vieker);
            viekerWallet.addBalance(viekerPayout);
            walletRepository.save(viekerWallet);

            WalletTransaction payoutTx = new WalletTransaction(
                    viekerWallet,
                    job,
                    viekerPayout,
                    TransactionType.ESCROW_RELEASE,
                    viekerWallet.getBalance(),
                    "Giải ngân giải quyết khiếu nại công việc #" + job.getId()
            );
            transactionRepository.save(payoutTx);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<WalletTransaction> getTransactionHistory(Long userId) {
        Wallet wallet = getWalletByUserId(userId);
        return transactionRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<WalletTransaction> getRecentTransactionsAll() {
        return transactionRepository.findTop20ByOrderByCreatedAtDesc();
    }
}
