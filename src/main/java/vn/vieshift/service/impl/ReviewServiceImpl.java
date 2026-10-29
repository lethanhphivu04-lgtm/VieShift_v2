package vn.vieshift.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.vieshift.dto.ReviewRequest;
import vn.vieshift.entity.Job;
import vn.vieshift.entity.JobStatus;
import vn.vieshift.entity.Review;
import vn.vieshift.entity.ViekerProfile;
import vn.vieshift.exception.InvalidJobStateException;
import vn.vieshift.exception.ResourceNotFoundException;
import vn.vieshift.exception.UnauthorizedActionException;
import vn.vieshift.repository.JobRepository;
import vn.vieshift.repository.ReviewRepository;
import vn.vieshift.repository.ViekerProfileRepository;
import vn.vieshift.service.ReviewService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final JobRepository jobRepository;
    private final ViekerProfileRepository profileRepository;

    public ReviewServiceImpl(ReviewRepository reviewRepository,
                             JobRepository jobRepository,
                             ViekerProfileRepository profileRepository) {
        this.reviewRepository = reviewRepository;
        this.jobRepository = jobRepository;
        this.profileRepository = profileRepository;
    }

    @Override
    public Review createReview(Long customerId, ReviewRequest request) {
        Job job = jobRepository.findById(request.getJobId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy công việc ID: " + request.getJobId()));

        if (!job.getCustomer().getId().equals(customerId)) {
            throw new UnauthorizedActionException("Chỉ khách hàng đã đặt việc mới có quyền đánh giá");
        }

        if (job.getStatus() != JobStatus.COMPLETED) {
            throw new InvalidJobStateException("Công việc phải hoàn thành mới có thể gửi đánh giá");
        }

        if (job.getVieker() == null) {
            throw new InvalidJobStateException("Công việc không có thông tin thợ để đánh giá");
        }

        if (reviewRepository.existsByJobId(job.getId())) {
            throw new IllegalStateException("Bạn đã gửi đánh giá cho công việc này rồi");
        }

        Review review = new Review(
                job,
                job.getCustomer(),
                job.getVieker(),
                request.getRating(),
                request.getComment() != null ? request.getComment().trim() : ""
        );
        Review savedReview = reviewRepository.save(review);

        // Cập nhật lại điểm đánh giá trung bình cho thợ
        Long viekerId = job.getVieker().getId();
        Double avg = reviewRepository.calculateAverageRating(viekerId);
        long count = reviewRepository.countByViekerId(viekerId);

        Optional<ViekerProfile> profileOpt = profileRepository.findByUserId(viekerId);
        if (profileOpt.isPresent()) {
            ViekerProfile profile = profileOpt.get();
            profile.setRatingAvg(BigDecimal.valueOf(avg != null ? avg : 5.0).setScale(2, RoundingMode.HALF_UP));
            profile.setRatingCount((int) count);
            profileRepository.save(profile);
        }

        return savedReview;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Review> getReviewByJobId(Long jobId) {
        return reviewRepository.findByJobId(jobId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Review> getReviewsByVieker(Long viekerId) {
        return reviewRepository.findByViekerIdOrderByCreatedAtDesc(viekerId);
    }
}
