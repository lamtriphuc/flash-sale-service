package com.baas.flashsale.tenant.controller;

import com.baas.flashsale.tenant.dto.*;
import com.baas.flashsale.tenant.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.baas.flashsale.security.TenantUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserResponse> createUser(
            @AuthenticationPrincipal TenantUserDetails currentUser,
            @Valid @RequestBody CreateUserRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(currentUser.getTenantId(), request));
    }

    @GetMapping
    public List<UserResponse> getUsersByTenant(
            @AuthenticationPrincipal TenantUserDetails currentUser
    ) {
        return userService.getUsersByTenant(currentUser.getTenantId());
    }

    @GetMapping("/{id}")
    public UserResponse getUserById(
            @AuthenticationPrincipal TenantUserDetails currentUser,
            @PathVariable Long id
    ) {
        return userService.getUserById(currentUser.getTenantId(), id);
    }

    @PutMapping("/{id}")
    public UserResponse updateUser(
            @AuthenticationPrincipal TenantUserDetails currentUser,
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        return userService.updateUser(currentUser.getTenantId(), id, request);
    }

    @PatchMapping("/{id}/deactivate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivateUser(
            @AuthenticationPrincipal TenantUserDetails currentUser,
            @PathVariable Long id
    ) {
        userService.deactivateUser(currentUser.getTenantId(), id);
    }

}
