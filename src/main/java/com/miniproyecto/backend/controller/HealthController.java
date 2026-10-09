package com.miniproyecto.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "Salud")
@RestController
@RequestMapping("/api")
public class HealthController {

    @Operation(summary = "Comprobar que el backend está en línea")
    @ApiResponse(responseCode = "200", description = "Backend en línea",
            content = @Content(mediaType = "application/json", examples = @ExampleObject("{\"status\": \"ok\"}")))
    @SecurityRequirements
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "ok"));
    }
}
