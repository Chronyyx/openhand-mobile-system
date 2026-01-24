package com.mana.openhand_backend.events.presentationlayer;

import com.mana.openhand_backend.events.businesslayer.EventStaffService;
import com.mana.openhand_backend.events.dataaccesslayer.Event;
import com.mana.openhand_backend.events.dataaccesslayer.EventStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = EventEmployeeController.class)
@AutoConfigureMockMvc(addFilters = false)
class EventEmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EventStaffService eventStaffService;

    @Test
    void getEventsForStaff_returnsList() throws Exception {
        when(eventStaffService.getEventsForStaff()).thenReturn(List.of(buildEvent(1L)));

        mockMvc.perform(get("/api/employee/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].status").value("OPEN"));
    }

    @Test
    void markEventCompleted_returnsUpdatedEvent() throws Exception {
        when(eventStaffService.markEventCompleted(4L)).thenReturn(buildEvent(4L));

        mockMvc.perform(put("/api/employee/events/4/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(4));
    }

    @Test
    void deleteArchivedEvent_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/employee/events/9"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteArchivedEvent_whenInvalid_returnsBadRequest() throws Exception {
        doThrow(new IllegalArgumentException("bad"))
                .when(eventStaffService)
                .deleteArchivedEvent(3L);

        mockMvc.perform(delete("/api/employee/events/3"))
                .andExpect(status().isBadRequest());
    }

    private Event buildEvent(Long id) {
        Event event = new Event(
                "Event",
                "Desc",
                LocalDateTime.of(2025, 1, 1, 9, 0),
                null,
                "Hall",
                "123 Street",
                EventStatus.OPEN,
                10,
                0,
                "CATEGORY"
        );
        ReflectionTestUtils.setField(event, "id", id);
        return event;
    }
}
