package com.miniproyecto.backend.controller;

import com.miniproyecto.backend.dto.EventProgressResponse;
import com.miniproyecto.backend.dto.ProgressResponse;
import com.miniproyecto.backend.service.ProgressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Progreso", description = "Avance de preparación de los eventos (gestiones ejecutadas sobre el total)")
@RestController
@RequestMapping("/api")
public class ProgressController {

    private final ProgressService progressService;

    public ProgressController(ProgressService progressService) {
        this.progressService = progressService;
    }

    @Operation(summary = "Progreso general y por evento",
            description = "Cuenta las gestiones hechas, pendientes, pospuestas y vencidas, y el porcentaje de avance "
                    + "(hechas / total). Un usuario sin eventos recibe todo en 0 y una lista vacía.")
    @GetMapping("/progress")
    public ProgressResponse progress() {
        return progressService.getProgress();
    }

    @Operation(summary = "Progreso de un evento")
    @ApiResponse(responseCode = "404", description = "El evento no existe o no es del usuario")
    @GetMapping("/events/{id}/progress")
    public EventProgressResponse eventProgress(@PathVariable Long id) {
        return progressService.getEventProgress(id);
    }
}
