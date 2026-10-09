package com.miniproyecto.backend.service;

import com.miniproyecto.backend.dto.TodayCategory;
import com.miniproyecto.backend.dto.TodayResponse;
import com.miniproyecto.backend.dto.TodayTaskResponse;
import com.miniproyecto.backend.repository.TaskRepository;
import com.miniproyecto.backend.repository.TaskRepository.TaskTodayView;
import com.miniproyecto.backend.security.CurrentUser;
import com.miniproyecto.backend.util.AppClock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class TodayService {

    static final int DEFAULT_UPCOMING_DAYS = 7;
    static final int MAX_UPCOMING_DAYS = 60;

    private final TaskRepository taskRepository;

    public TodayService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Transactional(readOnly = true)
    public TodayResponse getToday(Integer days, Boolean incluirHechas) {
        return build(AppClock.today(), days, Boolean.TRUE.equals(incluirHechas));
    }

    TodayResponse build(LocalDate today, Integer days, boolean incluirHechas) {
        int window = days == null
                ? DEFAULT_UPCOMING_DAYS
                : Math.max(0, Math.min(days, MAX_UPCOMING_DAYS));

        List<TodayTaskResponse> tasks = taskRepository
                .findTodayTasks(CurrentUser.id(), today.plusDays(window), incluirHechas)
                .stream()
                .map(view -> toResponse(view, today))
                .toList();

        return new TodayResponse(
                today,
                count(tasks, TodayCategory.OVERDUE),
                count(tasks, TodayCategory.TODAY),
                count(tasks, TodayCategory.UPCOMING),
                tasks
        );
    }

    private static long count(List<TodayTaskResponse> tasks, TodayCategory category) {
        return tasks.stream().filter(t -> t.category() == category).count();
    }

    private static TodayTaskResponse toResponse(TaskTodayView view, LocalDate today) {
        long diff = ChronoUnit.DAYS.between(today, view.getDate());
        TodayCategory category = diff < 0 ? TodayCategory.OVERDUE
                : diff == 0 ? TodayCategory.TODAY
                : TodayCategory.UPCOMING;
        return new TodayTaskResponse(
                view.getId(),
                view.getName(),
                view.getDescription(),
                view.getStatus(),
                view.getEstimatedHours(),
                view.getDate(),
                view.getStartTime(),
                view.getEndTime(),
                category,
                diff,
                view.getEventId(),
                view.getEventName(),
                view.getEventDate(),
                view.getClientName()
        );
    }
}
