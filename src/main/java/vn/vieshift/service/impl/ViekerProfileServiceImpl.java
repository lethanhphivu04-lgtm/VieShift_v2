package vn.vieshift.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.vieshift.dto.ViekerProfileUpdateRequest;
import vn.vieshift.entity.VerificationStatus;
import vn.vieshift.entity.ViekerProfile;
import vn.vieshift.exception.ResourceNotFoundException;
import vn.vieshift.repository.ViekerProfileRepository;
import vn.vieshift.service.ViekerProfileService;

import java.util.List;

@Service
@Transactional
public class ViekerProfileServiceImpl implements ViekerProfileService {

    private final ViekerProfileRepository profileRepository;
    private final vn.vieshift.service.NotificationService notificationService;

    public ViekerProfileServiceImpl(ViekerProfileRepository profileRepository,
                                    vn.vieshift.service.NotificationService notificationService) {
        this.profileRepository = profileRepository;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional(readOnly = true)
    public ViekerProfile getProfileByUserId(Long userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Chưa tìm thấy hồ sơ thợ cho người dùng ID: " + userId));
    }

    @Override
    public ViekerProfile updateProfile(Long userId, ViekerProfileUpdateRequest request) {
        ViekerProfile profile = getProfileByUserId(userId);
        profile.setIdCardNumber(request.getIdCardNumber().trim());
        if (request.getAvatarUrl() != null && !request.getAvatarUrl().isBlank()) {
            profile.setAvatarUrl(request.getAvatarUrl().trim());
        }
        profile.setSkillsDescription(request.getSkillsDescription().trim());
        profile.setExperienceYears(request.getExperienceYears());
        profile.setOperatingZone(request.getOperatingZone().trim());
        profile.setVerificationStatus(VerificationStatus.PENDING); // Cập nhật lại giấy tờ -> Chờ duyệt lại
        return profileRepository.save(profile);
    }

    @Override
    public void approveProfile(Long profileId) {
        ViekerProfile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ thợ ID: " + profileId));
        profile.setVerificationStatus(VerificationStatus.APPROVED);
        profile.setRejectionReason(null);
        profileRepository.save(profile);

        if (profile.getUser() != null) {
            try {
                notificationService.notifyUser(
                        profile.getUser(),
                        "Hồ sơ đối tác đã được phê duyệt!",
                        "Chúc mừng! Hồ sơ đối tác VieKer Pro của bạn đã được kiểm duyệt thành công. Bạn có thể vào Sàn nhận việc ngay bây giờ.",
                        "/vieker?tab=marketplace",
                        vn.vieshift.entity.NotificationType.KYC_UPDATE
                );
            } catch (Exception ignored) {}
        }
    }

    @Override
    public void rejectProfile(Long profileId, String reason) {
        ViekerProfile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ thợ ID: " + profileId));
        profile.setVerificationStatus(VerificationStatus.REJECTED);
        String r = reason != null ? reason.trim() : "Thông tin chưa đạt yêu cầu của nền tảng";
        profile.setRejectionReason(r);
        profileRepository.save(profile);

        if (profile.getUser() != null) {
            try {
                notificationService.notifyUser(
                        profile.getUser(),
                        "Hồ sơ đối tác cần cập nhật",
                        "Hồ sơ VieKer của bạn chưa đạt yêu cầu với lý do: " + r + ". Vui lòng kiểm tra lại thông tin.",
                        "/vieker?tab=profile",
                        vn.vieshift.entity.NotificationType.KYC_UPDATE
                );
            } catch (Exception ignored) {}
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ViekerProfile> getProfilesByStatus(VerificationStatus status) {
        return profileRepository.findByVerificationStatusOrderByCreatedAtDesc(status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ViekerProfile> getAllProfiles() {
        return profileRepository.findAll();
    }
}
