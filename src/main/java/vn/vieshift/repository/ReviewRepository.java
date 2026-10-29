package vn.vieshift.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.vieshift.entity.Review;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    Optional<Review> findByJobId(Long jobId);
    boolean existsByJobId(Long jobId);
    List<Review> findByViekerIdOrderByCreatedAtDesc(Long viekerId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.vieker.id = :viekerId")
    Double calculateAverageRating(@Param("viekerId") Long viekerId);

    long countByViekerId(Long viekerId);
}
