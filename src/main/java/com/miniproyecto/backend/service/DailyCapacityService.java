package com.miniproyecto.backend.service;

import com.miniproyecto.backend.exception.OverloadConflictException;
import com.miniproyecto.backend.exception.UnauthorizedException;
import com.miniproyecto.backend.repository.AppUserRepository;
import com.miniproyecto.backend.repository.TaskRepository;
import com.miniproyecto.backend.repository.TaskRepository.DayLoad;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Comprueba que las horas planificadas de un día no superen el límite diario del usuario. */
@Service
public class DailyCapacityService {

    static final int MAX_SUGGESTIONS = 3;
    static final int SUGGESTION_WINDOW_DAYS = 60;
    private static final long NO_TASK = -1L;

    public record PlannedTask(LocalDate date, BigDecimal hours) {
    }

    private final TaskRepository taskRepository;
    private final AppUserRepository appUserRepository;

    public DailyCapacityService(TaskRepository taskRepository, AppUserRepository appUserRepository) {
        this.taskRepository = taskRepository;
        this.appUserRepository = appUserRepository;
    }

    /**
     * Verifica que ubicar {@code hours} en {@code date} no pase del límite. {@code excludeTaskId} es la gestión
     * que se está moviendo o editando (null si es nueva); {@code eventDate} acota las fechas sugeridas.
     */
    public void ensureFits(
            Long userId, LocalDate today, LocalDate date, BigDecimal hours, Long excludeTaskId, LocalDate eventDate
    ) {
        long excludeId = excludeTaskId == null ? NO_TASK : excludeTaskId;
        int limit = limitOf(userId);

        BigDecimal planned = nz(taskRepository.sumPlannedHours(userId, date, excludeId));
        BigDecimal resulting = planned.add(hours);
        if (resulting.compareTo(BigDecimal.valueOf(limit)) <= 0) {
            return;
        }
        throw new OverloadConflictException(
                date, planned, hours, resulting, limit,
                suggestDates(userId, today, eventDate, excludeId, hours, limit));
    }

    /** Para un evento nuevo: suma las gestiones que caen el mismo día entre sí y con lo que ya existe. */
    public void ensureNewEventFits(Long userId, List<PlannedTask> tasks) {
        if (tasks.isEmpty()) {
            return;
        }
        int limit = limitOf(userId);
        Map<LocalDate, BigDecimal> requested = new TreeMap<>();
        for (PlannedTask task : tasks) {
            requested.merge(task.date(), task.hours(), BigDecimal::add);
        }
        for (Map.Entry<LocalDate, BigDecimal> day : requested.entrySet()) {
            BigDecimal planned = nz(taskRepository.sumPlannedHours(userId, day.getKey(), NO_TASK));
            BigDecimal resulting = planned.add(day.getValue());
            if (resulting.compareTo(BigDecimal.valueOf(limit)) > 0) {
                throw new OverloadConflictException(
                        day.getKey(), planned, day.getValue(), resulting, limit, List.of());
            }
        }
    }

    private int limitOf(Long userId) {
        return appUserRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Sesión no válida"))
                .getDailyHourLimit();
    }

    private static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /** Próximos días (hasta el evento) donde la gestión cabe dentro del límite diario. */
    private List<LocalDate> suggestDates(
            Long userId, LocalDate today, LocalDate eventDate, long excludeId, BigDecimal hours, int limit
    ) {
        LocalDate last = today.plusDays(SUGGESTION_WINDOW_DAYS);
        if (eventDate != null && eventDate.isBefore(last)) {
            last = eventDate;
        }
        if (last.isBefore(today)) {
            return List.of();
        }

        Map<LocalDate, BigDecimal> load = new HashMap<>();
        for (DayLoad day : taskRepository.sumPlannedHoursByDate(userId, today, last, excludeId)) {
            load.put(day.getDate(), day.getHours());
        }

        BigDecimal max = BigDecimal.valueOf(limit);
        List<LocalDate> suggestions = new ArrayList<>();
        for (LocalDate day = today; !day.isAfter(last) && suggestions.size() < MAX_SUGGESTIONS; day = day.plusDays(1)) {
            if (load.getOrDefault(day, BigDecimal.ZERO).add(hours).compareTo(max) <= 0) {
                suggestions.add(day);
            }
        }
        return suggestions;
    }
}
