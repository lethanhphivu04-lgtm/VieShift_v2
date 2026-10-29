package vn.vieshift.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.vieshift.entity.JobCategory;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobCategoryRepository extends JpaRepository<JobCategory, Long> {
    List<JobCategory> findByActiveTrueOrderByNameAsc();
    Optional<JobCategory> findByName(String name);
    boolean existsByName(String name);
}
