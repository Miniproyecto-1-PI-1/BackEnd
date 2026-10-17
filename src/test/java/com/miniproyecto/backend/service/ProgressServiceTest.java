package com.miniproyecto.backend.service;

import com.miniproyecto.backend.dto.EventProgressResponse;
import com.miniproyecto.backend.dto.ProgressResponse;
import com.miniproyecto.backend.entity.Event;
import com.miniproyecto.backend.entity.TaskStatus;
import com.miniproyecto.backend.exception.NotFoundException;
import com.miniproyecto.backend.repository.EventRepository;
import com.miniproyecto.backend.repository.TaskRepository;
import com.miniproyecto.backend.repository.TaskRepository.ProgressRow;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProgressServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 14);

    @Mock
    private EventRepository eventRepository;

    @Mock
    private TaskRepository taskRepository;

    private ProgressService service;

    @BeforeEach
    void setUp() {
        service = new ProgressService(eventRepository, taskRepository);
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "HS256").subject("1").build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private static Event event(long id, String name, int day) {
        Event e = new Event();
        e.setId(id);
        e.setName(name);
        e.setEventDate(LocalDate.of(2026, 11, day));
        return e;
    }

    private static ProgressRow row(long eventId, TaskStatus status, LocalDate dueDate) {
        return new ProgressRow() {
            public Long getEventId() { return eventId; }
            public TaskStatus getStatus() { return status; }
            public LocalDate getDueDate() { return dueDate; }
        };
    }

    private List<ProgressRow> rowsOfEventOne() {
        return List.of(
                row(1, TaskStatus.DONE, TODAY.minusDays(3)),
                row(1, TaskStatus.DONE, TODAY),
                row(1, TaskStatus.PENDING, TODAY),
                row(1, TaskStatus.POSTPONED, TODAY.plusDays(2)),
                row(1, TaskStatus.PENDING, TODAY.minusDays(1)),
                row(1, TaskStatus.POSTPONED, TODAY.minusDays(1)));
    }

    @Test
    void eventProgressSplitsTasksIntoMutuallyExclusiveStates() {
        when(eventRepository.findByIdAndUser_Id(1L, 1L)).thenReturn(Optional.of(event(1, "Boda", 20)));
        when(taskRepository.findProgressRows(1L)).thenReturn(rowsOfEventOne());

        EventProgressResponse p = service.getEventProgress(TODAY, 1L);

        assertThat(p.total()).isEqualTo(6);
        assertThat(p.done()).isEqualTo(2);
        assertThat(p.pending()).isEqualTo(1);
        assertThat(p.postponed()).isEqualTo(1);
        assertThat(p.overdue()).isEqualTo(2);
        assertThat(p.percent()).isEqualTo(33);
        assertThat(p.done() + p.pending() + p.postponed() + p.overdue()).isEqualTo(p.total());
    }

    @Test
    void generalProgressAddsUpEventsAndKeepsEmptyOnesAtZero() {
        when(eventRepository.findByUser_Id(1L)).thenReturn(List.of(event(2, "Fiesta", 25), event(1, "Boda", 20)));
        when(taskRepository.findProgressRows(1L)).thenReturn(rowsOfEventOne());

        ProgressResponse p = service.getProgress(TODAY);

        assertThat(p.total()).isEqualTo(6);
        assertThat(p.done()).isEqualTo(2);
        assertThat(p.percent()).isEqualTo(33);
        assertThat(p.events()).extracting(EventProgressResponse::name).containsExactly("Boda", "Fiesta");
        assertThat(p.events().get(1).total()).isZero();
        assertThat(p.events().get(1).percent()).isZero();
    }

    @Test
    void userWithoutEventsGetsZeroEverywhere() {
        when(eventRepository.findByUser_Id(1L)).thenReturn(List.of());
        when(taskRepository.findProgressRows(1L)).thenReturn(List.of());

        ProgressResponse p = service.getProgress(TODAY);

        assertThat(p.total()).isZero();
        assertThat(p.percent()).isZero();
        assertThat(p.events()).isEmpty();
    }

    @Test
    void progressOfAnEventOfAnotherUserIsNotFound() {
        when(eventRepository.findByIdAndUser_Id(7L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getEventProgress(TODAY, 7L)).isInstanceOf(NotFoundException.class);
    }
}
