package vn.vieshift.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ResolveDisputeRequest {

    @NotNull(message = "Thiếu thông tin khiếu nại")
    private Long disputeId;

    @NotBlank(message = "Vui lòng chọn phương án giải quyết")
    private String resolutionType = "FULL_REFUND_CUSTOMER"; // FULL_REFUND_CUSTOMER, FULL_PAYOUT_VIEKER, SPLIT_EQUAL

    @NotBlank(message = "Vui lòng nhập lý do giải quyết của Ban Quản Trị")
    private String adminNote;

    public Long getDisputeId() {
        return disputeId;
    }

    public void setDisputeId(Long disputeId) {
        this.disputeId = disputeId;
    }

    public String getResolutionType() {
        return resolutionType;
    }

    public void setResolutionType(String resolutionType) {
        this.resolutionType = resolutionType;
    }

    public String getAdminNote() {
        return adminNote;
    }

    public void setAdminNote(String adminNote) {
        this.adminNote = adminNote;
    }
}
