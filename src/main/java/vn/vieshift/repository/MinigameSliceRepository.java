package vn.vieshift.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.vieshift.entity.MinigameSlice;

import java.util.List;

@Repository
public interface MinigameSliceRepository extends JpaRepository<MinigameSlice, Long> {
    List<MinigameSlice> findByActiveTrueOrderBySliceIndexAsc();
}
