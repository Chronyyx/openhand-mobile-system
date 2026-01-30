package com.mana.openhand_backend.registrations.dataaccesslayer;

import com.mana.openhand_backend.events.dataaccesslayer.Event;
import com.mana.openhand_backend.identity.dataaccesslayer.User;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "registrations",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "event_id"}))
public class Registration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RegistrationStatus status;

    @Column(nullable = false)
    private LocalDateTime requestedAt;

    private LocalDateTime confirmedAt;

    private LocalDateTime cancelledAt;

    private LocalDateTime checkedInAt;

    private Integer waitlistedPosition;

    @Column(name = "registration_group_id")
    private String registrationGroupId;

    @Column(name = "primary_user_id")
    private Long primaryUserId;

    @Column(name = "is_primary_registrant")
    private Boolean primaryRegistrant;

    @Column(name = "participant_full_name")
    private String participantFullName;

    @Column(name = "participant_age")
    private Integer participantAge;

    @Column(name = "participant_date_of_birth")
    private LocalDate participantDateOfBirth;

    @Column(name = "participant_relation")
    private String participantRelation;

    protected Registration() {
    }

    public Registration(User user, Event event, RegistrationStatus status, LocalDateTime requestedAt) {
        this.user = user;
        this.event = event;
        this.status = status;
        this.requestedAt = requestedAt;
    }

    // Constructor for creating new registrations
    public Registration(User user, Event event) {
        this.user = user;
        this.event = event;
        this.requestedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public RegistrationStatus getStatus() {
        return status;
    }

    public void setStatus(RegistrationStatus status) {
        this.status = status;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(LocalDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }

    public LocalDateTime getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(LocalDateTime confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public LocalDateTime getCheckedInAt() {
        return checkedInAt;
    }

    public void setCheckedInAt(LocalDateTime checkedInAt) {
        this.checkedInAt = checkedInAt;
    }

    public Integer getWaitlistedPosition() {
        return waitlistedPosition;
    }

    public void setWaitlistedPosition(Integer waitlistedPosition) {
        this.waitlistedPosition = waitlistedPosition;
    }

    public String getRegistrationGroupId() {
        return registrationGroupId;
    }

    public void setRegistrationGroupId(String registrationGroupId) {
        this.registrationGroupId = registrationGroupId;
    }

    public Long getPrimaryUserId() {
        return primaryUserId;
    }

    public void setPrimaryUserId(Long primaryUserId) {
        this.primaryUserId = primaryUserId;
    }

    public Boolean getPrimaryRegistrant() {
        return primaryRegistrant;
    }

    public void setPrimaryRegistrant(Boolean primaryRegistrant) {
        this.primaryRegistrant = primaryRegistrant;
    }

    public String getParticipantFullName() {
        return participantFullName;
    }

    public void setParticipantFullName(String participantFullName) {
        this.participantFullName = participantFullName;
    }

    public Integer getParticipantAge() {
        return participantAge;
    }

    public void setParticipantAge(Integer participantAge) {
        this.participantAge = participantAge;
    }

    public LocalDate getParticipantDateOfBirth() {
        return participantDateOfBirth;
    }

    public void setParticipantDateOfBirth(LocalDate participantDateOfBirth) {
        this.participantDateOfBirth = participantDateOfBirth;
    }

    public String getParticipantRelation() {
        return participantRelation;
    }

    public void setParticipantRelation(String participantRelation) {
        this.participantRelation = participantRelation;
    }
}
