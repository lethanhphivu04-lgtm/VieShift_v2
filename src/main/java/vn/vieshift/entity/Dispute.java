package vn.vieshift.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "disputes")
public class Dispute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false, unique = true)
    private Job job;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opened_by_user_id", nullable = false)
    private User openedBy;

    @Column(nullable = false, length = 200)
    private String reason;

    @Column(columnDefinition = "TEXT")
    private String evidenceText;

    @Column(length = 500)
    private String attachmentPath;

    @Column(length = 255)
    private String attachmentOriginalName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DisputeStatus status = DisputeStatus.OPEN;

    @Column(columnDefinition = "TEXT")
    private String adminResolutionNote;

    @Column(precision = 12, scale = 2)
    private BigDecimal customerRefundAmount = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal viekerPayoutAmount = BigDecimal.ZERO;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime resolvedAt;

    public Dispute() {
    }

    public Dispute(Job job, User openedBy, String reason, String evidenceText) {
        this.job = job;
        this.openedBy = openedBy;
        this.reason = reason;
        this.evidenceText = evidenceText;
        this.status = DisputeStatus.OPEN;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Job getJob() {
        return job;
    }

    public void setJob(Job job) {
        this.job = job;
    }

    public User getOpenedBy() {
        return openedBy;
    }

    public void setOpenedBy(User openedBy) {
        this.openedBy = openedBy;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getEvidenceText() {
        return evidenceText;
    }

    public void setEvidenceText(String evidenceText) {
        this.evidenceText = evidenceText;
    }

    public String getAttachmentPath() {
        return attachmentPath;
    }

    public void setAttachmentPath(String attachmentPath) {
        this.attachmentPath = attachmentPath;
    }

    public String getAttachmentOriginalName() {
        return attachmentOriginalName;
    }

    public void setAttachmentOriginalName(String attachmentOriginalName) {
        this.attachmentOriginalName = attachmentOriginalName;
    }

    public DisputeStatus getStatus() {
        return status;
    }

    public void setStatus(DisputeStatus status) {
        this.status = status;
    }

    public String getAdminResolutionNote() {
        return adminResolutionNote;
    }

    public void setAdminResolutionNote(String adminResolutionNote) {
        this.adminResolutionNote = adminResolutionNote;
    }

    public BigDecimal getCustomerRefundAmount() {
        return customerRefundAmount;
    }

    public void setCustomerRefundAmount(BigDecimal customerRefundAmount) {
        this.customerRefundAmount = customerRefundAmount;
    }

    public BigDecimal getViekerPayoutAmount() {
        return viekerPayoutAmount;
    }

    public void setViekerPayoutAmount(BigDecimal viekerPayoutAmount) {
        this.viekerPayoutAmount = viekerPayoutAmount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }
}
