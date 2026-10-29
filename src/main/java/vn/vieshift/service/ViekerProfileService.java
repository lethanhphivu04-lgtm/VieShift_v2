package vn.vieshift.service;

import vn.vieshift.dto.ViekerProfileUpdateRequest;
import vn.vieshift.entity.VerificationStatus;
import vn.vieshift.entity.ViekerProfile;

import java.util.List;

public interface ViekerProfileService {
    ViekerProfile getProfileByUserId(Long userId);
    ViekerProfile updateProfile(Long userId, ViekerProfileUpdateRequest request);
    void approveProfile(Long profileId);
    void rejectProfile(Long profileId, String reason);
    List<ViekerProfile> getProfilesByStatus(VerificationStatus status);
    List<ViekerProfile> getAllProfiles();
}
