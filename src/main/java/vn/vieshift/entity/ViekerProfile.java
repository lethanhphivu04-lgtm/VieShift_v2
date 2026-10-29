package vn.vieshift.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "vieker_profiles")
public class ViekerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(length = 20)
    private String idCardNumber;

    @Column(length = 255)
    private String avatarUrl;

    @Column(columnDefinition = "TEXT")
    private String skillsDescription;

    private Integer experienceYears = 1;

    @Column(length = 150)
    private String operatingZone = "TP. Hồ Chí Minh";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VerificationStatus verificationStatus = VerificationStatus.PENDING;

    @Column(length = 255)
    private String rejectionReason;

    @Column(precision = 3, scale = 2)
    private BigDecimal ratingAvg = BigDecimal.valueOf(5.0);

    private Integer ratingCount = 0;

    private Integer totalJobsCompleted = 0;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // ===== Onboarding & KYC =====
    @Column(length = 64, unique = true)
    private String onboardingToken;

    @Column
    private LocalDateTime tokenExpiresAt;

    @Column(length = 150)
    private String primaryCategory;

    private java.time.LocalDate dateOfBirth;

    @Column(length = 255)
    private String permanentAddress;

    @Column(length = 30)
    private String driverLicenseNumber;

    @Column(length = 255)
    private String portraitPath;

    @Column(length = 255)
    private String idFrontPath;

    @Column(length = 255)
    private String idBackPath;

    @Column(length = 255)
    private String criminalRecordPath;

    @Column(length = 255)
    private String driverLicensePath;

    /** Danh sách giấy tờ admin yêu cầu bổ sung, phân tách bằng dấu phẩy (vd: "portrait,idFront") */
    @Column(length = 255)
    private String docsToUpdate;

    private LocalDateTime submittedAt;

    private LocalDateTime reviewedAt;

    // ===== Bổ sung chuyên môn & Phạm vi hoạt động =====
    @Column(length = 255)
    private String additionalCategories;

    @Column(length = 150)
    private String pendingCategoryRequest;

    private Integer pendingCategoryYears;

    @Column(columnDefinition = "TEXT")
    private String pendingCategoryDesc;

    @Column(length = 255)
    private String pendingCategoryCertPath;

    private LocalDateTime pendingCategoryRequestedAt;

    private Integer serviceRadiusKm = 10;

    public String getAdditionalCategories() { return additionalCategories; }
    public void setAdditionalCategories(String additionalCategories) { this.additionalCategories = additionalCategories; }
    public String getPendingCategoryRequest() { return pendingCategoryRequest; }
    public void setPendingCategoryRequest(String pendingCategoryRequest) { this.pendingCategoryRequest = pendingCategoryRequest; }
    public Integer getPendingCategoryYears() { return pendingCategoryYears; }
    public void setPendingCategoryYears(Integer pendingCategoryYears) { this.pendingCategoryYears = pendingCategoryYears; }
    public String getPendingCategoryDesc() { return pendingCategoryDesc; }
    public void setPendingCategoryDesc(String pendingCategoryDesc) { this.pendingCategoryDesc = pendingCategoryDesc; }
    public String getPendingCategoryCertPath() { return pendingCategoryCertPath; }
    public void setPendingCategoryCertPath(String pendingCategoryCertPath) { this.pendingCategoryCertPath = pendingCategoryCertPath; }
    public LocalDateTime getPendingCategoryRequestedAt() { return pendingCategoryRequestedAt; }
    public void setPendingCategoryRequestedAt(LocalDateTime pendingCategoryRequestedAt) { this.pendingCategoryRequestedAt = pendingCategoryRequestedAt; }
    public Integer getServiceRadiusKm() { return serviceRadiusKm != null ? serviceRadiusKm : 10; }
    public void setServiceRadiusKm(Integer serviceRadiusKm) { this.serviceRadiusKm = serviceRadiusKm; }

    public boolean canPerformCategory(String categoryName) {
        if (categoryName == null || categoryName.isBlank()) return true;
        String target = categoryName.trim().toLowerCase();

        if (primaryCategory != null) {
            String p = primaryCategory.trim().toLowerCase();
            if (p.equals(target) || p.contains(target) || target.contains(p)) {
                return true;
            }
        }

        if (additionalCategories != null && !additionalCategories.isBlank()) {
            String[] parts = additionalCategories.split(",");
            for (String part : parts) {
                String a = part.trim().toLowerCase();
                if (!a.isEmpty() && (a.equals(target) || a.contains(target) || target.contains(a))) {
                    return true;
                }
            }
        }
        return false;
    }

    public java.util.List<String> getAllApprovedCategories() {
        java.util.List<String> list = new java.util.ArrayList<>();
        if (primaryCategory != null && !primaryCategory.isBlank()) {
            list.add(primaryCategory.trim());
        }
        if (additionalCategories != null && !additionalCategories.isBlank()) {
            for (String s : additionalCategories.split(",")) {
                String trimmed = s.trim();
                if (!trimmed.isEmpty() && !list.contains(trimmed)) {
                    list.add(trimmed);
                }
            }
        }
        return list;
    }

    public String getOnboardingToken() { return onboardingToken; }
    public void setOnboardingToken(String onboardingToken) { this.onboardingToken = onboardingToken; }
    public LocalDateTime getTokenExpiresAt() { return tokenExpiresAt; }
    public void setTokenExpiresAt(LocalDateTime tokenExpiresAt) { this.tokenExpiresAt = tokenExpiresAt; }
    public boolean isTokenExpired() {
        return tokenExpiresAt != null && LocalDateTime.now().isAfter(tokenExpiresAt);
    }
    public String getPrimaryCategory() { return primaryCategory; }
    public void setPrimaryCategory(String primaryCategory) { this.primaryCategory = primaryCategory; }
    public java.time.LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(java.time.LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    public String getPermanentAddress() { return permanentAddress; }
    public void setPermanentAddress(String permanentAddress) { this.permanentAddress = permanentAddress; }
    public String getDriverLicenseNumber() { return driverLicenseNumber; }
    public void setDriverLicenseNumber(String driverLicenseNumber) { this.driverLicenseNumber = driverLicenseNumber; }
    public String getPortraitPath() { return portraitPath; }
    public void setPortraitPath(String portraitPath) { this.portraitPath = portraitPath; }
    public String getIdFrontPath() { return idFrontPath; }
    public void setIdFrontPath(String idFrontPath) { this.idFrontPath = idFrontPath; }
    public String getIdBackPath() { return idBackPath; }
    public void setIdBackPath(String idBackPath) { this.idBackPath = idBackPath; }
    public String getCriminalRecordPath() { return criminalRecordPath; }
    public void setCriminalRecordPath(String criminalRecordPath) { this.criminalRecordPath = criminalRecordPath; }
    public String getDriverLicensePath() { return driverLicensePath; }
    public void setDriverLicensePath(String driverLicensePath) { this.driverLicensePath = driverLicensePath; }
    public String getDocsToUpdate() { return docsToUpdate; }
    public void setDocsToUpdate(String docsToUpdate) { this.docsToUpdate = docsToUpdate; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }

    public boolean needsDoc(String docKey) {
        if (verificationStatus != VerificationStatus.NEEDS_UPDATE || docsToUpdate == null) return false;
        return java.util.Arrays.asList(docsToUpdate.split(",")).contains(docKey);
    }

    public boolean isEditable() {
        return verificationStatus == VerificationStatus.DRAFT || verificationStatus == VerificationStatus.NEEDS_UPDATE;
    }

    public ViekerProfile() {
    }

    public ViekerProfile(User user) {
        this.user = user;
        this.verificationStatus = VerificationStatus.PENDING;
        this.ratingAvg = BigDecimal.valueOf(5.0);
        this.ratingCount = 0;
        this.totalJobsCompleted = 0;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getIdCardNumber() {
        return idCardNumber;
    }

    public void setIdCardNumber(String idCardNumber) {
        this.idCardNumber = idCardNumber;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getSkillsDescription() {
        return skillsDescription;
    }

    public void setSkillsDescription(String skillsDescription) {
        this.skillsDescription = skillsDescription;
    }

    public Integer getExperienceYears() {
        return experienceYears;
    }

    public void setExperienceYears(Integer experienceYears) {
        this.experienceYears = experienceYears;
    }

    public String getOperatingZone() {
        return operatingZone;
    }

    public void setOperatingZone(String operatingZone) {
        this.operatingZone = operatingZone;
    }

    public VerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(VerificationStatus verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public BigDecimal getRatingAvg() {
        return ratingAvg;
    }

    public void setRatingAvg(BigDecimal ratingAvg) {
        this.ratingAvg = ratingAvg;
    }

    public Integer getRatingCount() {
        return ratingCount;
    }

    public void setRatingCount(Integer ratingCount) {
        this.ratingCount = ratingCount;
    }

    public Integer getTotalJobsCompleted() {
        return totalJobsCompleted;
    }

    public void setTotalJobsCompleted(Integer totalJobsCompleted) {
        this.totalJobsCompleted = totalJobsCompleted;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isApproved() {
        return this.verificationStatus == VerificationStatus.APPROVED;
    }
}
