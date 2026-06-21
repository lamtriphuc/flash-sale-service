package com.baas.flashsale.tenant;

import com.baas.flashsale.tenant.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('OWNER')")
public class UserController {

    private final UserService userService;

    @PostMapping
    public UserResponse createUser(
            @Valid @RequestBody CreateUserRequest request
    ) {
        return userService.createUser(request);
    }

    @GetMapping
    public List<UserResponse> getAllUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/tenant/{tenantId}")
    public List<UserResponse> getUsersByTenant(
            @PathVariable Long tenantId
    ) {
        return userService.getUsersByTenant(tenantId);
    }

    @GetMapping("/{id}")
    public UserResponse getUserById(
            @PathVariable Long id
    ) {
        return userService.getUserById(id);
    }

    @PutMapping("/{id}")
    public UserResponse updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        return userService.updateUser(id, request);
    }

    @PatchMapping("/{id}/deactivate")
    public void deactivateUser(
            @PathVariable Long id
    ) {
        userService.deactivateUser(id);
    }

}
