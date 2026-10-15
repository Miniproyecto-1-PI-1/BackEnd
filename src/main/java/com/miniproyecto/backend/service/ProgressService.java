package com.miniproyecto.backend.service;

import com.miniproyecto.backend.dto.EventProgressResponse;
import com.miniproyecto.backend.dto.ProgressResponse;
import com.miniproyecto.backend.entity.Event;
import com.miniproyecto.backend.entity.TaskStatus;
import com.miniproyecto.backend.exception.NotFoundException;
import com.miniproyecto.backend.repository.EventRepository;
import com.miniproyecto.backend.repository.TaskRepository;
import com.miniproyecto.backend.repository.TaskRepository.ProgressRow;
import com.miniproyecto.backend.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
public class ProgressService {

    private final EventRepository eventRepository;
    private final TaskRepository taskRepository;

    public ProgressService(EventRepository eventRepository, TaskRepository taskRepository) {
        this.eventRepository = eventRepository;
        this.taskRepository = taskRepository;
    }

    @Transactional(readOnly = true)
    public ProgressResponse getProgress() {
        return getProgress(LocalDate.now());
    }

    @Transactional(readOnly = true)
    public EventProgressResponse getEventProgress(Long eventId) {
        return getEventProgress(LocalDate.now(), eventId);
    }

    ProgressResponse getProgress(LocalDate today) {
        Long userId = CurrentUser.id();
        List<ProgressRow> rows = taskRepository.findProgressRows(userId);
        List<EventProgressResponse> events = eventRepository.findByUser_Id(userId).stream()
                .sorted(Comparator.comparing(Event::getEventDate).thenComparing(Event::getName))
                .map(event -> count(event, rows, today))
                .toList();

        long done = events.stream().mapToLong(EventProgressResponse::done).sum();
        long pending = events.stream().mapToLong(EventProgressResponse::pending).sum();
        long postponed = events.stream().mapToLong(EventProgressResponse::postponed).sum();
        long overdue = events.stream().mapToLong(EventProgressResponse::overdue).sum();
        long total = done + pending + postponed + overdue;
        return new ProgressResponse(total, done, pending, postponed, overdue, percent(done, total), events);
    }

    EventProgressResponse getEventProgress(LocalDate today, Long eventId) {
        Long userId = CurrentUser.id();
        Event event = eventRepository.findByIdAndUser_Id(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Evento no encontrado"));
        return count(event, taskRepository.findProgressRows(userId), today);
    }

    private static EventProgressResponse count(Event event, List<ProgressRow> rows, LocalDate today) {
        long done = 0;
        long pending = 0;
        long postponed = 0;
        long overdue = 0;
        for (ProgressRow row : rows) {
            if (!row.getEventId().equals(event.getId())) {
                continue;
            }
            if (row.getStatus() == TaskStatus.DONE) {
                done++;
            } else if (row.getDueDate().isBefore(today)) {
                overdue++;
            } else if (row.getStatus() == TaskStatus.POSTPONED) {
                postponed++;
            } else {
                pending++;
            }
        }
        long total = done + pending + postponed + overdue;
        return new EventProgressResponse(
                event.getId(), event.getName(), event.getEventDate(),
                total, done, pending, postponed, overdue, percent(done, total));
    }

    static int percent(long done, long total) {
        return total == 0 ? 0 : (int) Math.round(done * 100.0 / total);
    }
}
