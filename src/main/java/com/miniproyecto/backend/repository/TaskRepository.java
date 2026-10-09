package com.miniproyecto.backend.repository;

import com.miniproyecto.backend.entity.Task;
import com.miniproyecto.backend.entity.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    @Query("""
            SELECT t.id AS id,
                   t.name AS name,
                   t.description AS description,
                   t.status AS status,
                   t.estimatedHours AS estimatedHours,
                   t.dueDate AS date,
                   t.startTime AS startTime,
                   t.endTime AS endTime,
                   e.id AS eventId,
                   e.name AS eventName,
                   e.eventDate AS eventDate,
                   c.name AS clientName
            FROM Task t
            JOIN t.event e
            LEFT JOIN e.client c
            WHERE e.user.id = :userId
              AND (:incluirHechas = true OR t.status <> com.miniproyecto.backend.entity.TaskStatus.DONE)
              AND t.dueDate <= :limit
            ORDER BY t.dueDate ASC, t.estimatedHours ASC, t.id ASC
            """)
    List<TaskTodayView> findTodayTasks(
            @Param("userId") Long userId,
            @Param("limit") LocalDate limit,
            @Param("incluirHechas") boolean incluirHechas);

    /** Horas ya planificadas (no hechas) de un día, sin contar la gestión {@code excludeId}; null si no hay. */
    @Query("""
            SELECT SUM(t.estimatedHours)
            FROM Task t JOIN t.event e
            WHERE e.user.id = :userId
              AND t.dueDate = :date
              AND t.status <> com.miniproyecto.backend.entity.TaskStatus.DONE
              AND t.id <> :excludeId
            """)
    BigDecimal sumPlannedHours(
            @Param("userId") Long userId,
            @Param("date") LocalDate date,
            @Param("excludeId") Long excludeId);

    @Query("""
            SELECT t.dueDate AS date, SUM(t.estimatedHours) AS hours
            FROM Task t JOIN t.event e
            WHERE e.user.id = :userId
              AND t.dueDate BETWEEN :from AND :to
              AND t.status <> com.miniproyecto.backend.entity.TaskStatus.DONE
              AND t.id <> :excludeId
            GROUP BY t.dueDate
            """)
    List<DayLoad> sumPlannedHoursByDate(
            @Param("userId") Long userId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("excludeId") Long excludeId);

    interface DayLoad {
        LocalDate getDate();
        BigDecimal getHours();
    }

    interface TaskTodayView {
        Long getId();
        String getName();
        String getDescription();
        TaskStatus getStatus();
        BigDecimal getEstimatedHours();
        LocalDate getDate();
        LocalTime getStartTime();
        LocalTime getEndTime();
        Long getEventId();
        String getEventName();
        LocalDate getEventDate();
        String getClientName();
    }
}
