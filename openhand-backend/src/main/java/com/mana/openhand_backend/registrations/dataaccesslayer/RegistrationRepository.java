package com.mana.openhand_backend.registrations.dataaccesslayer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RegistrationRepository extends JpaRepository<Registration, Long> {

    Optional<Registration> findByUserIdAndEventId(Long userId, Long eventId);

    List<Registration> findByUserId(Long userId);

    List<Registration> findByEventId(Long eventId);

    boolean existsByUserIdAndEventId(Long userId, Long eventId);

    long countByEventIdAndStatus(Long eventId, RegistrationStatus status);
}
