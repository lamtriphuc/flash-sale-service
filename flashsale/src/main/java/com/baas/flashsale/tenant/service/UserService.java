package com.baas.flashsale.tenant.service;

import com.baas.flashsale.common.BusinessException;
import com.baas.flashsale.tenant.dto.CreateUserRequest;
import com.baas.flashsale.tenant.dto.UpdateUserRequest;
import com.baas.flashsale.tenant.dto.UserResponse;
import com.baas.flashsale.tenant.entity.Tenant;
import com.baas.flashsale.tenant.entity.User;
import com.baas.flashsale.tenant.repository.TenantRepository;
import com.baas.flashsale.tenant.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

    public UserResponse createUser(Long tenantId, CreateUserRequest request) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new BusinessException("FORBIDDEN_RESOURCE", HttpStatus.FORBIDDEN, "Tenant not found"));

        if (!Boolean.TRUE.equals(tenant.getActive())) {
            throw new BusinessException("TENANT_SUSPENDED", HttpStatus.FORBIDDEN, "Tenant is inactive");
        }

        String username = request.getUsername().trim();

        if (userRepository.existsByTenantIdAndUsername(tenantId, username)) {
            throw new BusinessException("VALIDATION_ERROR", HttpStatus.BAD_REQUEST, "Username already exists in this tenant");
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

    public List<UserResponse> getUsersByTenant(Long tenantId) {
        return userRepository.findByTenantId(tenantId)
                .stream()
                .map(this::mapToUserResponse)
                .toList();
    }

    public UserResponse getUserById(Long tenantId, Long id) {
        User user = findUserById(tenantId, id);
        return mapToUserResponse(user);
    }

    public UserResponse updateUser(Long tenantId, Long id, UpdateUserRequest request) {
        User user = findUserById(tenantId, id);

        user.setFullName(request.getFullName().trim());
        user.setRole(request.getRole());
        user.setActive(request.getActive());

        return mapToUserResponse(userRepository.save(user));
    }

    public void deactivateUser(Long tenantId, Long id) {
        User user = findUserById(tenantId, id);
        user.setActive(false);
        userRepository.save(user);
    }

    private User findUserById(Long tenantId, Long id) {
        return userRepository.findById(id)
                .filter(user -> user.getTenant().getId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("FORBIDDEN_RESOURCE", HttpStatus.FORBIDDEN, "User does not belong to tenant"));
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
