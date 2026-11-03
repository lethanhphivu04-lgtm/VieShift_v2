package vn.vieshift.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.vieshift.entity.Job;
import vn.vieshift.entity.Product;
import vn.vieshift.entity.ProductReview;
import vn.vieshift.entity.ShopOrder;
import vn.vieshift.entity.StockReceipt;
import vn.vieshift.entity.User;
import vn.vieshift.entity.Voucher;
import vn.vieshift.exception.ResourceNotFoundException;
import vn.vieshift.repository.ProductRepository;
import vn.vieshift.repository.ProductReviewRepository;
import vn.vieshift.repository.ShopOrderRepository;
import vn.vieshift.repository.StockReceiptRepository;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class ShopService {

    public static final BigDecimal FREESHIP_THRESHOLD = new BigDecimal("300000");
    public static final BigDecimal STANDARD_SHIPPING_FEE = new BigDecimal("28000");

    private final ProductRepository productRepository;
    private final ShopOrderRepository shopOrderRepository;
    private final ProductReviewRepository productReviewRepository;
    private final StockReceiptRepository stockReceiptRepository;
    private final WalletService walletService;
    private final VoucherService voucherService;
    private final DataIntegrityService dataIntegrityService;
    private final GhnShippingService ghnShippingService;

    public ShopService(ProductRepository productRepository,
                       ShopOrderRepository shopOrderRepository,
                       ProductReviewRepository productReviewRepository,
                       StockReceiptRepository stockReceiptRepository,
                       WalletService walletService,
                       VoucherService voucherService,
                       DataIntegrityService dataIntegrityService,
                       GhnShippingService ghnShippingService) {
        this.productRepository = productRepository;
        this.shopOrderRepository = shopOrderRepository;
        this.productReviewRepository = productReviewRepository;
        this.stockReceiptRepository = stockReceiptRepository;
        this.walletService = walletService;
        this.voucherService = voucherService;
        this.dataIntegrityService = dataIntegrityService;
        this.ghnShippingService = ghnShippingService;
    }

    @Transactional(readOnly = true)
    public List<Product> getAllActiveProducts() {
        return productRepository.findByActiveTrueOrderBySoldCountDesc().stream()
                .filter(p -> !p.isDeleted())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Product> getAllProducts() {
        return productRepository.findAll().stream()
                .filter(p -> !p.isDeleted())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Product> getTrashedProducts() {
        return productRepository.findAll().stream()
                .filter(Product::isDeleted)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Product> searchProducts(String category, String query) {
        return searchProducts(category, query, "popular", "ALL");
    }

    @Transactional(readOnly = true)
    public List<Product> searchProducts(String category, String query, String sort, String priceRange) {
        List<Product> products = (category != null && !category.isBlank() && !category.equalsIgnoreCase("ALL"))
                ? new ArrayList<>(productRepository.findByActiveTrueAndCategoryOrderBySoldCountDesc(category.trim()))
                : new ArrayList<>(productRepository.findByActiveTrueOrderBySoldCountDesc());

        products = products.stream()
                .filter(p -> !p.isDeleted())
                .collect(Collectors.toList());

        if (query != null && !query.isBlank()) {
            String q = query.trim().toLowerCase();
            products = products.stream()
                    .filter(p -> (p.getName() != null && p.getName().toLowerCase().contains(q))
                            || (p.getDescription() != null && p.getDescription().toLowerCase().contains(q))
                            || (p.getCategory() != null && p.getCategory().toLowerCase().contains(q)))
                    .collect(Collectors.toList());
        }

        if (priceRange != null && !priceRange.isBlank() && !priceRange.equalsIgnoreCase("ALL")) {
            switch (priceRange.toUpperCase()) {
                case "UNDER_150K" -> products = products.stream()
                        .filter(p -> p.getPrice() != null && p.getPrice().compareTo(new BigDecimal("150000")) <= 0)
                        .collect(Collectors.toList());
                case "150K_300K" -> products = products.stream()
                        .filter(p -> p.getPrice() != null
                                && p.getPrice().compareTo(new BigDecimal("150000")) > 0
                                && p.getPrice().compareTo(new BigDecimal("300000")) <= 0)
                        .collect(Collectors.toList());
                case "OVER_300K" -> products = products.stream()
                        .filter(p -> p.getPrice() != null && p.getPrice().compareTo(new BigDecimal("300000")) > 0)
                        .collect(Collectors.toList());
                default -> {}
            }
        }

        if (sort != null && !sort.isBlank()) {
            switch (sort.toLowerCase()) {
                case "price_asc" -> products.sort(Comparator.comparing(Product::getPrice, Comparator.nullsLast(Comparator.naturalOrder())));
                case "price_desc" -> products.sort(Comparator.comparing(Product::getPrice, Comparator.nullsLast(Comparator.reverseOrder())));
                case "discount" -> products.sort(Comparator.comparingInt(Product::getDiscountPercent).reversed());
                case "newest" -> products.sort(Comparator.comparing(Product::getId, Comparator.nullsLast(Comparator.reverseOrder())));
                default -> products.sort(Comparator.comparing(Product::getSoldCount, Comparator.nullsLast(Comparator.reverseOrder())));
            }
        }

        return products;
    }

    @Transactional(readOnly = true)
    public List<String> getCategories() {
        return getAllActiveProducts().stream()
                .map(Product::getCategory)
                .filter(c -> c != null && !c.isBlank())
                .distinct()
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm #" + id));
    }

    @Transactional(readOnly = true)
    public List<ProductReview> getProductReviews(Long productId) {
        return productReviewRepository.findByProductIdOrderByCreatedAtDesc(productId)
                .stream()
                .filter(ProductReview::isVisible)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductReview> getAllProductReviews() {
        return productReviewRepository.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
    }

    public ProductReview replyProductReview(Long reviewId, String adminReply, Boolean visible) {
        ProductReview review = productReviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đánh giá #" + reviewId));
        review.setAdminReply(adminReply != null && !adminReply.isBlank() ? adminReply.trim() : null);
        if (visible != null) {
            review.setVisible(visible);
        }
        return productReviewRepository.save(review);
    }

    public void deleteProductReview(Long reviewId) {
        productReviewRepository.deleteById(reviewId);
    }

    public ProductReview addProductReview(Long productId, String reviewerName, String reviewerPhone,
                                          Integer rating, String comment) {
        Product product = getProductById(productId);
        if (reviewerName == null || reviewerName.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập họ tên của bạn.");
        }
        if (comment == null || comment.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập nội dung đánh giá sản phẩm.");
        }
        String phone = reviewerPhone != null ? reviewerPhone.trim() : "";
        boolean verified = !phone.isBlank() && shopOrderRepository.countByRecipientPhone(phone) > 0;
        ProductReview review = new ProductReview(
                product,
                reviewerName.trim(),
                phone,
                rating != null ? rating : 5,
                comment.trim(),
                verified
        );
        return productReviewRepository.save(review);
    }

    // =========================================================================
    // QUẢN LÝ PHIẾU NHẬP KHO & CÔNG NỢ NHÀ CUNG CẤP (KẾ THỪA LTW_01)
    // =========================================================================

    @Transactional(readOnly = true)
    public List<StockReceipt> getAllStockReceipts() {
        return stockReceiptRepository.findAllByOrderByCreatedAtDesc();
    }

    public StockReceipt createStockReceipt(Long productId, String supplierName, Integer quantity,
                                           BigDecimal importPrice, BigDecimal paidAmount,
                                           String note, String createdBy) {
        Product product = getProductById(productId);
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Số lượng nhập kho phải lớn hơn 0.");
        }
        if (importPrice == null || importPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Đơn giá vốn nhập kho phải lớn hơn 0.");
        }

        StockReceipt receipt = new StockReceipt(
                product, supplierName, quantity, importPrice, paidAmount, note, createdBy
        );
        StockReceipt savedReceipt = stockReceiptRepository.save(receipt);

        // Cộng tồn kho và ký lại Checksum HMAC-SHA256
        product.setStockQuantity((product.getStockQuantity() != null ? product.getStockQuantity() : 0) + quantity);
        saveWithChecksum(product);

        return savedReceipt;
    }

    public StockReceipt paySupplierDebt(Long receiptId, BigDecimal payAmount) {
        StockReceipt receipt = stockReceiptRepository.findById(receiptId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu nhập kho #" + receiptId));
        if (payAmount == null || payAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Số tiền thanh toán nợ phải lớn hơn 0.");
        }
        BigDecimal newPaid = receipt.getPaidAmount().add(payAmount).min(receipt.getTotalCost());
        receipt.setPaidAmount(newPaid);
        return stockReceiptRepository.save(receipt);
    }

    // =========================================================================
    // QUẢN LÝ SẢN PHẨM & THÙNG RÁC KHÔI PHỤC (SOFT-DELETE TRASH TỪ LTW_01)
    // =========================================================================

    public Product createProduct(String name, String category, String description,
                                 BigDecimal price, BigDecimal originalPrice,
                                 Integer stockQuantity, String unit,
                                 String imageUrl, String relatedServiceCategory) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Tên sản phẩm không được để trống");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Giá bán sản phẩm phải lớn hơn 0");
        }
        BigDecimal orig = (originalPrice != null && originalPrice.compareTo(price) >= 0)
                ? originalPrice
                : price;
        int stock = (stockQuantity != null && stockQuantity >= 0) ? stockQuantity : 50;
        String u = (unit != null && !unit.isBlank()) ? unit.trim() : "Bộ";
        String img = (imageUrl != null && !imageUrl.isBlank())
                ? imageUrl.trim()
                : "https://images.unsplash.com/photo-1581092160607-ee22621dd758?auto=format&fit=crop&w=600&q=80";

        Product product = new Product(
                name.trim(),
                category != null && !category.isBlank() ? category.trim() : "Vật tư & Thiết bị gia đình",
                description != null ? description.trim() : "",
                price,
                orig,
                stock,
                u,
                img,
                relatedServiceCategory != null && !relatedServiceCategory.isBlank() ? relatedServiceCategory.trim() : null
        );
        return saveWithChecksum(product);
    }

    private Product saveWithChecksum(Product product) {
        Product saved = productRepository.save(product);
        saved.setChecksum(dataIntegrityService.computeProductChecksum(saved));
        return productRepository.save(saved);
    }

    public Product toggleProductActive(Long id) {
        Product product = getProductById(id);
        product.setActive(!product.isActive());
        return saveWithChecksum(product);
    }

    public Product updateProduct(Long id, String name, String category, String description,
                                 BigDecimal price, BigDecimal originalPrice,
                                 Integer stockQuantity, String unit,
                                 String imageUrl, String relatedServiceCategory) {
        Product product = getProductById(id);
        if (name != null && !name.isBlank()) {
            product.setName(name.trim());
        }
        if (category != null && !category.isBlank()) {
            product.setCategory(category.trim());
        }
        if (description != null) {
            product.setDescription(description.trim());
        }
        if (price != null && price.compareTo(BigDecimal.ZERO) > 0) {
            product.setPrice(price);
        }
        if (originalPrice != null && originalPrice.compareTo(BigDecimal.ZERO) > 0) {
            product.setOriginalPrice(originalPrice);
        }
        if (stockQuantity != null && stockQuantity >= 0) {
            product.setStockQuantity(stockQuantity);
        }
        if (unit != null && !unit.isBlank()) {
            product.setUnit(unit.trim());
        }
        if (imageUrl != null && !imageUrl.isBlank()) {
            product.setImageUrl(imageUrl.trim());
        }
        if (relatedServiceCategory != null) {
            product.setRelatedServiceCategory(relatedServiceCategory.isBlank() ? null : relatedServiceCategory.trim());
        }
        return saveWithChecksum(product);
    }

    public void deleteProduct(Long id) {
        Product product = getProductById(id);
        product.setActive(false);
        product.setDeleted(true);
        saveWithChecksum(product);
    }

    public Product restoreProduct(Long id) {
        Product product = getProductById(id);
        product.setDeleted(false);
        product.setActive(true);
        return saveWithChecksum(product);
    }

    public void forceDeleteProduct(Long id) {
        Product product = getProductById(id);
        if (product.getSoldCount() != null && product.getSoldCount() > 0) {
            throw new IllegalStateException("Sản phẩm #" + id + " (" + product.getName() + ") đã phát sinh "
                    + product.getSoldCount() + " lượt mua trong lịch sử kế toán nên không thể xóa vĩnh viễn khỏi CSDL (chỉ có thể lưu trữ trong Thùng Rác).");
        }
        productRepository.delete(product);
    }

    @Transactional(readOnly = true)
    public ShopOrder getOrderById(Long orderId) {
        return shopOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng vật tư #" + orderId));
    }

    public Product updateProductStockAndPrice(Long id, BigDecimal price, Integer stockQuantity) {
        Product product = getProductById(id);
        if (price != null && price.compareTo(BigDecimal.ZERO) > 0) {
            product.setPrice(price);
        }
        if (stockQuantity != null && stockQuantity >= 0) {
            product.setStockQuantity(stockQuantity);
        }
        return saveWithChecksum(product);
    }

    public ShopOrder createStandaloneOrder(User customer,
                                           List<Long> productIds,
                                           List<Integer> quantities,
                                           String shippingAddress,
                                           String recipientPhone,
                                           String notes) {
        return createStandaloneOrder(
                customer,
                productIds,
                quantities,
                customer != null ? customer.getFullName() : "Khách hàng",
                shippingAddress,
                recipientPhone,
                notes,
                "WALLET",
                null,
                "SELF_INSTALL",
                null,
                null
        );
    }

    public ShopOrder createStandaloneOrder(User customer,
                                           List<Long> productIds,
                                           List<Integer> quantities,
                                           String recipientName,
                                           String shippingAddress,
                                           String recipientPhone,
                                           String notes,
                                           String paymentMethod,
                                           String voucherCode,
                                           String installationMode,
                                           Integer ghnDistrictId,
                                           String ghnWardCode) {
        if (productIds == null || productIds.isEmpty()) {
            throw new IllegalArgumentException("Giỏ hàng đang trống. Vui lòng chọn ít nhất 1 sản phẩm.");
        }
        if (shippingAddress == null || shippingAddress.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập địa chỉ nhận hàng.");
        }

        Map<Long, Integer> itemQtyMap = new LinkedHashMap<>();
        for (int i = 0; i < productIds.size(); i++) {
            Long pid = productIds.get(i);
            int qty = (quantities != null && i < quantities.size() && quantities.get(i) != null)
                    ? quantities.get(i) : 1;
            if (pid != null && qty > 0) {
                itemQtyMap.merge(pid, qty, Integer::sum);
            }
        }

        if (itemQtyMap.isEmpty()) {
            throw new IllegalArgumentException("Số lượng sản phẩm không hợp lệ.");
        }

        BigDecimal subtotalAmount = BigDecimal.ZERO;
        int totalItems = 0;
        List<String> summaryParts = new ArrayList<>();
        NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));

        List<Product> productsToUpdate = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : itemQtyMap.entrySet()) {
            Product p = getProductById(entry.getKey());
            int qty = entry.getValue();

            if (p.getChecksum() != null && !p.getChecksum().isBlank() && !dataIntegrityService.verifyProductChecksum(p)) {
                throw new IllegalStateException("Sản phẩm '"
                        + p.getName() + "' hiện đang được hệ thống đồng bộ lại giá niêm yết. Vui lòng liên hệ bộ phận hỗ trợ hoặc thử lại sau ít phút.");
            }

            if (!p.isActive() || p.isDeleted()) {
                throw new IllegalArgumentException("Sản phẩm '" + p.getName() + "' hiện đang tạm ngừng kinh doanh.");
            }
            if (p.getStockQuantity() < qty) {
                throw new IllegalArgumentException("Sản phẩm '" + p.getName() + "' chỉ còn " + p.getStockQuantity() + " " + p.getUnit() + " trong kho.");
            }

            BigDecimal lineTotal = p.getPrice().multiply(BigDecimal.valueOf(qty));
            subtotalAmount = subtotalAmount.add(lineTotal);
            totalItems += qty;
            summaryParts.add(qty + "x " + p.getName() + " (" + nf.format(lineTotal) + " đ)");

            p.setStockQuantity(p.getStockQuantity() - qty);
            p.setSoldCount((p.getSoldCount() != null ? p.getSoldCount() : 0) + qty);
            p.setChecksum(dataIntegrityService.computeProductChecksum(p));
            productsToUpdate.add(p);
        }

        // Tính cước phí GHN thực tế theo Quận/Huyện & Phường/Xã + Chính sách Trợ giá Freeship (giống LTW_01)
        int distId = (ghnDistrictId != null && ghnDistrictId > 0) ? ghnDistrictId : 1462;
        String wCode = (ghnWardCode != null && !ghnWardCode.isBlank()) ? ghnWardCode : "21722";
        Map<String, Object> ghnCalc = ghnShippingService.calculateShippingFee(distId, wCode, totalItems, subtotalAmount, 2);
        BigDecimal shippingFee = (BigDecimal) ghnCalc.getOrDefault("shippingFee", BigDecimal.ZERO);
        String shippingMethodName = (String) ghnCalc.getOrDefault("serviceName", "🚀 GHN Giao Nhanh Chuẩn");

        // Áp dụng Voucher giảm giá (nếu có)
        BigDecimal discountAmount = BigDecimal.ZERO;
        String appliedVoucherCode = null;
        if (voucherCode != null && !voucherCode.isBlank()) {
            String cleanCode = voucherCode.trim().toUpperCase();
            Optional<Voucher> vOpt = voucherService.validateVoucher(cleanCode, subtotalAmount);
            if (vOpt.isPresent()) {
                Voucher v = vOpt.get();
                discountAmount = v.calculateDiscount(subtotalAmount);
                appliedVoucherCode = v.getCode();
                voucherService.recordVoucherUsage(appliedVoucherCode);
            } else {
                throw new IllegalArgumentException("Mã giảm giá '" + cleanCode + "' không hợp lệ, đã hết lượt hoặc chưa đạt giá trị đơn tối thiểu.");
            }
        }

        BigDecimal totalAmount = subtotalAmount.subtract(discountAmount).add(shippingFee);
        if (totalAmount.compareTo(BigDecimal.ZERO) < 0) {
            totalAmount = BigDecimal.ZERO;
        }

        String itemsSummary = String.join(", ", summaryParts);
        String normalizedPayment = (paymentMethod != null && !paymentMethod.isBlank())
                ? paymentMethod.trim().toUpperCase()
                : "WALLET";
        String normalizedInstall = (installationMode != null && !installationMode.isBlank())
                ? installationMode.trim().toUpperCase()
                : "SELF_INSTALL";

        String initialStatus;
        if ("COD".equals(normalizedPayment)) {
            initialStatus = "PENDING_COD";
        } else if ("VNPAY".equals(normalizedPayment)) {
            initialStatus = "PENDING_VNPAY";
        } else {
            normalizedPayment = "WALLET";
            initialStatus = "PAID";
            if (totalAmount.compareTo(BigDecimal.ZERO) > 0) {
                walletService.payForShopOrder(
                        customer.getId(),
                        totalAmount,
                        "Thanh toán đơn vật tư VieShift Mall: " + itemsSummary
                                + (appliedVoucherCode != null ? " (Voucher " + appliedVoucherCode + " giảm -" + nf.format(discountAmount) + "đ)" : ""),
                        null
                );
            }
        }

        productRepository.saveAll(productsToUpdate);

        ShopOrder order = new ShopOrder();
        order.setCustomer(customer);
        order.setItemsSummary(itemsSummary);
        order.setSubtotalAmount(subtotalAmount);
        order.setDiscountAmount(discountAmount);
        order.setShippingFee(shippingFee);
        order.setShippingMethodName(shippingMethodName);
        order.setVoucherCode(appliedVoucherCode);
        order.setTotalAmount(totalAmount);
        order.setTotalItems(totalItems);
        order.setRecipientName(recipientName != null && !recipientName.isBlank() ? recipientName.trim() : customer.getFullName());
        order.setShippingAddress(shippingAddress.trim());
        order.setRecipientPhone(recipientPhone != null && !recipientPhone.isBlank() ? recipientPhone.trim() : customer.getPhone());
        order.setPaymentMethod(normalizedPayment);
        order.setInstallationMode(normalizedInstall);
        order.setOrderType("NEED_VIEKER".equals(normalizedInstall) ? "WITH_JOB" : "STANDALONE");
        order.setStatus(initialStatus);

        String finalNotes = notes != null ? notes.trim() : "";
        if ("NEED_VIEKER".equals(normalizedInstall)) {
            finalNotes = "[Yêu cầu Thợ VieKer lắp đặt] " + finalNotes;
        }
        order.setNotes(finalNotes.isBlank() ? null : finalNotes);

        return shopOrderRepository.save(order);
    }

    public ShopOrder createJobAddonOrder(User customer, Job job, List<Long> addonProductIds) {
        if (addonProductIds == null || addonProductIds.isEmpty()) {
            return null;
        }

        List<Product> selectedProducts = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<String> summaryParts = new ArrayList<>();
        NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));

        for (Long pid : addonProductIds) {
            if (pid == null) continue;
            productRepository.findById(pid).ifPresent(p -> {
                if (p.isActive() && !p.isDeleted() && p.getStockQuantity() > 0) {
                    selectedProducts.add(p);
                }
            });
        }

        if (selectedProducts.isEmpty()) {
            return null;
        }

        for (Product p : selectedProducts) {
            totalAmount = totalAmount.add(p.getPrice());
            summaryParts.add("1x " + p.getName() + " (" + nf.format(p.getPrice()) + " đ)");
            p.setStockQuantity(p.getStockQuantity() - 1);
            p.setSoldCount((p.getSoldCount() != null ? p.getSoldCount() : 0) + 1);
            p.setChecksum(dataIntegrityService.computeProductChecksum(p));
        }

        String itemsSummary = String.join(", ", summaryParts);
        walletService.payForShopOrder(
                customer.getId(),
                totalAmount,
                "Mua kèm vật tư cho đơn việc #" + job.getId() + ": " + itemsSummary,
                job
        );

        productRepository.saveAll(selectedProducts);

        ShopOrder order = new ShopOrder();
        order.setCustomer(customer);
        order.setRelatedJob(job);
        order.setItemsSummary(itemsSummary);
        order.setSubtotalAmount(totalAmount);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setShippingFee(BigDecimal.ZERO);
        order.setShippingMethodName("🛠️ Đối tác VieKer mang theo");
        order.setTotalAmount(totalAmount);
        order.setTotalItems(selectedProducts.size());
        order.setRecipientName(customer.getFullName());
        order.setShippingAddress(job.getAddress());
        order.setRecipientPhone(customer.getPhone());
        order.setPaymentMethod("WALLET");
        order.setInstallationMode("NEED_VIEKER");
        order.setOrderType("JOB_ADDON");
        order.setStatus("PAID");
        order.setNotes("Đối tác VieKer mang theo khi đến thực hiện công việc #" + job.getId());

        return shopOrderRepository.save(order);
    }

    @Transactional(readOnly = true)
    public List<ShopOrder> getCustomerOrders(Long customerId) {
        return shopOrderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    @Transactional(readOnly = true)
    public List<ShopOrder> getAllOrders() {
        return shopOrderRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Optional<ShopOrder> findOrderForTracking(String codeOrId, String phone) {
        if (codeOrId == null || codeOrId.isBlank()) {
            return Optional.empty();
        }
        String raw = codeOrId.trim().toUpperCase().replace("#", "");
        Optional<ShopOrder> found = shopOrderRepository.findByOrderCodeIgnoreCase(raw);
        if (found.isEmpty()) {
            String numericPart = raw.replace("VS-MALL-", "").trim();
            try {
                Long id = Long.parseLong(numericPart);
                found = shopOrderRepository.findById(id);
            } catch (NumberFormatException ignored) {}
        }
        if (found.isPresent() && phone != null && !phone.isBlank()) {
            ShopOrder o = found.get();
            String cleanPhone = phone.trim();
            boolean phoneMatch = (o.getRecipientPhone() != null && o.getRecipientPhone().contains(cleanPhone))
                    || (o.getCustomer() != null && o.getCustomer().getPhone() != null && o.getCustomer().getPhone().contains(cleanPhone));
            if (!phoneMatch) {
                return Optional.empty();
            }
        }
        return found;
    }

    public ShopOrder updateOrderStatus(Long orderId, String newStatus) {
        ShopOrder order = shopOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng #" + orderId));

        String oldStatus = order.getStatus();
        if ("CANCELLED".equalsIgnoreCase(newStatus) && !"CANCELLED".equalsIgnoreCase(oldStatus)) {
            if ("PAID".equalsIgnoreCase(oldStatus) && ("WALLET".equalsIgnoreCase(order.getPaymentMethod()) || "VNPAY".equalsIgnoreCase(order.getPaymentMethod()))) {
                walletService.refundShopOrder(
                        order.getCustomer().getId(),
                        order.getTotalAmount(),
                        "Hoàn tiền hủy đơn vật tư #VS-MALL-" + order.getId() + " (" + order.getItemsSummary() + ")",
                        order.getRelatedJob()
                );
            }
            if (order.getVoucherCode() != null && !order.getVoucherCode().isBlank()) {
                voucherService.releaseVoucherUsage(order.getVoucherCode());
            }
        }

        order.setStatus(newStatus.toUpperCase());
        return shopOrderRepository.save(order);
    }

    public void cancelJobAddonOrders(Long jobId) {
        if (jobId == null) return;
        for (ShopOrder order : shopOrderRepository.findByRelatedJobId(jobId)) {
            if (!"CANCELLED".equalsIgnoreCase(order.getStatus())) {
                updateOrderStatus(order.getId(), "CANCELLED");
            }
        }
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalShopRevenue() {
        BigDecimal rev = shopOrderRepository.sumTotalShopRevenue();
        return rev != null ? rev : BigDecimal.ZERO;
    }
}
