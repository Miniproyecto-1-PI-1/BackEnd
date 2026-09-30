package com.miniproyecto.backend.service;

import com.miniproyecto.backend.dto.AuthResponse;
import com.miniproyecto.backend.dto.LoginRequest;
import com.miniproyecto.backend.dto.RegisterRequest;
import com.miniproyecto.backend.entity.AppUser;
import com.miniproyecto.backend.exception.ConflictException;
import com.miniproyecto.backend.exception.UnauthorizedException;
import com.miniproyecto.backend.repository.AppUserRepository;
import com.miniproyecto.backend.security.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private TokenService tokenService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(appUserRepository, passwordEncoder, tokenService);
    }

    private void stubSave() {
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(invocation -> {
            AppUser user = invocation.getArgument(0);
            if (user.getId() == null) {
                user.setId(7L);
            }
            return user;
        });
        when(tokenService.issue(any(AppUser.class))).thenReturn("jwt");
    }

    private AppUser existingUser(String rawPassword) {
        AppUser user = new AppUser();
        user.setId(1L);
        user.setName("Valentina");
        user.setEmail("valentina@eventosvv.co");
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        return user;
    }

    @Test
    void registerHashesPasswordAndNormalizesEmail() {
        when(appUserRepository.existsByEmail("ana@correo.com")).thenReturn(false);
        stubSave();

        AuthResponse response = authService.register(new RegisterRequest(" Ana ", " Ana@Correo.com ", "secreto1"));

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository).save(captor.capture());
        AppUser saved = captor.getValue();
        assertThat(saved.getName()).isEqualTo("Ana");
        assertThat(saved.getEmail()).isEqualTo("ana@correo.com");
        assertThat(saved.getPasswordHash()).isNotEqualTo("secreto1");
        assertThat(passwordEncoder.matches("secreto1", saved.getPasswordHash())).isTrue();
        assertThat(saved.getLastLoginAt()).isNotNull();
        assertThat(response.token()).isEqualTo("jwt");
        assertThat(response.user().id()).isEqualTo(7L);
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(appUserRepository.existsByEmail("valentina@eventosvv.co")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("Valentina", "valentina@eventosvv.co", "secreto1")))
                .isInstanceOf(ConflictException.class);
        verify(appUserRepository, never()).save(any());
    }

    @Test
    void loginReturnsTokenWithValidCredentials() {
        when(appUserRepository.findByEmail("valentina@eventosvv.co"))
                .thenReturn(Optional.of(existingUser("secreto1")));
        stubSave();

        AuthResponse response = authService.login(new LoginRequest("Valentina@EventosVV.co", "secreto1"));

        assertThat(response.token()).isEqualTo("jwt");
        assertThat(response.user().email()).isEqualTo("valentina@eventosvv.co");
    }

    @Test
    void loginRejectsWrongPassword() {
        when(appUserRepository.findByEmail("valentina@eventosvv.co"))
                .thenReturn(Optional.of(existingUser("secreto1")));

        assertThatThrownBy(() -> authService.login(new LoginRequest("valentina@eventosvv.co", "otra")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Correo o contraseña incorrectos");
    }

    @Test
    void loginRejectsUnknownEmail() {
        when(appUserRepository.findByEmail("nadie@correo.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("nadie@correo.com", "secreto1")))
                .isInstanceOf(UnauthorizedException.class);
    }
}
