package com.miniproyecto.backend.repository;

import com.miniproyecto.backend.entity.AppUser;
import com.miniproyecto.backend.entity.Event;
import com.miniproyecto.backend.entity.Task;
import com.miniproyecto.backend.entity.TaskStatus;
import com.miniproyecto.backend.repository.TaskRepository.DayLoad;
import com.miniproyecto.backend.repository.TaskRepository.TaskTodayView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Valida contra una base real (H2 en modo PostgreSQL) las consultas JPQL del límite diario y de /api/today. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "DB_URL=jdbc:h2:mem:queries;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "DB_USER=sa",
        "DB_PASSWORD=",
        "JWT_SECRET=pruebas",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class TaskRepositoryQueriesTest {

    private static final LocalDate DAY = LocalDate.of(2026, 10, 10);

    @Autowired
    private TestEntityManager em;

    @Autowired
    private TaskRepository taskRepository;

    private AppUser user;
    private AppUser other;
    private Event event;
    private Task mine;

    @BeforeEach
    void setUp() {
        user = newUser("a@x.co");
        other = newUser("b@x.co");
        event = newEvent(user, "Boda");
        mine = newTask(event, "Reservar salón", DAY, "4.00", TaskStatus.PENDING);
        newTask(event, "Catering", DAY, "2.50", TaskStatus.POSTPONED);
        newTask(event, "Ya hecha", DAY, "3.00", TaskStatus.DONE);
        newTask(event, "Otro día", DAY.plusDays(1), "1.00", TaskStatus.PENDING);
        Event foreign = newEvent(other, "Ajeno");
        newTask(foreign, "De otro usuario", DAY, "5.00", TaskStatus.PENDING);
        em.flush();
        em.clear();
    }

    private AppUser newUser(String email) {
        AppUser u = new AppUser();
        u.setName("Usuario");
        u.setEmail(email);
        u.setPasswordHash("x");
        u.setLastLoginAt(LocalDateTime.now());
        return em.persist(u);
    }

    private Event newEvent(AppUser owner, String name) {
        Event e = new Event();
        e.setUser(owner);
        e.setName(name);
        e.setEventDate(DAY.plusDays(20));
        e.setEventTime(LocalTime.NOON);
        e.setPlace("Salón");
        return em.persist(e);
    }

    private Task newTask(Event e, String name, LocalDate date, String hours, TaskStatus status) {
        Task t = new Task();
        t.setEvent(e);
        t.setName(name);
        t.setDueDate(date);
        t.setEstimatedHours(new BigDecimal(hours));
        t.setStatus(status);
        return em.persist(t);
    }

    @Test
    void sumPlannedHoursCountsOnlyMyNotDoneTasksOfThatDay() {
        BigDecimal total = taskRepository.sumPlannedHours(user.getId(), DAY, -1L);

        assertThat(total).isEqualByComparingTo("6.50");
    }

    @Test
    void sumPlannedHoursExcludesTheTaskBeingMoved() {
        BigDecimal total = taskRepository.sumPlannedHours(user.getId(), DAY, mine.getId());

        assertThat(total).isEqualByComparingTo("2.50");
    }

    @Test
    void sumPlannedHoursIsNullWhenTheDayIsEmpty() {
        assertThat(taskRepository.sumPlannedHours(user.getId(), DAY.plusDays(9), -1L)).isNull();
    }

    @Test
    void sumPlannedHoursByDateGroupsTheRangeAndSkipsDoneAndForeignTasks() {
        List<DayLoad> loads = taskRepository.sumPlannedHoursByDate(user.getId(), DAY, DAY.plusDays(3), -1L);

        assertThat(loads).extracting(DayLoad::getDate).containsExactlyInAnyOrder(DAY, DAY.plusDays(1));
        assertThat(loads.stream().filter(l -> l.getDate().equals(DAY)).findFirst().orElseThrow().getHours())
                .isEqualByComparingTo("6.50");
    }

    @Test
    void findTodayTasksIncludesTheEventDate() {
        List<TaskTodayView> tasks = taskRepository.findTodayTasks(user.getId(), DAY.plusDays(1), false);

        assertThat(tasks).hasSize(3);
        assertThat(tasks).allSatisfy(t -> assertThat(t.getEventDate()).isEqualTo(DAY.plusDays(20)));
        assertThat(tasks).extracting(TaskTodayView::getName).doesNotContain("Ya hecha", "De otro usuario");
    }

    @Test
    void findTodayTasksCanIncludeDoneOnes() {
        assertThat(taskRepository.findTodayTasks(user.getId(), DAY.plusDays(1), true)).hasSize(4);
    }
}
