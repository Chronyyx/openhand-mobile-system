package com.mana.openhand_backend.events.dataaccesslayer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findByStartDateTimeAfterOrderByStartDateTimeAsc(LocalDateTime dateTime);
}
