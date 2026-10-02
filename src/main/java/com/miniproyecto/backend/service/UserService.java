package com.miniproyecto.backend.service;

import com.miniproyecto.backend.dto.AvatarRequest;
import com.miniproyecto.backend.dto.ChangePasswordRequest;
import com.miniproyecto.backend.dto.DeleteAccountRequest;
import com.miniproyecto.backend.dto.UpdateProfileRequest;
import com.miniproyecto.backend.dto.UserResponse;
import com.miniproyecto.backend.entity.AppUser;
import com.miniproyecto.backend.exception.ConflictException;
import com.miniproyecto.backend.exception.FieldErrorException;
import com.miniproyecto.backend.exception.UnauthorizedException;
import com.miniproyecto.backend.repository.AppUserRepository;
import com.miniproyecto.backend.repository.ClientRepository;
import com.miniproyecto.backend.repository.EventRepository;
import com.miniproyecto.backend.security.CurrentUser;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

@Service
public class UserService {

    /** Unos 300 KB de base64; el frontend envía JPEG 256x256 de ~30 KB. */
    static final int MAX_AVATAR_LENGTH = 400_000;
    private static final Pattern AVATAR_PATTERN =
            Pattern.compile("^data:image/(jpeg|png|webp);base64,[A-Za-z0-9+/]+={0,2}$");
    private static final String WRONG_PASSWORD = "La contraseña actual no es correcta.";

    private final AppUserRepository appUserRepository;
    private final EventRepository eventRepository;
    private final ClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            AppUserRepository appUserRepository,
            EventRepository eventRepository,
            ClientRepository clientRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.appUserRepository = appUserRepository;
        this.eventRepository = eventRepository;
        this.clientRepository = clientRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse updateProfile(UpdateProfileRequest request) {
        AppUser user = currentUser();
        String email = AuthService.normalizeEmail(request.email());

        if (!email.equals(user.getEmail())) {
            if (request.currentPassword() == null || request.currentPassword().isBlank()) {
                throw new FieldErrorException("currentPassword", "Ingresa tu contraseña para cambiar el correo.");
            }
            requirePassword(user, request.currentPassword(), "currentPassword");
            if (appUserRepository.existsByEmailAndIdNot(email, user.getId())) {
                throw new ConflictException("Ese correo ya está registrado");
            }
            user.setEmail(email);
        }
        user.setName(request.name().trim());

        return AuthService.toUserResponse(appUserRepository.save(user));
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        AppUser user = currentUser();
        requirePassword(user, request.currentPassword(), "currentPassword");
        if (request.newPassword().equals(request.currentPassword())) {
            throw new FieldErrorException("newPassword", "La nueva contraseña debe ser distinta a la actual.");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        appUserRepository.save(user);
    }

    @Transactional
    public UserResponse updateAvatar(AvatarRequest request) {
        String image = request.image().trim();
        if (image.length() > MAX_AVATAR_LENGTH) {
            throw new FieldErrorException("image", "La imagen es demasiado grande.");
        }
        if (!AVATAR_PATTERN.matcher(image).matches()) {
            throw new FieldErrorException("image", "El archivo debe ser una imagen JPG, PNG o WebP.");
        }
        AppUser user = currentUser();
        user.setAvatar(image);
        return AuthService.toUserResponse(appUserRepository.save(user));
    }

    /** Borra la cuenta con todos sus eventos (y gestiones, por cascada) y clientes. */
    @Transactional
    public void deleteAccount(DeleteAccountRequest request) {
        AppUser user = currentUser();
        requirePassword(user, request.password(), "password");

        eventRepository.deleteAll(eventRepository.findByUser_Id(user.getId()));
        clientRepository.deleteByUser_Id(user.getId());
        appUserRepository.delete(user);
    }

    private AppUser currentUser() {
        return appUserRepository.findById(CurrentUser.id())
                .orElseThrow(() -> new UnauthorizedException("Sesión no válida"));
    }

    private void requirePassword(AppUser user, String rawPassword, String field) {
        if (rawPassword == null || !passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new FieldErrorException(field, WRONG_PASSWORD);
        }
    }
}
