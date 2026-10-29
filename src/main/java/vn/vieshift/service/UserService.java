package vn.vieshift.service;

import vn.vieshift.dto.RegisterRequest;
import vn.vieshift.entity.Role;
import vn.vieshift.entity.User;

import java.util.List;
import java.util.Optional;

public interface UserService {
    User register(RegisterRequest request);
    User findById(Long id);
    Optional<User> findByEmail(String email);
    User getByEmail(String email);
    List<User> getUsersByRole(Role role);
    void toggleUserStatus(Long userId);
}
