package com.miniproyecto.backend.repository;

import com.miniproyecto.backend.entity.Task;
import com.miniproyecto.backend.entity.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    @Query("""
            SELECT t.id AS id,
                   t.name AS name,
                   t.status AS status,
                   t.estimatedHours AS estimatedHours,
                   t.dueDate AS date,
                   e.id AS eventId,
                   e.name AS eventName,
                   c.name AS clientName
            FROM Task t
            JOIN t.event e
            LEFT JOIN e.client c
            WHERE e.user.id = :userId
              AND t.status <> com.miniproyecto.backend.entity.TaskStatus.DONE
              AND t.dueDate <= :limit
            ORDER BY t.dueDate ASC, t.estimatedHours ASC, t.id ASC
            """)
    List<TaskTodayView> findTodayTasks(@Param("userId") Long userId, @Param("limit") LocalDate limit);

    interface TaskTodayView {
        Long getId();
        String getName();
        TaskStatus getStatus();
        BigDecimal getEstimatedHours();
        LocalDate getDate();
        Long getEventId();
        String getEventName();
        String getClientName();
    }
}