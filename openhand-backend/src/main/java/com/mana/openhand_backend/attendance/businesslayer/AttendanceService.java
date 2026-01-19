package com.mana.openhand_backend.attendance.businesslayer;

import com.mana.openhand_backend.attendance.domainclientlayer.AttendanceEventAttendeesResponseModel;
import com.mana.openhand_backend.attendance.domainclientlayer.AttendanceEventSummaryResponseModel;
import com.mana.openhand_backend.attendance.domainclientlayer.AttendanceUpdateResponseModel;

import java.util.List;

public interface AttendanceService {
    List<AttendanceEventSummaryResponseModel> getAttendanceEvents();

    AttendanceEventAttendeesResponseModel getEventAttendees(Long eventId);

    AttendanceUpdateResponseModel checkInAttendee(Long eventId, Long userId);

    AttendanceUpdateResponseModel undoCheckInAttendee(Long eventId, Long userId);
}
