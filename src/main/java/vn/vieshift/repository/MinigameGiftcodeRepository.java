package vn.vieshift.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.vieshift.entity.MinigameGiftcode;

import java.util.Optional;

@Repository
public interface MinigameGiftcodeRepository extends JpaRepository<MinigameGiftcode, Long> {
    Optional<MinigameGiftcode> findByCodeIgnoreCaseAndActiveTrue(String code);
    Optional<MinigameGiftcode> findByCodeIgnoreCase(String code);
}
