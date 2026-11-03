package vn.vieshift.service;

import vn.vieshift.dto.CreateJobRequest;
import vn.vieshift.entity.Job;
import vn.vieshift.entity.JobStatus;

import java.math.BigDecimal;
import java.util.List;

public interface JobService {
    Job createJob(Long customerId, CreateJobRequest request);
    Job getJobById(Long jobId);
    List<Job> getJobsByCustomer(Long customerId);
    List<Job> getJobsByVieker(Long viekerId);
    List<Job> getAvailableJobsForMarketplace(Long categoryId);
    List<Job> getAllJobs();
    List<Job> getJobsByStatus(JobStatus status);

    Job acceptJob(Long viekerId, Long jobId);
    Job startJob(Long viekerId, Long jobId);
    Job reportCompleteJob(Long viekerId, Long jobId);
    Job approveAndPayJob(Long customerId, Long jobId);
    Job cancelJob(Long customerId, Long jobId);

    BigDecimal calculateEstimatedPrice(Long categoryId, BigDecimal hours, BigDecimal distanceKm);
}
