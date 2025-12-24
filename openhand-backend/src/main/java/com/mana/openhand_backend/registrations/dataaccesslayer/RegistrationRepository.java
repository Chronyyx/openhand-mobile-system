package com.mana.openhand_backend.registrations.dataaccesslayer;

import com.mana.openhand_backend.events.dataaccesslayer.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface RegistrationRepository extends JpaRepository<Registration, Long> {

    Optional<Registration> findByUserIdAndEventId(Long userId, Long eventId);

    List<Registration> findByUserId(Long userId);

    List<Registration> findByEventId(Long eventId);

    boolean existsByUserIdAndEventId(Long userId, Long eventId);

    long countByEventIdAndStatus(Long eventId, RegistrationStatus status);

    /**
     * Retrieves an Event with a pessimistic write lock to prevent concurrent registrations
     * from violating capacity constraints. The database will lock the row until the transaction
     * completes, ensuring atomic check-and-update operations.
     *
     * @param eventId the ID of the event to lock
     * @return the locked event
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Event e WHERE e.id = :eventId")
    Optional<Event> findEventByIdForUpdate(@Param("eventId") Long eventId);
}

