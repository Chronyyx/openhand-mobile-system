package com.mana.openhand_backend.events.domainclientlayer;

public class EventAttendeeResponseModel {
    private Long attendeeId;
    private String fullName;
    private Integer age;

    public EventAttendeeResponseModel() {
    }

    public EventAttendeeResponseModel(Long attendeeId, String fullName, Integer age) {
        this.attendeeId = attendeeId;
        this.fullName = fullName;
        this.age = age;
    }

    public Long getAttendeeId() {
        return attendeeId;
    }

    public void setAttendeeId(Long attendeeId) {
        this.attendeeId = attendeeId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }
}
