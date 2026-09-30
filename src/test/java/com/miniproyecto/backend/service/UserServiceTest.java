package com.miniproyecto.backend.service;

import com.miniproyecto.backend.dto.AvatarRequest;
import com.miniproyecto.backend.dto.ChangePasswordRequest;
import com.miniproyecto.backend.dto.DeleteAccountRequest;
import com.miniproyecto.backend.dto.UpdateProfileRequest;
import com.miniproyecto.backend.dto.UserResponse;
import com.miniproyecto.backend.entity.AppUser;
import com.miniproyecto.backend.entity.Event;
import com.miniproyecto.backend.exception.ConflictException;
import com.miniproyecto.backend.exception.FieldErrorException;
import com.miniproyecto.backend.repository.AppUserRepository;
import com.miniproyecto.backend.repository.ClientRepository;
import com.miniproyecto.backend.repository.EventRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private ClientRepository clientRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);

    private UserService userService;

    private AppUser user;

    @BeforeEach
    void setUp() {
        userService = new UserService(appUserRepository, eventRepository, clientRepository, passwordEncoder);

        user = new AppUser();
        user.setId(1L);
        user.setName("Valentina");
        user.setEmail("valentina@eventosvv.co");
        user.setPasswordHash(passwordEncoder.encode("secreto1"));
        // Las validaciones de formato fallan antes de buscar al usuario.
        lenient().when(appUserRepository.findById(1L)).thenReturn(Optional.of(user));

        Jwt jwt = Jwt.withTokenValue("token").header("alg", "HS256").subject("1").build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void stubSave() {
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private static String fieldOf(Throwable ex) {
        return ((FieldErrorException) ex).getErrors().keySet().iterator().next();
    }

    @Test
    void updateProfileChangingOnlyNameDoesNotAskForPassword() {
        stubSave();

        UserResponse response = userService.updateProfile(
                new UpdateProfileRequest(" Valentina V. ", "Valentina@EventosVV.co", null));

        assertThat(response.name()).isEqualTo("Valentina V.");
        assertThat(response.email()).isEqualTo("valentina@eventosvv.co");
    }

    @Test
    void updateProfileChangingEmailRequiresPassword() {
        assertThatThrownBy(() -> userService.updateProfile(
                new UpdateProfileRequest("Valentina", "nuevo@correo.com", null)))
                .isInstanceOf(FieldErrorException.class)
                .satisfies(ex -> assertThat(fieldOf(ex)).isEqualTo("currentPassword"));
        verify(appUserRepository, never()).save(any());
    }

    @Test
    void updateProfileChangingEmailRejectsWrongPassword() {
        assertThatThrownBy(() -> userService.updateProfile(
                new UpdateProfileRequest("Valentina", "nuevo@correo.com", "otra")))
                .isInstanceOf(FieldErrorException.class);
        verify(appUserRepository, never()).save(any());
    }

    @Test
    void updateProfileRejectsEmailOfAnotherAccount() {
        when(appUserRepository.existsByEmailAndIdNot("nuevo@correo.com", 1L)).thenReturn(true);

        assertThatThrownBy(() -> userService.updateProfile(
                new UpdateProfileRequest("Valentina", "nuevo@correo.com", "secreto1")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void updateProfileChangesEmailWithCorrectPassword() {
        when(appUserRepository.existsByEmailAndIdNot("nuevo@correo.com", 1L)).thenReturn(false);
        stubSave();

        UserResponse response = userService.updateProfile(
                new UpdateProfileRequest("Valentina", "Nuevo@Correo.com", "secreto1"));

        assertThat(response.email()).isEqualTo("nuevo@correo.com");
    }

    @Test
    void changePasswordRejectsWrongCurrentPassword() {
        assertThatThrownBy(() -> userService.changePassword(new ChangePasswordRequest("otra", "nueva123")))
                .isInstanceOf(FieldErrorException.class)
                .satisfies(ex -> assertThat(fieldOf(ex)).isEqualTo("currentPassword"));
        verify(appUserRepository, never()).save(any());
    }

    @Test
    void changePasswordRejectsSamePassword() {
        assertThatThrownBy(() -> userService.changePassword(new ChangePasswordRequest("secreto1", "secreto1")))
                .isInstanceOf(FieldErrorException.class)
                .satisfies(ex -> assertThat(fieldOf(ex)).isEqualTo("newPassword"));
    }

    @Test
    void changePasswordStoresNewHash() {
        stubSave();

        userService.changePassword(new ChangePasswordRequest("secreto1", "nueva123"));

        assertThat(passwordEncoder.matches("nueva123", user.getPasswordHash())).isTrue();
        assertThat(passwordEncoder.matches("secreto1", user.getPasswordHash())).isFalse();
    }

    @Test
    void updateAvatarStoresValidImage() {
        stubSave();
        String image = "data:image/jpeg;base64,/9j/4AAQSkZJRg==";

        UserResponse response = userService.updateAvatar(new AvatarRequest(image));

        assertThat(response.avatar()).isEqualTo(image);
    }

    @Test
    void updateAvatarRejectsNonImage() {
        assertThatThrownBy(() -> userService.updateAvatar(new AvatarRequest("data:text/html;base64,PGgxPg==")))
                .isInstanceOf(FieldErrorException.class)
                .satisfies(ex -> assertThat(fieldOf(ex)).isEqualTo("image"));
    }

    @Test
    void updateAvatarRejectsOversizedImage() {
        String image = "data:image/png;base64," + "A".repeat(UserService.MAX_AVATAR_LENGTH);

        assertThatThrownBy(() -> userService.updateAvatar(new AvatarRequest(image)))
                .isInstanceOf(FieldErrorException.class);
    }

    @Test
    void deleteAccountRejectsWrongPassword() {
        assertThatThrownBy(() -> userService.deleteAccount(new DeleteAccountRequest("otra")))
                .isInstanceOf(FieldErrorException.class)
                .satisfies(ex -> assertThat(fieldOf(ex)).isEqualTo("password"));
        verify(eventRepository, never()).deleteAll(any());
        verify(clientRepository, never()).deleteByUser_Id(any());
        verify(appUserRepository, never()).delete(any());
    }

    @Test
    void deleteAccountRemovesEventsClientsAndUserInOrder() {
        List<Event> events = List.of(new Event());
        when(eventRepository.findByUser_Id(1L)).thenReturn(events);

        userService.deleteAccount(new DeleteAccountRequest("secreto1"));

        InOrder order = inOrder(eventRepository, clientRepository, appUserRepository);
        order.verify(eventRepository).deleteAll(events);
        order.verify(clientRepository).deleteByUser_Id(1L);
        order.verify(appUserRepository).delete(user);
    }
}
