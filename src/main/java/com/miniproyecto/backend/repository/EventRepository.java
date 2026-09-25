package com.miniproyecto.backend.repository;

import com.miniproyecto.backend.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    Optional<Event> findByIdAndUser_Id(Long id, Long userId);

    @Query("""
            SELECT DISTINCT e
            FROM Event e
            LEFT JOIN FETCH e.client
            LEFT JOIN FETCH e.tasks
            WHERE e.id = :id AND e.user.id = :userId
            """)
    Optional<Event> findDetailByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    @Query("""
            SELECT e.id AS id,
                   e.name AS name,
                   e.type AS type,
                   e.eventDate AS date,
                   c.name AS clientName,
                   COUNT(t.id) AS totalTasks,
                   COALESCE(SUM(CASE WHEN t.status = com.miniproyecto.backend.entity.TaskStatus.DONE THEN 1 ELSE 0 END), 0) AS doneTasks
            FROM Event e
            LEFT JOIN e.client c
            LEFT JOIN e.tasks t
            WHERE e.user.id = :userId
              AND (:q IS NULL OR :q = '' OR LOWER(e.name) LIKE LOWER(CONCAT('%', :q, '%')))
            GROUP BY e.id, e.name, e.type, e.eventDate, c.name, e.createdAt
            ORDER BY e.createdAt DESC
            """)
    List<EventSummaryView> findSummariesByUserId(@Param("userId") Long userId, @Param("q") String q);

    interface EventSummaryView {
        Long getId();

        String getName();

        String getType();

        LocalDate getDate();

        String getClientName();

        Long getTotalTasks();

        Long getDoneTasks();
    }
}
