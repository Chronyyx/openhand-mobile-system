package com.mana.openhand_backend.registrations.domainclientlayer;

import java.util.ArrayList;
import java.util.List;

public class GroupRegistrationResponseModel {
    private Long eventId;
    private ParticipantResponseModel primaryRegistrant;
    private List<ParticipantResponseModel> participants = new ArrayList<>();
    private Integer remainingCapacity;

    public GroupRegistrationResponseModel() {
    }

    public GroupRegistrationResponseModel(Long eventId, ParticipantResponseModel primaryRegistrant,
                                          List<ParticipantResponseModel> participants, Integer remainingCapacity) {
        this.eventId = eventId;
        this.primaryRegistrant = primaryRegistrant;
        this.participants = participants != null ? participants : new ArrayList<>();
        this.remainingCapacity = remainingCapacity;
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public ParticipantResponseModel getPrimaryRegistrant() {
        return primaryRegistrant;
    }

    public void setPrimaryRegistrant(ParticipantResponseModel primaryRegistrant) {
        this.primaryRegistrant = primaryRegistrant;
    }

    public List<ParticipantResponseModel> getParticipants() {
        return participants;
    }

    public void setParticipants(List<ParticipantResponseModel> participants) {
        this.participants = participants != null ? participants : new ArrayList<>();
    }

    public Integer getRemainingCapacity() {
        return remainingCapacity;
    }

    public void setRemainingCapacity(Integer remainingCapacity) {
        this.remainingCapacity = remainingCapacity;
    }
}
