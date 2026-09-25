package com.miniproyecto.backend.service;

import com.miniproyecto.backend.dto.ClientResponse;
import com.miniproyecto.backend.dto.CreateEventRequest;
import com.miniproyecto.backend.dto.EventDetailResponse;
import com.miniproyecto.backend.dto.EventSummaryResponse;
import com.miniproyecto.backend.dto.TaskRequest;
import com.miniproyecto.backend.dto.TaskResponse;
import com.miniproyecto.backend.entity.AppUser;
import com.miniproyecto.backend.entity.Client;
import com.miniproyecto.backend.entity.Event;
import com.miniproyecto.backend.entity.Task;
import com.miniproyecto.backend.entity.TaskStatus;
import com.miniproyecto.backend.exception.NotFoundException;
import com.miniproyecto.backend.repository.AppUserRepository;
import com.miniproyecto.backend.repository.EventRepository;
import com.miniproyecto.backend.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class EventService {

    private static final BigDecimal DEFAULT_ESTIMATED_HOURS = new BigDecimal("1.00");

    private final EventRepository eventRepository;
    private final AppUserRepository appUserRepository;
    private final ClientService clientService;

    public EventService(
            EventRepository eventRepository,
            AppUserRepository appUserRepository,
            ClientService clientService
    ) {
        this.eventRepository = eventRepository;
        this.appUserRepository = appUserRepository;
        this.clientService = clientService;
    }

    @Transactional
    public EventDetailResponse create(CreateEventRequest request) {
        AppUser user = appUserRepository.findById(CurrentUser.id())
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        Client client = clientService.findOrCreate(user, request.client());

        Event event = new Event();
        event.setUser(user);
        event.setClient(client);
        event.setName(request.name().trim());
        event.setDescription(request.description());
        event.setEventDate(request.date());
        event.setEventTime(request.time() != null ? request.time() : LocalTime.MIDNIGHT);
        event.setPlace(request.place().trim());

        if (request.tasks() != null) {
            for (TaskRequest taskRequest : request.tasks()) {
                Task task = toTask(taskRequest);
                task.setEvent(event);
                event.getTasks().add(task);
            }
        }

        return toDetail(eventRepository.save(event));
    }

    @Transactional(readOnly = true)
    public List<EventSummaryResponse> list(String q) {
        String query = (q == null || q.isBlank()) ? null : q.trim();
        return eventRepository.findSummariesByUserId(CurrentUser.id(), query).stream()
                .map(row -> {
                    long total = row.getTotalTasks() == null ? 0 : row.getTotalTasks();
                    long done = row.getDoneTasks() == null ? 0 : row.getDoneTasks();
                    return new EventSummaryResponse(
                            row.getId(),
                            row.getName(),
                            row.getDate(),
                            row.getClientName(),
                            total,
                            done,
                            progress(total, done)
                    );
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public EventDetailResponse getById(Long id) {
        Event event = eventRepository.findDetailByIdAndUserId(id, CurrentUser.id())
                .orElseThrow(() -> new NotFoundException("Evento no encontrado"));
        return toDetail(event);
    }

    private Task toTask(TaskRequest request) {
        Task task = new Task();
        task.setName(request.name().trim());
        task.setDescription(request.description());
        task.setDueDate(request.dueDate() != null ? request.dueDate() : LocalDate.now());
        task.setStartTime(request.startTime());
        task.setEndTime(request.endTime());
        task.setEstimatedHours(estimatedHours(request.startTime(), request.endTime()));
        task.setStatus(TaskStatus.PENDING);
        return task;
    }

    BigDecimal estimatedHours(LocalTime startTime, LocalTime endTime) {
        if (startTime == null || endTime == null || !endTime.isAfter(startTime)) {
            return DEFAULT_ESTIMATED_HOURS;
        }
        long minutes = Duration.between(startTime, endTime).toMinutes();
        return BigDecimal.valueOf(minutes)
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    int progress(long totalTasks, long doneTasks) {
        if (totalTasks == 0) {
            return 0;
        }
        return (int) Math.round(doneTasks * 100.0 / totalTasks);
    }

    private EventDetailResponse toDetail(Event event) {
        List<TaskResponse> tasks = event.getTasks() == null
                ? List.of()
                : event.getTasks().stream().map(this::toTaskResponse).toList();
        long total = tasks.size();
        long done = tasks.stream().filter(task -> task.status() == TaskStatus.DONE).count();
        return new EventDetailResponse(
                event.getId(),
                event.getName(),
                event.getDescription(),
                event.getEventDate(),
                event.getEventTime(),
                event.getPlace(),
                toClientResponse(event.getClient()),
                new ArrayList<>(tasks),
                total,
                done,
                progress(total, done)
        );
    }

    private ClientResponse toClientResponse(Client client) {
        if (client == null) {
            return null;
        }
        return new ClientResponse(client.getId(), client.getName(), client.getPhone(), client.getEmail());
    }

    private TaskResponse toTaskResponse(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getName(),
                task.getDescription(),
                task.getDueDate(),
                task.getStartTime(),
                task.getEndTime(),
                task.getEstimatedHours(),
                task.getStatus()
        );
    }
}
