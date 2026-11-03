package vn.vieshift.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.vieshift.entity.Role;
import vn.vieshift.entity.StaffPermission;
import vn.vieshift.entity.User;
import vn.vieshift.repository.StaffPermissionRepository;
import vn.vieshift.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class StaffPermissionService {

    private final UserRepository userRepository;
    private final StaffPermissionRepository staffPermissionRepository;
    private final PasswordEncoder passwordEncoder;

    public StaffPermissionService(UserRepository userRepository,
                                  StaffPermissionRepository staffPermissionRepository,
                                  PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.staffPermissionRepository = staffPermissionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<User> getAllStaffUsers() {
        return userRepository.findByRoleOrderByCreatedAtDesc(Role.STAFF);
    }

    public StaffPermission getPermissionByUserId(Long userId) {
        return staffPermissionRepository.findByUserId(userId).orElse(null);
    }

    public StaffPermission getOrCreatePermission(User user) {
        if (user.getRole() != Role.STAFF) {
            return null;
        }
        return staffPermissionRepository.findByUserId(user.getId())
                .orElseGet(() -> staffPermissionRepository.save(new StaffPermission(user)));
    }

    @Transactional
    public User createStaff(String email, String rawPassword, String fullName, String phone,
                            String department, boolean canCreate, boolean canEdit, boolean canDelete,
                            boolean canUsers, boolean canServices, boolean canVieker, boolean canDispute,
                            boolean canShop, boolean canFinance, boolean canMinigame) {
        String cleanEmail = email.trim().toLowerCase();
        if (userRepository.findByEmail(cleanEmail).isPresent()) {
            throw new IllegalArgumentException("Email " + cleanEmail + " đã tồn tại trên hệ thống!");
        }

        User staff = new User(cleanEmail, passwordEncoder.encode(rawPassword), fullName.trim(), phone, Role.STAFF);
        staff = userRepository.save(staff);

        StaffPermission perm = new StaffPermission(staff);
        perm.setDepartment(department != null && !department.isBlank() ? department.trim() : "Ban Vận Hành & CSKH");
        perm.setCanCreate(canCreate);
        perm.setCanEdit(canEdit);
        perm.setCanDelete(canDelete);
        perm.setCanManageUsers(canUsers);
        perm.setCanManageServices(canServices);
        perm.setCanApproveVieker(canVieker);
        perm.setCanResolveDispute(canDispute);
        perm.setCanManageShop(canShop);
        perm.setCanManageFinance(canFinance);
        perm.setCanManageMinigame(canMinigame);
        perm.setUpdatedAt(LocalDateTime.now());
        staffPermissionRepository.save(perm);

        return staff;
    }

    @Transactional
    public StaffPermission updatePermission(Long staffUserId, String department,
                                           boolean canCreate, boolean canEdit, boolean canDelete,
                                           boolean canUsers, boolean canServices, boolean canVieker,
                                           boolean canDispute, boolean canShop, boolean canFinance,
                                           boolean canMinigame) {
        User staff = userRepository.findById(staffUserId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhân viên với ID: " + staffUserId));

        StaffPermission perm = staffPermissionRepository.findByUserId(staff.getId())
                .orElseGet(() -> new StaffPermission(staff));

        if (department != null && !department.isBlank()) {
            perm.setDepartment(department.trim());
        }
        perm.setCanCreate(canCreate);
        perm.setCanEdit(canEdit);
        perm.setCanDelete(canDelete);
        perm.setCanManageUsers(canUsers);
        perm.setCanManageServices(canServices);
        perm.setCanApproveVieker(canVieker);
        perm.setCanResolveDispute(canDispute);
        perm.setCanManageShop(canShop);
        perm.setCanManageFinance(canFinance);
        perm.setCanManageMinigame(canMinigame);
        perm.setUpdatedAt(LocalDateTime.now());

        return staffPermissionRepository.save(perm);
    }
}
