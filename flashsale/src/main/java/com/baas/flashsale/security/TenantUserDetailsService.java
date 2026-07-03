package com.baas.flashsale.security;

import com.baas.flashsale.tenant.entity.User;
import com.baas.flashsale.tenant.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TenantUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return new TenantUserDetails(user);
    }

    @Transactional(readOnly = true)
    public TenantUserDetails loadUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return new TenantUserDetails(user);
    }

    @Transactional(readOnly = true)
    public TenantUserDetails loadUserByTenantIdAndUsername(Long tenantId, String username) {
        User user = userRepository.findByTenantIdAndUsername(tenantId, username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return new TenantUserDetails(user);
    }

    @Transactional(readOnly = true)
    public TenantUserDetails loadUserByTenantCodeAndUsername(String tenantCode, String username) {
        User user = userRepository.findByTenantCodeAndUsername(tenantCode, username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return new TenantUserDetails(user);
    }
}
