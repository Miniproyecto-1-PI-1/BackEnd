package com.miniproyecto.backend.controller;

import com.miniproyecto.backend.dto.AvatarRequest;
import com.miniproyecto.backend.dto.ChangePasswordRequest;
import com.miniproyecto.backend.dto.DeleteAccountRequest;
import com.miniproyecto.backend.dto.UpdateProfileRequest;
import com.miniproyecto.backend.dto.UserResponse;
import com.miniproyecto.backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Usuario", description = "Perfil, contraseña, foto y cuenta del usuario autenticado")
@RestController
@RequestMapping("/api/users/me")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Editar nombre y correo (cambiar el correo exige la contraseña actual)")
    @ApiResponse(responseCode = "409", description = "El correo ya está registrado")
    @PutMapping
    public UserResponse updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(request);
    }

    @Operation(summary = "Cambiar la contraseña indicando la actual")
    @ApiResponse(responseCode = "204", description = "Contraseña actualizada")
    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Subir la foto de perfil como data URL (JPG, PNG o WebP)")
    @PutMapping("/avatar")
    public UserResponse updateAvatar(@Valid @RequestBody AvatarRequest request) {
        return userService.updateAvatar(request);
    }

    @Operation(summary = "Eliminar la cuenta con todos sus eventos, gestiones y clientes")
    @ApiResponse(responseCode = "204", description = "Cuenta eliminada")
    @DeleteMapping
    public ResponseEntity<Void> deleteAccount(@Valid @RequestBody DeleteAccountRequest request) {
        userService.deleteAccount(request);
        return ResponseEntity.noContent().build();
    }
}
