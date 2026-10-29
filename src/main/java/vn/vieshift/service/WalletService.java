package vn.vieshift.service;

import vn.vieshift.dto.DepositRequest;
import vn.vieshift.dto.WithdrawRequest;
import vn.vieshift.entity.Job;
import vn.vieshift.entity.User;
import vn.vieshift.entity.Wallet;
import vn.vieshift.entity.WalletTransaction;

import java.math.BigDecimal;
import java.util.List;

public interface WalletService {
    Wallet getOrCreateWallet(User user);
    Wallet getWalletByUserId(Long userId);
    Wallet deposit(Long userId, DepositRequest request);
    Wallet deposit(Long userId, BigDecimal amount, String description);
    Wallet withdraw(Long userId, WithdrawRequest request);
    void holdEscrow(Long customerId, Job job, BigDecimal amount);
    void releaseEscrowToVieker(Job job, BigDecimal netAmount, BigDecimal platformFee);
    void transferTip(Long customerId, Long viekerId, Job job, BigDecimal tipAmount);
    void refundEscrowToCustomer(Job job, BigDecimal amount);
    void resolveEscrowSplit(Job job, BigDecimal customerRefund, BigDecimal viekerPayout);
    List<WalletTransaction> getTransactionHistory(Long userId);
    List<WalletTransaction> getRecentTransactionsAll();
}
