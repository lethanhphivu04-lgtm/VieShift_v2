package vn.vieshift.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.vieshift.entity.StaffPermission;

import java.util.Optional;

@Repository
public interface StaffPermissionRepository extends JpaRepository<StaffPermission, Long> {
    Optional<StaffPermission> findByUserId(Long userId);
    void deleteByUserId(Long userId);
}
