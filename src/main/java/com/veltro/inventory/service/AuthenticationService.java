package com.veltro.inventory.service;

import com.veltro.inventory.config.JwtProperties;
import com.veltro.inventory.dto.auth.ChangePasswordRequest;
import com.veltro.inventory.dto.auth.LoginRequest;
import com.veltro.inventory.dto.auth.LoginResponse;
import com.veltro.inventory.dto.auth.RefreshRequest;
import com.veltro.inventory.exception.NotFoundException;
import com.veltro.inventory.model.UserEntity;
import com.veltro.inventory.repository.UserRepository;
import com.veltro.inventory.security.CustomUserDetailsService;
import com.veltro.inventory.security.JwtTokenProvider;
import com.veltro.inventory.security.VeltroUserDetails;
import com.veltro.inventory.model.BusinessEntity;
import com.veltro.inventory.repository.BusinessRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service dedicated to authentication operations (Login, Refresh, Password Management).
 *
 * <p>Extracted from {@code AuthService} to follow SRP (Single Responsibility Principle).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;
    private final UserRepository userRepository;
    private final BusinessRepository businessRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProperties jwtProperties;

    /**
     * Authenticates username/password and returns a token pair.
     */
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.username());

        String accessToken = jwtTokenProvider.generateAccessToken(userDetails);
        String refreshToken = jwtTokenProvider.generateRefreshToken(userDetails);

        String role = userDetails.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("");

        Long businessId = null;
        if (userDetails instanceof VeltroUserDetails v) {
            businessId = v.getBusinessId();
        }

        UserBusinessInfo info = resolveUserBusinessInfo(request.username(), businessId, role);

        log.info("User '{}' logged in successfully (bid={})", request.username(), businessId);

        return LoginResponse.of(
                accessToken,
                refreshToken,
                jwtProperties.accessTokenExpiration(),
                request.username(),
                role,
                businessId,
                info.email(),
                info.businessName(),
                info.adminName());
    }

    /**
     * Validates a Refresh token and issues a new Access token.
     */
    @Transactional(readOnly = true)
    public LoginResponse refresh(RefreshRequest request) {
        String token = request.refreshToken();

        if (!jwtTokenProvider.isValidRefreshToken(token)) {
            throw new IllegalArgumentException("Refresh token is invalid or expired.");
        }

        String username = jwtTokenProvider.extractUsername(token);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        String newAccessToken = jwtTokenProvider.generateAccessToken(userDetails);

        String role = userDetails.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("");

        Long businessId = null;
        if (userDetails instanceof VeltroUserDetails v) {
            businessId = v.getBusinessId();
        }

        UserBusinessInfo info = resolveUserBusinessInfo(username, businessId, role);

        log.debug("Access token refreshed for user '{}'", username);

        return LoginResponse.of(
                newAccessToken,
                token,
                jwtProperties.accessTokenExpiration(),
                username,
                role,
                businessId,
                info.email(),
                info.businessName(),
                info.adminName());
    }

    /**
     * Stateless logout.
     */
    public void logout(String username) {
        log.info("User '{}' logged out (stateless)", username);
    }

    /**
     * Changes user password.
     */
    @Transactional
    public void changePassword(String username, ChangePasswordRequest request) {
        UserEntity user = userRepository.findByUsernameAndActiveTrue(username)
                .orElseThrow(() -> new NotFoundException("User not found: " + username));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        log.info("Password changed successfully for user '{}'", username);
    }

    /**
     * Holds resolved user and business information for authentication responses.
     */
    private record UserBusinessInfo(String email, String businessName, String adminName) {}

    /**
     * Resolves the email, business name, and admin name for the given user.
     *
     * @param username   the username to look up
     * @param businessId the business ID (may be {@code null})
     * @param role       the user's role (e.g. "ADMIN", "CASHIER")
     * @return a {@link UserBusinessInfo} with resolved values, or all-null if no business
     */
    private UserBusinessInfo resolveUserBusinessInfo(String username, Long businessId, String role) {
        if (businessId == null) {
            return new UserBusinessInfo(null, null, null);
        }

        UserEntity userEntity = userRepository.findByUsernameAndActiveTrue(username)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
        String email = userEntity.getEmail();

        String businessName = null;
        String adminName = null;
        BusinessEntity businessEntity = businessRepository.findById(businessId).orElse(null);
        if (businessEntity != null) {
            businessName = businessEntity.getName();
            if (!"ADMIN".equals(role)) {
                if (businessEntity.getOwner() != null) {
                    adminName = businessEntity.getOwner().getUsername();
                }
            }
        }

        return new UserBusinessInfo(email, businessName, adminName);
    }
}
