package vn.vieshift.service.impl;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.vieshift.dto.RegisterRequest;
import vn.vieshift.entity.Role;
import vn.vieshift.entity.User;
import vn.vieshift.entity.ViekerProfile;
import vn.vieshift.entity.Wallet;
import vn.vieshift.exception.ResourceNotFoundException;
import vn.vieshift.repository.UserRepository;
import vn.vieshift.repository.ViekerProfileRepository;
import vn.vieshift.repository.WalletRepository;
import vn.vieshift.service.UserService;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final ViekerProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository,
                           WalletRepository walletRepository,
                           ViekerProfileRepository profileRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.profileRepository = profileRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public User register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail().trim().toLowerCase())) {
            throw new IllegalArgumentException("Email này đã được sử dụng trên hệ thống");
        }

        User user = new User(
                request.getEmail().trim().toLowerCase(),
                passwordEncoder.encode(request.getPassword()),
                request.getFullName().trim(),
                request.getPhone().trim(),
                request.getRole()
        );
        User savedUser = userRepository.save(user);

        // Khởi tạo ví tiền cho người dùng mới
        Wallet wallet = new Wallet(savedUser);
        walletRepository.save(wallet);

        // Nếu là Thợ (Vieker), khởi tạo hồ sơ nghiệp vụ KYC
        if (savedUser.getRole() == Role.VIEKER) {
            ViekerProfile profile = new ViekerProfile(savedUser);
            if (request.getIdCardNumber() != null && !request.getIdCardNumber().isBlank()) {
                profile.setIdCardNumber(request.getIdCardNumber().trim());
            }
            if (request.getSkillsDescription() != null && !request.getSkillsDescription().isBlank()) {
                profile.setSkillsDescription(request.getSkillsDescription().trim());
            }
            if (request.getExperienceYears() != null) {
                profile.setExperienceYears(request.getExperienceYears());
            }
            if (request.getOperatingZone() != null && !request.getOperatingZone().isBlank()) {
                profile.setOperatingZone(request.getOperatingZone().trim());
            }
            profileRepository.save(profile);
        }

        return savedUser;
    }

    @Override
    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng có ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email.trim().toLowerCase());
    }

    @Override
    @Transactional(readOnly = true)
    public User getByEmail(String email) {
        return findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng có Email: " + email));
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> getUsersByRole(Role role) {
        return userRepository.findByRoleOrderByCreatedAtDesc(role);
    }

    @Override
    public void toggleUserStatus(Long userId) {
        User user = findById(userId);
        user.setActive(!user.isActive());
        userRepository.save(user);
    }
}
