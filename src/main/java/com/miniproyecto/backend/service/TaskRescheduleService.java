package com.miniproyecto.backend.service;

import com.miniproyecto.backend.dto.RescheduleTaskRequest;
import com.miniproyecto.backend.dto.TodayCategory;
import com.miniproyecto.backend.dto.TodayTaskResponse;
import com.miniproyecto.backend.entity.Event;
import com.miniproyecto.backend.entity.Task;
import com.miniproyecto.backend.entity.TaskStatus;
import com.miniproyecto.backend.exception.ConflictException;
import com.miniproyecto.backend.exception.FieldErrorException;
import com.miniproyecto.backend.exception.NotFoundException;
import com.miniproyecto.backend.exception.OverloadConflictException;
import com.miniproyecto.backend.exception.UnauthorizedException;
import com.miniproyecto.backend.repository.AppUserRepository;
import com.miniproyecto.backend.repository.EventRepository;
import com.miniproyecto.backend.repository.TaskRepository;
import com.miniproyecto.backend.repository.TaskRepository.DayLoad;
import com.miniproyecto.backend.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TaskRescheduleService {

    static final int MAX_SUGGESTIONS = 3;
    static final int SUGGESTION_WINDOW_DAYS = 60;

    private final EventRepository eventRepository;
    private final TaskRepository taskRepository;
    private final AppUserRepository appUserRepository;

    public TaskRescheduleService(
            EventRepository eventRepository,
            TaskRepository taskRepository,
            AppUserRepository appUserRepository
    ) {
        this.eventRepository = eventRepository;
        this.taskRepository = taskRepository;
        this.appUserRepository = appUserRepository;
    }

    @Transactional
    public TodayTaskResponse reschedule(Long eventId, Long taskId, RescheduleTaskRequest request) {
        return reschedule(LocalDate.now(), eventId, taskId, request);
    }

    TodayTaskResponse reschedule(LocalDate today, Long eventId, Long taskId, RescheduleTaskRequest request) {
        Long userId = CurrentUser.id();
        Event event = eventRepository.findDetailByIdAndUserId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Evento no encontrado"));
        Task task = event.getTasks().stream()
                .filter(t -> t.getId().equals(taskId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Gestión no encontrada"));

        if (task.getStatus() == TaskStatus.DONE) {
            throw new ConflictException("Una gestión ejecutada no se puede reprogramar.");
        }

        LocalDate newDate = request.dueDate();
        if (newDate.isBefore(today)) {
            throw new FieldErrorException("dueDate", "La fecha límite no puede ser anterior al día de hoy.");
        }
        if (event.getEventDate() != null && newDate.isAfter(event.getEventDate())) {
            throw new FieldErrorException("dueDate", "La fecha límite no puede ser posterior al evento.");
        }

        BigDecimal hours = request.estimatedHours() != null
                ? request.estimatedHours().setScale(2, RoundingMode.HALF_UP)
                : task.getEstimatedHours();
        boolean dateChanged = !newDate.equals(task.getDueDate());
        boolean hoursChanged = hours.compareTo(task.getEstimatedHours()) != 0;

        if (dateChanged || hoursChanged) {
            checkCapacity(userId, today, event, task, newDate, hours);
        }

        task.setDueDate(newDate);
        task.setEstimatedHours(hours);
        if (dateChanged && task.getStatus() == TaskStatus.PENDING) {
            task.setStatus(TaskStatus.POSTPONED);
        }
        eventRepository.save(event);

        return toResponse(task, event, today);
    }

    private void checkCapacity(Long userId, LocalDate today, Event event, Task task, LocalDate date, BigDecimal hours) {
        int limit = appUserRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Sesión no válida"))
                .getDailyHourLimit();

        BigDecimal planned = taskRepository.sumPlannedHours(userId, date, task.getId());
        if (planned == null) {
            planned = BigDecimal.ZERO;
        }
        BigDecimal resulting = planned.add(hours);
        if (resulting.compareTo(BigDecimal.valueOf(limit)) <= 0) {
            return;
        }
        throw new OverloadConflictException(
                date, planned, hours, resulting, limit,
                suggestDates(userId, today, event, task.getId(), hours, limit));
    }

    /** Próximos días (hasta el evento) donde la gestión cabe dentro del límite diario. */
    private List<LocalDate> suggestDates(
            Long userId, LocalDate today, Event event, Long taskId, BigDecimal hours, int limit
    ) {
        LocalDate last = today.plusDays(SUGGESTION_WINDOW_DAYS);
        if (event.getEventDate() != null && event.getEventDate().isBefore(last)) {
            last = event.getEventDate();
        }
        if (last.isBefore(today)) {
            return List.of();
        }

        Map<LocalDate, BigDecimal> load = new HashMap<>();
        for (DayLoad day : taskRepository.sumPlannedHoursByDate(userId, today, last, taskId)) {
            load.put(day.getDate(), day.getHours());
        }

        BigDecimal max = BigDecimal.valueOf(limit);
        List<LocalDate> suggestions = new ArrayList<>();
        for (LocalDate day = today; !day.isAfter(last) && suggestions.size() < MAX_SUGGESTIONS; day = day.plusDays(1)) {
            BigDecimal planned = load.getOrDefault(day, BigDecimal.ZERO);
            if (planned.add(hours).compareTo(max) <= 0) {
                suggestions.add(day);
            }
        }
        return suggestions;
    }

    private static TodayTaskResponse toResponse(Task task, Event event, LocalDate today) {
        long diff = ChronoUnit.DAYS.between(today, task.getDueDate());
        TodayCategory category = diff < 0 ? TodayCategory.OVERDUE
                : diff == 0 ? TodayCategory.TODAY
                : TodayCategory.UPCOMING;
        return new TodayTaskResponse(
                task.getId(),
                task.getName(),
                task.getDescription(),
                task.getStatus(),
                task.getEstimatedHours(),
                task.getDueDate(),
                task.getStartTime(),
                task.getEndTime(),
                category,
                diff,
                event.getId(),
                event.getName(),
                event.getClient() != null ? event.getClient().getName() : null
        );
    }
}
