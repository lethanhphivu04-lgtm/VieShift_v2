package vn.vieshift.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ViekerProfileUpdateRequest {

    @NotBlank(message = "Vui lòng nhập số CCCD/CMND")
    private String idCardNumber;

    private String avatarUrl;

    @NotBlank(message = "Vui lòng mô tả kinh nghiệm và kỹ năng sở trường")
    private String skillsDescription;

    @NotNull(message = "Vui lòng nhập số năm kinh nghiệm")
    private Integer experienceYears = 1;

    @NotBlank(message = "Vui lòng nhập khu vực hoạt động")
    private String operatingZone = "TP. Hồ Chí Minh";

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
}
