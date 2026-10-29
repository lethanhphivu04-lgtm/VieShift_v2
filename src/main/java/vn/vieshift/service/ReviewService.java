package vn.vieshift.service;

import vn.vieshift.dto.ReviewRequest;
import vn.vieshift.entity.Review;

import java.util.List;
import java.util.Optional;

public interface ReviewService {
    Review createReview(Long customerId, ReviewRequest request);
    Optional<Review> getReviewByJobId(Long jobId);
    List<Review> getReviewsByVieker(Long viekerId);
}
