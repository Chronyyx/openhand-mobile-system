package com.mana.openhand_backend.events.dataaccesslayer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findByStartDateTimeAfterOrderByStartDateTimeAsc(LocalDateTime dateTime);

    List<Event> findByStartDateTimeBetweenOrderByStartDateTimeAsc(LocalDateTime startDateTime, LocalDateTime endDateTime);

    List<Event> findByStartDateTimeGreaterThanEqualOrderByStartDateTimeAsc(LocalDateTime dateTime);

    List<Event> findByStartDateTimeGreaterThanEqualAndStatusNotOrderByStartDateTimeAsc(
            LocalDateTime dateTime,
            EventStatus status
    );

    List<Event> findByStatusNotOrderByStartDateTimeAsc(EventStatus status);

    List<Event> findByEndDateTimeNotNullAndEndDateTimeLessThanEqualAndStatusNot(
            LocalDateTime dateTime,
            EventStatus status
    );
}
