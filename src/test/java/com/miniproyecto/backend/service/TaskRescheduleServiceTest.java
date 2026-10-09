package com.miniproyecto.backend.service;

import com.miniproyecto.backend.dto.RescheduleTaskRequest;
import com.miniproyecto.backend.dto.TodayCategory;
import com.miniproyecto.backend.dto.TodayTaskResponse;
import com.miniproyecto.backend.entity.AppUser;
import com.miniproyecto.backend.entity.Event;
import com.miniproyecto.backend.entity.Task;
import com.miniproyecto.backend.entity.TaskStatus;
import com.miniproyecto.backend.exception.ConflictException;
import com.miniproyecto.backend.exception.FieldErrorException;
import com.miniproyecto.backend.exception.NotFoundException;
import com.miniproyecto.backend.exception.OverloadConflictException;
import com.miniproyecto.backend.repository.AppUserRepository;
import com.miniproyecto.backend.repository.EventRepository;
import com.miniproyecto.backend.repository.TaskRepository;
import com.miniproyecto.backend.repository.TaskRepository.DayLoad;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskRescheduleServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 8);
    private static final LocalDate EVENT_DATE = LocalDate.of(2026, 10, 30);

    @Mock
    private EventRepository eventRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private AppUserRepository appUserRepository;

    private TaskRescheduleService service;
    private Event event;
    private Task task;

    @BeforeEach
    void setUp() {
        service = new TaskRescheduleService(eventRepository, taskRepository, appUserRepository);

        AppUser user = new AppUser();
        user.setId(1L);
        user.setDailyHourLimit(6);
        lenient().when(appUserRepository.findById(1L)).thenReturn(Optional.of(user));

        event = new Event();
        event.setId(5L);
        event.setName("Boda Camila & Andrés");
        event.setEventDate(EVENT_DATE);
        task = new Task();
        task.setId(9L);
        task.setName("Confirmar catering");
        task.setEvent(event);
        task.setDueDate(TODAY.minusDays(1));
        task.setEstimatedHours(new BigDecimal("3.00"));
        task.setStatus(TaskStatus.PENDING);
        event.getTasks().add(task);
        lenient().when(eventRepository.findDetailByIdAndUserId(5L, 1L)).thenReturn(Optional.of(event));
        lenient().when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

        Jwt jwt = Jwt.withTokenValue("token").header("alg", "HS256").subject("1").build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private RescheduleTaskRequest to(LocalDate date) {
        return new RescheduleTaskRequest(date, null);
    }

    private static DayLoad load(LocalDate date, String hours) {
        return new DayLoad() {
            public LocalDate getDate() { return date; }
            public BigDecimal getHours() { return new BigDecimal(hours); }
        };
    }

    @Test
    void movesOverdueTaskToTodayAndMarksItPostponed() {
        when(taskRepository.sumPlannedHours(1L, TODAY, 9L)).thenReturn(null);

        TodayTaskResponse response = service.reschedule(TODAY, 5L, 9L, to(TODAY));

        assertThat(response.dueDate()).isEqualTo(TODAY);
        assertThat(response.category()).isEqualTo(TodayCategory.TODAY);
        assertThat(response.status()).isEqualTo(TaskStatus.POSTPONED);
        assertThat(task.getDueDate()).isEqualTo(TODAY);
        verify(eventRepository).save(event);
    }

    @Test
    void movingToTomorrowIsUpcoming() {
        when(taskRepository.sumPlannedHours(1L, TODAY.plusDays(1), 9L)).thenReturn(new BigDecimal("2.00"));

        TodayTaskResponse response = service.reschedule(TODAY, 5L, 9L, to(TODAY.plusDays(1)));

        assertThat(response.category()).isEqualTo(TodayCategory.UPCOMING);
        assertThat(response.daysFromToday()).isEqualTo(1);
    }

    @Test
    void landingExactlyOnTheLimitIsAllowed() {
        when(taskRepository.sumPlannedHours(1L, TODAY, 9L)).thenReturn(new BigDecimal("3.00"));

        TodayTaskResponse response = service.reschedule(TODAY, 5L, 9L, to(TODAY));

        assertThat(response.category()).isEqualTo(TodayCategory.TODAY);
    }

    @Test
    void overloadedDayRaisesConflictWithFiguresAndSuggestions() {
        when(taskRepository.sumPlannedHours(1L, TODAY, 9L)).thenReturn(new BigDecimal("4.00"));
        when(taskRepository.sumPlannedHoursByDate(eq(1L), eq(TODAY), eq(EVENT_DATE), eq(9L)))
                .thenReturn(List.of(load(TODAY, "4.00"), load(TODAY.plusDays(1), "5.00")));

        assertThatThrownBy(() -> service.reschedule(TODAY, 5L, 9L, to(TODAY)))
                .isInstanceOfSatisfying(OverloadConflictException.class, ex -> {
                    assertThat(ex.getMessage()).isEqualTo("Quedarías con 7h planificadas ese día (tu límite es 6h).");
                    assertThat(ex.getPlannedHours()).isEqualByComparingTo("4");
                    assertThat(ex.getTaskHours()).isEqualByComparingTo("3");
                    assertThat(ex.getResultingHours()).isEqualByComparingTo("7");
                    assertThat(ex.getLimitHours()).isEqualTo(6);
                    assertThat(ex.getExceedsBy()).isEqualByComparingTo("1");
                    assertThat(ex.getAvailableHours()).isEqualByComparingTo("2");
                    assertThat(ex.getSuggestedDates()).containsExactly(
                            TODAY.plusDays(2), TODAY.plusDays(3), TODAY.plusDays(4));
                });
        assertThat(task.getDueDate()).isEqualTo(TODAY.minusDays(1));
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void reducingHoursResolvesTheConflict() {
        when(taskRepository.sumPlannedHours(1L, TODAY, 9L)).thenReturn(new BigDecimal("4.00"));

        TodayTaskResponse response = service.reschedule(
                TODAY, 5L, 9L, new RescheduleTaskRequest(TODAY, new BigDecimal("2")));

        assertThat(response.estimatedHours()).isEqualByComparingTo("2");
        assertThat(task.getEstimatedHours()).isEqualByComparingTo("2");
    }

    @Test
    void pastDateIsRejected() {
        assertThatThrownBy(() -> service.reschedule(TODAY, 5L, 9L, to(TODAY.minusDays(1))))
                .isInstanceOfSatisfying(FieldErrorException.class,
                        ex -> assertThat(ex.getErrors()).containsKey("dueDate"));
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void dateAfterTheEventIsRejected() {
        assertThatThrownBy(() -> service.reschedule(TODAY, 5L, 9L, to(EVENT_DATE.plusDays(1))))
                .isInstanceOfSatisfying(FieldErrorException.class,
                        ex -> assertThat(ex.getErrors().get("dueDate")).contains("posterior al evento"));
    }

    @Test
    void doneTaskCannotBeRescheduled() {
        task.setStatus(TaskStatus.DONE);

        assertThatThrownBy(() -> service.reschedule(TODAY, 5L, 9L, to(TODAY)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void eventOfAnotherUserIsNotFound() {
        when(eventRepository.findDetailByIdAndUserId(77L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reschedule(TODAY, 77L, 9L, to(TODAY)))
                .isInstanceOf(NotFoundException.class);
        verify(taskRepository, never()).sumPlannedHours(anyLong(), any(), anyLong());
    }

    @Test
    void unknownTaskIsNotFound() {
        assertThatThrownBy(() -> service.reschedule(TODAY, 5L, 404L, to(TODAY)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void sameDateAndHoursSkipsTheCapacityCheck() {
        task.setDueDate(TODAY);

        service.reschedule(TODAY, 5L, 9L, to(TODAY));

        verify(taskRepository, never()).sumPlannedHours(anyLong(), any(), anyLong());
        assertThat(task.getStatus()).isEqualTo(TaskStatus.PENDING);
    }
}
