package com.miniproyecto.backend.controller;

import com.miniproyecto.backend.dto.AuthResponse;
import com.miniproyecto.backend.dto.LoginRequest;
import com.miniproyecto.backend.dto.RegisterRequest;
import com.miniproyecto.backend.dto.UserResponse;
import com.miniproyecto.backend.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Autenticación", description = "Registro, inicio de sesión y usuario actual")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Crear una cuenta y devolver su token")
    @ApiResponse(responseCode = "201", description = "Cuenta creada")
    @ApiResponse(responseCode = "409", description = "El correo ya está registrado")
    @SecurityRequirements
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(201).body(authService.register(request));
    }

    @Operation(summary = "Iniciar sesión con correo y contraseña")
    @ApiResponse(responseCode = "401", description = "Correo o contraseña incorrectos")
    @SecurityRequirements
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @Operation(summary = "Obtener el usuario del token actual")
    @GetMapping("/me")
    public UserResponse me() {
        return authService.me();
    }
}
