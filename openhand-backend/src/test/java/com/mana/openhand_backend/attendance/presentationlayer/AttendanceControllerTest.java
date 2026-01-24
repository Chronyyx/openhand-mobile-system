package com.mana.openhand_backend.attendance.presentationlayer;

import com.mana.openhand_backend.attendance.businesslayer.AttendanceService;
import com.mana.openhand_backend.attendance.domainclientlayer.AttendanceAttendeeResponseModel;
import com.mana.openhand_backend.attendance.domainclientlayer.AttendanceEventAttendeesResponseModel;
import com.mana.openhand_backend.attendance.domainclientlayer.AttendanceEventSummaryResponseModel;
import com.mana.openhand_backend.attendance.domainclientlayer.AttendanceUpdateResponseModel;
import com.mana.openhand_backend.attendance.utils.AttendanceCheckInNotAllowedException;
import com.mana.openhand_backend.attendance.utils.AttendanceRegistrationNotFoundException;
import com.mana.openhand_backend.events.utils.EventNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AttendanceController.class)
@AutoConfigureMockMvc(addFilters = false)
class AttendanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AttendanceService attendanceService;

    @Test
    void getAttendanceEvents_returnsList() throws Exception {
        List<AttendanceEventSummaryResponseModel> events = List.of(
                new AttendanceEventSummaryResponseModel(
                        1L,
                        "Event",
                        "2025-01-01T09:00",
                        null,
                        "Hall",
                        "123 Street",
                        "OPEN",
                        10,
                        5,
                        2,
                        20.0
                )
        );

        when(attendanceService.getAttendanceEvents()).thenReturn(events);

        mockMvc.perform(get("/api/employee/attendance/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventId").value(1))
                .andExpect(jsonPath("$[0].title").value("Event"))
                .andExpect(jsonPath("$[0].checkedInCount").value(2));
    }

    @Test
    void getEventAttendees_returnsPayload() throws Exception {
        AttendanceEventAttendeesResponseModel response = new AttendanceEventAttendeesResponseModel(
                4L,
                2,
                1,
                List.of(new AttendanceAttendeeResponseModel(9L, "A", "a@test.com", "CONFIRMED", true, "2025-01-01T09:10"))
        );

        when(attendanceService.getEventAttendees(4L)).thenReturn(response);

        mockMvc.perform(get("/api/employee/attendance/events/4/attendees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value(4))
                .andExpect(jsonPath("$.checkedInCount").value(1))
                .andExpect(jsonPath("$.attendees[0].userId").value(9));
    }

    @Test
    void getEventAttendees_whenEventMissing_returnsNotFound() throws Exception {
        when(attendanceService.getEventAttendees(anyLong())).thenThrow(new EventNotFoundException(99L));

        mockMvc.perform(get("/api/employee/attendance/events/99/attendees"))
                .andExpect(status().isNotFound());
    }

    @Test
    void checkInAttendee_returnsUpdate() throws Exception {
        AttendanceUpdateResponseModel update = new AttendanceUpdateResponseModel(
                2L, 7L, true, "2025-01-01T10:00", 3, 1, 33.3
        );

        when(attendanceService.checkInAttendee(2L, 7L)).thenReturn(update);

        mockMvc.perform(put("/api/employee/attendance/events/2/attendees/7/check-in"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value(2))
                .andExpect(jsonPath("$.checkedIn").value(true));
    }

    @Test
    void checkInAttendee_whenNotAllowed_returnsBadRequest() throws Exception {
        when(attendanceService.checkInAttendee(anyLong(), anyLong()))
                .thenThrow(new AttendanceCheckInNotAllowedException(1L, 2L));

        mockMvc.perform(put("/api/employee/attendance/events/1/attendees/2/check-in"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void undoCheckInAttendee_returnsUpdate() throws Exception {
        AttendanceUpdateResponseModel update = new AttendanceUpdateResponseModel(
                2L, 7L, false, null, 3, 0, 0.0
        );

        when(attendanceService.undoCheckInAttendee(2L, 7L)).thenReturn(update);

        mockMvc.perform(delete("/api/employee/attendance/events/2/attendees/7/check-in"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checkedIn").value(false));
    }

    @Test
    void undoCheckInAttendee_whenMissing_returnsNotFound() throws Exception {
        when(attendanceService.undoCheckInAttendee(anyLong(), anyLong()))
                .thenThrow(new AttendanceRegistrationNotFoundException(1L, 2L));

        mockMvc.perform(delete("/api/employee/attendance/events/1/attendees/2/check-in"))
                .andExpect(status().isNotFound());
    }
}
