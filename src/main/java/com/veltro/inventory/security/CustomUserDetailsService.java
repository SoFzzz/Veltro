package com.veltro.inventory.security;

import com.veltro.inventory.model.UserEntity;
import com.veltro.inventory.domain.iam.ports.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Spring Security {@link UserDetailsService} adapter.
 *
 * Loads a user from the domain port {@link UserRepository} and converts it into
 * a {@link VeltroUserDetails} object that carries userId and businessId
 * for multi-tenant context. Only active users are returned (soft-delete AC-05).
 *
 * The granted authority follows the {@code ROLE_<ROLE>} Spring Security convention.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserEntity user = userRepository.findByUsernameAndActiveTrue(username)
                .orElseThrow(() -> {
                    log.debug("User not found or inactive: {}", username);
                    return new UsernameNotFoundException("User not found: " + username);
                });

        return new VeltroUserDetails(
                user.getUsername(),
                user.getPasswordHash(),
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())),
                user.getId(),
                user.getBusinessId());
    }
}
