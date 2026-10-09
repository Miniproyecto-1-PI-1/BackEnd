package com.miniproyecto.backend.service;

import com.miniproyecto.backend.dto.TodayCategory;
import com.miniproyecto.backend.dto.TodayResponse;
import com.miniproyecto.backend.entity.TaskStatus;
import com.miniproyecto.backend.repository.TaskRepository;
import com.miniproyecto.backend.repository.TaskRepository.TaskTodayView;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TodayServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 1);

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private TodayService todayService;

    @BeforeEach
    void setUp() {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "HS256").subject("1").build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private record FakeView(
            Long id,
            String name,
            String description,
            TaskStatus status,
            BigDecimal estimatedHours,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime
    ) implements TaskTodayView {

        @Override
        public Long getId() {
            return id;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getDescription() {
            return description;
        }

        @Override
        public TaskStatus getStatus() {
            return status;
        }

        @Override
        public BigDecimal getEstimatedHours() {
            return estimatedHours;
        }

        @Override
        public LocalDate getDate() {
            return date;
        }

        @Override
        public LocalTime getStartTime() {
            return startTime;
        }

        @Override
        public LocalTime getEndTime() {
            return endTime;
        }

        @Override
        public Long getEventId() {
            return 100L;
        }

        @Override
        public String getEventName() {
            return "Boda";
        }

        @Override
        public LocalDate getEventDate() {
            return LocalDate.of(2026, 12, 5);
        }

        @Override
        public String getClientName() {
            return "Ana";
        }
    }

    private TaskTodayView view(long id, LocalDate date, TaskStatus status, String hours) {
        return new FakeView(id, "Gestión " + id, null, status, new BigDecimal(hours), date, null, null);
    }

    @Test
    void classifiesOverdueTodayAndUpcomingKeepingRepositoryOrder() {
        when(taskRepository.findTodayTasks(1L, TODAY.plusDays(7), false)).thenReturn(List.of(
                view(1, TODAY.minusDays(3), TaskStatus.PENDING, "2.00"),
                view(2, TODAY, TaskStatus.PENDING, "0.50"),
                view(3, TODAY, TaskStatus.POSTPONED, "3.00"),
                view(4, TODAY.plusDays(2), TaskStatus.PENDING, "1.00")
        ));

        TodayResponse response = todayService.build(TODAY, null, false);

        assertThat(response.today()).isEqualTo(TODAY);
        assertThat(response.overdueCount()).isEqualTo(1);
        assertThat(response.todayCount()).isEqualTo(2);
        assertThat(response.upcomingCount()).isEqualTo(1);
        assertThat(response.tasks()).extracting("id").containsExactly(1L, 2L, 3L, 4L);
        assertThat(response.tasks()).extracting("category").containsExactly(
                TodayCategory.OVERDUE, TodayCategory.TODAY, TodayCategory.TODAY, TodayCategory.UPCOMING);
        assertThat(response.tasks().get(0).daysFromToday()).isEqualTo(-3);
        assertThat(response.tasks().get(3).daysFromToday()).isEqualTo(2);
    }

    @Test
    void usesSevenDaysWindowByDefault() {
        when(taskRepository.findTodayTasks(1L, TODAY.plusDays(7), false)).thenReturn(List.of());

        todayService.build(TODAY, null, false);

        verify(taskRepository).findTodayTasks(1L, TODAY.plusDays(7), false);
    }

    @Test
    void clampsWindowBetweenZeroAndMax() {
        when(taskRepository.findTodayTasks(1L, TODAY, false)).thenReturn(List.of());
        when(taskRepository.findTodayTasks(1L, TODAY.plusDays(60), false)).thenReturn(List.of());

        todayService.build(TODAY, -5, false);
        todayService.build(TODAY, 999, false);

        verify(taskRepository).findTodayTasks(1L, TODAY, false);
        verify(taskRepository).findTodayTasks(1L, TODAY.plusDays(60), false);
    }

    @Test
    void returnsEmptyResponseWhenNothingIsPending() {
        when(taskRepository.findTodayTasks(1L, TODAY.plusDays(7), false)).thenReturn(List.of());

        TodayResponse response = todayService.build(TODAY, null, false);

        assertThat(response.tasks()).isEmpty();
        assertThat(response.overdueCount()).isZero();
        assertThat(response.todayCount()).isZero();
        assertThat(response.upcomingCount()).isZero();
    }

    @Test
    void passesThroughIncluirHechasToRepository() {
        when(taskRepository.findTodayTasks(1L, TODAY.plusDays(7), true)).thenReturn(List.of(
                view(1, TODAY, TaskStatus.DONE, "1.00")
        ));

        TodayResponse response = todayService.getToday(null, true);

        verify(taskRepository).findTodayTasks(1L, TODAY.plusDays(7), true);
        assertThat(response.tasks()).hasSize(1);
        assertThat(response.tasks().get(0).status()).isEqualTo(TaskStatus.DONE);
    }

    @Test
    void mapsDescriptionAndTimeRangeWhenPresent() {
        FakeView withDetails = new FakeView(
                5L, "Reservar salón", "Confirmar aforo", TaskStatus.PENDING,
                new BigDecimal("2.00"), TODAY, LocalTime.of(9, 0), LocalTime.of(11, 0));
        when(taskRepository.findTodayTasks(1L, TODAY.plusDays(7), false)).thenReturn(List.of(withDetails));

        TodayResponse response = todayService.build(TODAY, null, false);

        var task = response.tasks().get(0);
        assertThat(task.description()).isEqualTo("Confirmar aforo");
        assertThat(task.startTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(task.endTime()).isEqualTo(LocalTime.of(11, 0));
    }

    @Test
    void includesTheEventDateSoTheClientCanBoundReprogramming() {
        when(taskRepository.findTodayTasks(1L, TODAY.plusDays(7), false))
                .thenReturn(List.of(view(1, TODAY, TaskStatus.PENDING, "1.00")));

        TodayResponse response = todayService.build(TODAY, null, false);

        assertThat(response.tasks().get(0).eventDate()).isEqualTo(LocalDate.of(2026, 12, 5));
    }
}
