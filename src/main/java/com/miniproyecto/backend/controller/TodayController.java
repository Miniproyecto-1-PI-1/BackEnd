package com.miniproyecto.backend.controller;

import com.miniproyecto.backend.dto.TodayResponse;
import com.miniproyecto.backend.service.TodayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Hoy", description = "Gestiones urgentes del día y lo próximo")
@RestController
@RequestMapping("/api/today")
public class TodayController {

    private final TodayService todayService;

    public TodayController(TodayService todayService) {
        this.todayService = todayService;
    }

    @Operation(summary = "Gestiones vencidas, de hoy y próximas, en orden de prioridad")
    @GetMapping
    public TodayResponse today(
            @Parameter(description = "Ventana para las próximas, en días (por defecto 7, máximo 60)")
            @RequestParam(required = false) Integer days,
            @Parameter(description = "Si es true, incluye también las gestiones ya ejecutadas")
            @RequestParam(required = false) Boolean incluirHechas) {
        return todayService.getToday(days, incluirHechas);
    }
}
