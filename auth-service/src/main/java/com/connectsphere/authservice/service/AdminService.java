package com.connectsphere.authservice.service;

import com.connectsphere.authservice.dto.AuthResponse;
import com.connectsphere.authservice.dto.UserResponse;
import com.connectsphere.authservice.entity.User;
import com.connectsphere.authservice.entity.UserStatus;
import com.connectsphere.authservice.exception.ResourceNotFoundException;
import com.connectsphere.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final UserRepository userRepository;

    // ========================= GET ALL USERS =========================
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream().map(this::toUserResponse).toList();
    }

    // ========================= SUSPEND USERS =========================
    @Transactional
    public void suspendUser(UUID id) {
        User user = getUserById(id);
        user.setStatus(UserStatus.SUSPENDED);
        user.setIsActive(false);
    }

    // ========================= ACTIVATE USERS =========================
    @Transactional
    public void activateUser(UUID id) {
        User user = getUserById(id);
        user.setStatus(UserStatus.ACTIVE);
        user.setIsActive(true);
    }

    // ========================= DELETE USERS =========================
    @Transactional
    public void deleteUser(UUID id) {
        User user = getUserById(id);
        user.setStatus(UserStatus.DELETED);
        user.setIsActive(false);
        user.setIsDeleted(true);
    }

    // ========================= HELPER =========================
    private User getUserById(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User Not Found"));
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getUsername(),
                user.getEmail(),
                user.getBio(),
                user.getProfilePicUrl(),
                user.getRole(),
                user.getStatus(),
                user.getIsDeleted(),
                user.getCreatedAt()
        );
    }
}
