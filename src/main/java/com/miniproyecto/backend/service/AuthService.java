package com.miniproyecto.backend.service;

import com.miniproyecto.backend.dto.AuthResponse;
import com.miniproyecto.backend.dto.LoginRequest;
import com.miniproyecto.backend.dto.RegisterRequest;
import com.miniproyecto.backend.dto.UserResponse;
import com.miniproyecto.backend.entity.AppUser;
import com.miniproyecto.backend.exception.ConflictException;
import com.miniproyecto.backend.exception.UnauthorizedException;
import com.miniproyecto.backend.repository.AppUserRepository;
import com.miniproyecto.backend.security.CurrentUser;
import com.miniproyecto.backend.security.TokenService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;

@Service
public class AuthService {

    private static final String BAD_CREDENTIALS = "Correo o contraseña incorrectos";

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public AuthService(
            AppUserRepository appUserRepository,
            PasswordEncoder passwordEncoder,
            TokenService tokenService
    ) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (appUserRepository.existsByEmail(email)) {
            throw new ConflictException("Ese correo ya está registrado");
        }

        AppUser user = new AppUser();
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setLastLoginAt(LocalDateTime.now());

        return toAuthResponse(appUserRepository.save(user));
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        AppUser user = appUserRepository.findByEmail(normalizeEmail(request.email()))
                .filter(found -> passwordEncoder.matches(request.password(), found.getPasswordHash()))
                .orElseThrow(() -> new UnauthorizedException(BAD_CREDENTIALS));

        user.setLastLoginAt(LocalDateTime.now());
        return toAuthResponse(appUserRepository.save(user));
    }

    @Transactional(readOnly = true)
    public UserResponse me() {
        return appUserRepository.findById(CurrentUser.id())
                .map(AuthService::toUserResponse)
                .orElseThrow(() -> new UnauthorizedException("Sesión no válida"));
    }

    private AuthResponse toAuthResponse(AppUser user) {
        return new AuthResponse(tokenService.issue(user), toUserResponse(user));
    }

    static UserResponse toUserResponse(AppUser user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getAvatar(), user.getDailyHourLimit());
    }

    static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
