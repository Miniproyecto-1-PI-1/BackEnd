package com.miniproyecto.backend.controller;

import com.miniproyecto.backend.dto.CreateEventRequest;
import com.miniproyecto.backend.dto.EventDetailResponse;
import com.miniproyecto.backend.dto.EventSummaryResponse;
import com.miniproyecto.backend.service.EventService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import com.miniproyecto.backend.dto.TaskRequest;
import com.miniproyecto.backend.dto.UpdateEventRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping
    public ResponseEntity<EventDetailResponse> create(@Valid @RequestBody CreateEventRequest request) {
        EventDetailResponse created = eventService.create(request);
        URI location = URI.create("/api/events/" + created.id());
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public List<EventSummaryResponse> list(@RequestParam(required = false) String q) {
        return eventService.list(q);
    }

    @GetMapping("/{id}")
    public EventDetailResponse getById(@PathVariable Long id) {
        return eventService.getById(id);
    }

    @PutMapping("/{id}")
    public EventDetailResponse update(@PathVariable Long id, @Valid @RequestBody UpdateEventRequest request) {
        return eventService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        eventService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/tasks")
    public ResponseEntity<EventDetailResponse> addTask(@PathVariable Long id, @Valid @RequestBody TaskRequest request) {
        return ResponseEntity.status(201).body(eventService.addTask(id, request));
    }

    @PutMapping("/{id}/tasks/{taskId}")
    public EventDetailResponse updateTask(
            @PathVariable Long id,
            @PathVariable Long taskId,
            @Valid @RequestBody TaskRequest request
    ) {
        return eventService.updateTask(id, taskId, request);
    }

    @DeleteMapping("/{id}/tasks/{taskId}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id, @PathVariable Long taskId) {
        eventService.deleteTask(id, taskId);
        return ResponseEntity.noContent().build();
    }
}
