package com.miniproyecto.backend.service;

import com.miniproyecto.backend.entity.AppUser;
import com.miniproyecto.backend.exception.OverloadConflictException;
import com.miniproyecto.backend.repository.AppUserRepository;
import com.miniproyecto.backend.repository.TaskRepository;
import com.miniproyecto.backend.repository.TaskRepository.DayLoad;
import com.miniproyecto.backend.service.DailyCapacityService.PlannedTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DailyCapacityServiceTest {

    private static final long USER = 1L;
    private static final long TASK = 9L;
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 8);
    private static final LocalDate EVENT_DATE = LocalDate.of(2026, 10, 30);

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private AppUserRepository appUserRepository;

    private DailyCapacityService service;

    @BeforeEach
    void setUp() {
        service = new DailyCapacityService(taskRepository, appUserRepository);
        AppUser user = new AppUser();
        user.setId(USER);
        user.setDailyHourLimit(6);
        when(appUserRepository.findById(USER)).thenReturn(Optional.of(user));
    }

    private static DayLoad load(LocalDate date, String hours) {
        return new DayLoad() {
            public LocalDate getDate() { return date; }
            public BigDecimal getHours() { return new BigDecimal(hours); }
        };
    }

    private void ensure(LocalDate date, String hours, Long excludeTaskId) {
        service.ensureFits(USER, TODAY, date, new BigDecimal(hours), excludeTaskId, EVENT_DATE);
    }

    @Test
    void emptyDayAlwaysFitsWithinTheLimit() {
        when(taskRepository.sumPlannedHours(USER, TODAY, TASK)).thenReturn(null);

        assertThatCode(() -> ensure(TODAY, "6", TASK)).doesNotThrowAnyException();
    }

    @Test
    void landingExactlyOnTheLimitIsAllowed() {
        when(taskRepository.sumPlannedHours(USER, TODAY, TASK)).thenReturn(new BigDecimal("3.00"));

        assertThatCode(() -> ensure(TODAY, "3", TASK)).doesNotThrowAnyException();
    }

    @Test
    void overloadedDayRaisesConflictWithFiguresAndSuggestions() {
        when(taskRepository.sumPlannedHours(USER, TODAY, TASK)).thenReturn(new BigDecimal("4.00"));
        when(taskRepository.sumPlannedHoursByDate(eq(USER), eq(TODAY), eq(EVENT_DATE), eq(TASK)))
                .thenReturn(List.of(load(TODAY, "4.00"), load(TODAY.plusDays(1), "5.00")));

        assertThatThrownBy(() -> ensure(TODAY, "3", TASK))
                .isInstanceOfSatisfying(OverloadConflictException.class, ex -> {
                    assertThat(ex.getMessage()).isEqualTo("Quedarías con 7h planificadas ese día (tu límite es 6h).");
                    assertThat(ex.getDate()).isEqualTo(TODAY);
                    assertThat(ex.getPlannedHours()).isEqualByComparingTo("4");
                    assertThat(ex.getTaskHours()).isEqualByComparingTo("3");
                    assertThat(ex.getResultingHours()).isEqualByComparingTo("7");
                    assertThat(ex.getLimitHours()).isEqualTo(6);
                    assertThat(ex.getExceedsBy()).isEqualByComparingTo("1");
                    assertThat(ex.getAvailableHours()).isEqualByComparingTo("2");
                    assertThat(ex.getSuggestedDates()).containsExactly(
                            TODAY.plusDays(2), TODAY.plusDays(3), TODAY.plusDays(4));
                });
    }

    @Test
    void newTaskIsCheckedWithoutExcludingAnyTask() {
        when(taskRepository.sumPlannedHours(USER, TODAY, -1L)).thenReturn(new BigDecimal("5.00"));
        when(taskRepository.sumPlannedHoursByDate(eq(USER), eq(TODAY), eq(EVENT_DATE), eq(-1L))).thenReturn(List.of());

        assertThatThrownBy(() -> ensure(TODAY, "2", null)).isInstanceOf(OverloadConflictException.class);
    }

    @Test
    void taskLargerThanTheLimitGetsNoSuggestions() {
        when(taskRepository.sumPlannedHours(USER, TODAY, TASK)).thenReturn(null);
        when(taskRepository.sumPlannedHoursByDate(eq(USER), eq(TODAY), eq(EVENT_DATE), eq(TASK))).thenReturn(List.of());

        assertThatThrownBy(() -> ensure(TODAY, "8", TASK))
                .isInstanceOfSatisfying(OverloadConflictException.class,
                        ex -> assertThat(ex.getSuggestedDates()).isEmpty());
    }

    @Test
    void newEventSumsTasksOfTheSameDayAmongThemselves() {
        when(taskRepository.sumPlannedHours(USER, TODAY, -1L)).thenReturn(new BigDecimal("1.00"));

        assertThatThrownBy(() -> service.ensureNewEventFits(USER, List.of(
                new PlannedTask(TODAY, new BigDecimal("3.00")),
                new PlannedTask(TODAY, new BigDecimal("3.00")))))
                .isInstanceOfSatisfying(OverloadConflictException.class, ex -> {
                    assertThat(ex.getPlannedHours()).isEqualByComparingTo("1");
                    assertThat(ex.getTaskHours()).isEqualByComparingTo("6");
                    assertThat(ex.getResultingHours()).isEqualByComparingTo("7");
                    assertThat(ex.getSuggestedDates()).isEmpty();
                });
    }

    @Test
    void newEventWithTasksOnDifferentDaysWithinTheLimitPasses() {
        when(taskRepository.sumPlannedHours(USER, TODAY, -1L)).thenReturn(null);
        when(taskRepository.sumPlannedHours(USER, TODAY.plusDays(1), -1L)).thenReturn(null);

        assertThatCode(() -> service.ensureNewEventFits(USER, List.of(
                new PlannedTask(TODAY, new BigDecimal("6.00")),
                new PlannedTask(TODAY.plusDays(1), new BigDecimal("6.00")))))
                .doesNotThrowAnyException();
    }
}
