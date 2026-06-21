package com.baas.flashsale.tenant;

import com.baas.flashsale.tenant.dto.CreateUserRequest;
import com.baas.flashsale.tenant.dto.UpdateUserRequest;
import com.baas.flashsale.tenant.dto.UserResponse;
import com.baas.flashsale.tenant.entity.Tenant;
import com.baas.flashsale.tenant.entity.User;
import com.baas.flashsale.tenant.repository.TenantRepository;
import com.baas.flashsale.tenant.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {
    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;

    public UserResponse createUser(CreateUserRequest request) {
        Tenant tenant = tenantRepository.findById(request.getTenantId())
                .orElseThrow(() -> new RuntimeException("Tenant not found"));

        if (!Boolean.TRUE.equals(tenant.getActive())) {
            throw new RuntimeException("Tenant is inactive");
        }

        String username = request.getUsername().trim();

        if (userRepository.existsByTenantIdAndUsername(request.getTenantId(), username)) {
            throw new RuntimeException("Username already exists in this tenant");
        }

        User user = User.builder()
                .tenant(tenant)
                .username(username)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName().trim())
                .role(request.getRole())
                .active(true)
                .build();

        User savedUser = userRepository.save(user);

        return mapToUserResponse(savedUser);
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::mapToUserResponse)
                .toList();
    }

    public List<UserResponse> getUsersByTenant(Long tenantId) {
        tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));

        return userRepository.findByTenantId(tenantId)
                .stream()
                .map(this::mapToUserResponse)
                .toList();
    }

    public UserResponse getUserById(Long id) {
        User user = findUserById(id);
        return mapToUserResponse(user);
    }

    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        User user = findUserById(id);

        user.setFullName(request.getFullName().trim());
        user.setRole(request.getRole());
        user.setActive(request.getActive());

        return mapToUserResponse(userRepository.save(user));
    }

    public void deactivateUser(Long id) {
        User user = findUserById(id);
        user.setActive(false);
        userRepository.save(user);
    }

    private User findUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .tenantId(user.getTenant().getId())
                .tenantCode(user.getTenant().getCode())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .role(user.getRole())
                .active(user.getActive())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
