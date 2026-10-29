package vn.vieshift.service;

import org.springframework.web.multipart.MultipartFile;
import vn.vieshift.dto.DisputeRequest;
import vn.vieshift.dto.ResolveDisputeRequest;
import vn.vieshift.entity.Dispute;
import vn.vieshift.entity.DisputeStatus;

import java.nio.file.Path;
import java.util.List;

public interface DisputeService {
    Dispute openDispute(Long userId, DisputeRequest request);
    Dispute openDispute(Long userId, DisputeRequest request, MultipartFile attachment);
    Path resolveAttachment(Long disputeId);
    Dispute getDisputeById(Long disputeId);
    Dispute getDisputeByJobId(Long jobId);
    Dispute resolveDispute(Long adminId, ResolveDisputeRequest request);
    List<Dispute> getDisputesByStatus(DisputeStatus status);
    List<Dispute> getAllDisputes();
}
