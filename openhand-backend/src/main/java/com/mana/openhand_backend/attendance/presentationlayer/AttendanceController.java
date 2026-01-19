package com.mana.openhand_backend.attendance.presentationlayer;

import com.mana.openhand_backend.attendance.businesslayer.AttendanceService;
import com.mana.openhand_backend.attendance.domainclientlayer.AttendanceEventAttendeesResponseModel;
import com.mana.openhand_backend.attendance.domainclientlayer.AttendanceEventSummaryResponseModel;
import com.mana.openhand_backend.attendance.domainclientlayer.AttendanceUpdateResponseModel;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/employee/attendance")
@PreAuthorize("hasRole('ROLE_EMPLOYEE') or hasRole('ROLE_ADMIN')")
public class AttendanceController {

    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @GetMapping("/events")
    public List<AttendanceEventSummaryResponseModel> getAttendanceEvents() {
        return attendanceService.getAttendanceEvents();
    }

    @GetMapping("/events/{eventId}/attendees")
    public AttendanceEventAttendeesResponseModel getEventAttendees(@PathVariable Long eventId) {
        return attendanceService.getEventAttendees(eventId);
    }

    @PutMapping("/events/{eventId}/attendees/{userId}/check-in")
    public AttendanceUpdateResponseModel checkInAttendee(@PathVariable Long eventId, @PathVariable Long userId) {
        return attendanceService.checkInAttendee(eventId, userId);
    }

    @DeleteMapping("/events/{eventId}/attendees/{userId}/check-in")
    public AttendanceUpdateResponseModel undoCheckInAttendee(@PathVariable Long eventId, @PathVariable Long userId) {
        return attendanceService.undoCheckInAttendee(eventId, userId);
    }
}
