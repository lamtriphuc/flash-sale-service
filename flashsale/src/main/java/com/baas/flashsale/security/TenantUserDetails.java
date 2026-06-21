package com.baas.flashsale.security;

import com.baas.flashsale.tenant.entity.User;
import com.baas.flashsale.tenant.entity.UserRole;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class TenantUserDetails implements UserDetails {
    private final Long id;
    private final Long tenantId;
    private final String tenantCode;
    private final String username;
    private final String password;
    private final String fullName;
    private final UserRole role;
    private final boolean active;
    private final boolean tenantActive;
    private final Collection<? extends GrantedAuthority> authorities;

    public TenantUserDetails(User user) {
        this.id = user.getId();
        this.tenantId = user.getTenant().getId();
        this.tenantCode = user.getTenant().getCode();
        this.username = user.getUsername();
        this.password = user.getPasswordHash();
        this.fullName = user.getFullName();
        this.role = user.getRole();
        this.active = Boolean.TRUE.equals(user.getActive());
        this.tenantActive = Boolean.TRUE.equals(user.getTenant().getActive());
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return active;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active && tenantActive;
    }
}
