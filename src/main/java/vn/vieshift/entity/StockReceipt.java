package vn.vieshift.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Phiếu Nhập Kho & Quản Lý Công Nợ Nhà Cung Cấp
 * Kế thừa từ Models\StockReceipt.php và StockController.php của repo LTW_01 (Minishop_LeThanhPVu)
 */
@Entity
@Table(name = "shop_stock_receipts")
public class StockReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String receiptCode;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, length = 150)
    private String supplierName = "Nhà cung cấp Vật tư Chính hãng";

    @Column(nullable = false)
    private Integer quantity = 10;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal importPrice = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal totalCost = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(length = 300)
    private String note;

    @Column(length = 100)
    private String createdBy = "Quản trị viên Hệ thống";

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public StockReceipt() {
    }

    public StockReceipt(Product product, String supplierName, Integer quantity,
                        BigDecimal importPrice, BigDecimal paidAmount, String note, String createdBy) {
        this.receiptCode = "PNK-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
                + "-" + (int) (Math.random() * 90 + 10);
        this.product = product;
        this.supplierName = (supplierName != null && !supplierName.isBlank())
                ? supplierName.trim()
                : "Nhà cung cấp Vật tư Chính hãng";
        this.quantity = quantity != null && quantity > 0 ? quantity : 1;
        this.importPrice = importPrice != null ? importPrice : BigDecimal.ZERO;
        this.totalCost = this.importPrice.multiply(BigDecimal.valueOf(this.quantity));
        this.paidAmount = paidAmount != null ? paidAmount.min(this.totalCost) : this.totalCost;
        this.note = note;
        this.createdBy = createdBy != null ? createdBy : "Admin";
        this.createdAt = LocalDateTime.now();
    }

    public BigDecimal getDebtAmount() {
        BigDecimal total = totalCost != null ? totalCost : BigDecimal.ZERO;
        BigDecimal paid = paidAmount != null ? paidAmount : BigDecimal.ZERO;
        return total.subtract(paid).max(BigDecimal.ZERO);
    }

    public boolean isFullyPaid() {
        return getDebtAmount().compareTo(BigDecimal.ZERO) == 0;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getReceiptCode() {
        return receiptCode;
    }

    public void setReceiptCode(String receiptCode) {
        this.receiptCode = receiptCode;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getImportPrice() {
        return importPrice;
    }

    public void setImportPrice(BigDecimal importPrice) {
        this.importPrice = importPrice;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(BigDecimal totalCost) {
        this.totalCost = totalCost;
    }

    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(BigDecimal paidAmount) {
        this.paidAmount = paidAmount;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getProductName() {
        return product != null ? product.getName() : "Sản phẩm vật tư";
    }

    public Integer getImportQuantity() {
        return quantity;
    }

    public BigDecimal getImportUnitPrice() {
        return importPrice;
    }

    public String getNotes() {
        return note;
    }

    public String getPaymentStatus() {
        if (isFullyPaid()) {
            return "PAID";
        }
        if (paidAmount != null && paidAmount.compareTo(BigDecimal.ZERO) > 0) {
            return "PARTIAL";
        }
        return "DEBT";
    }

    public String getPaymentStatusDisplayName() {
        String st = getPaymentStatus();
        if ("PAID".equals(st)) {
            return "Đã tất toán";
        }
        if ("PARTIAL".equals(st)) {
            return "Trả một phần (Còn nợ)";
        }
        return "Ghi nợ NCC";
    }
}
