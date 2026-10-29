package vn.vieshift.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class DisputeRequest {

    private Long jobId;

    @NotBlank(message = "Vui lòng nhập lý do khiếu nại")
    private String reason;

    @NotBlank(message = "Vui lòng mô tả chi tiết bằng chứng / tình huống xảy ra")
    private String evidenceText;

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
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
}
