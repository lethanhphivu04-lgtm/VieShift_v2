package vn.vieshift.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.vieshift.entity.MinigameGiftcode;
import vn.vieshift.entity.MinigameGiftcodeRedemption;
import vn.vieshift.entity.User;

import java.util.List;

@Repository
public interface MinigameGiftcodeRedemptionRepository extends JpaRepository<MinigameGiftcodeRedemption, Long> {
    boolean existsByGiftcodeAndPhone(MinigameGiftcode giftcode, String phone);
    boolean existsByGiftcodeAndUser(MinigameGiftcode giftcode, User user);
    List<MinigameGiftcodeRedemption> findByPhone(String phone);
    List<MinigameGiftcodeRedemption> findByUser(User user);
}
