package com.mana.openhand_backend.events.presentationlayer;

import com.mana.openhand_backend.events.businesslayer.EventService;
import com.mana.openhand_backend.events.domainclientlayer.RegistrationSummaryResponseModel;
import com.mana.openhand_backend.registrations.domainclientlayer.AttendeeResponseModel;
import com.mana.openhand_backend.events.utils.EventNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventControllerMemberStatusUnitTest {

    @Mock
    private EventService eventService;

    @InjectMocks
    private EventController eventController;

    @Test
    void getRegistrationSummary_callsServiceWithEventId() throws EventNotFoundException {
        // arrange
        long eventId = 1L;
        RegistrationSummaryResponseModel mockResponse = createMockRegistrationSummary(eventId, 0);
        when(eventService.getRegistrationSummary(eventId)).thenReturn(mockResponse);

        // act
        RegistrationSummaryResponseModel result = eventController.getRegistrationSummary(eventId);

        // assert
        assertNotNull(result);
        verify(eventService, times(1)).getRegistrationSummary(eventId);
    }

    @Test
    void getRegistrationSummary_returnsMemberStatusInResponse() throws EventNotFoundException {
        // arrange
        long eventId = 1L;
        List<AttendeeResponseModel> attendees = new ArrayList<>();
        AttendeeResponseModel attendee = new AttendeeResponseModel();
        attendee.setMemberStatus("ACTIVE");
        attendees.add(attendee);
        
        RegistrationSummaryResponseModel mockResponse = new RegistrationSummaryResponseModel(
                eventId, 1, 0, 10, 9, 10.0, attendees);
        when(eventService.getRegistrationSummary(eventId)).thenReturn(mockResponse);

        // act
        RegistrationSummaryResponseModel result = eventController.getRegistrationSummary(eventId);

        // assert
        assertEquals(1, result.getAttendees().size());
        assertEquals("ACTIVE", result.getAttendees().get(0).getMemberStatus());
    }

    @Test
    void getRegistrationSummary_includesAttendeeList() throws EventNotFoundException {
        // arrange
        long eventId = 2L;
        List<AttendeeResponseModel> attendees = new ArrayList<>();
        AttendeeResponseModel attendee1 = new AttendeeResponseModel();
        attendee1.setUserName("John Doe");
        AttendeeResponseModel attendee2 = new AttendeeResponseModel();
        attendee2.setUserName("Jane Smith");
        attendees.add(attendee1);
        attendees.add(attendee2);
        
        RegistrationSummaryResponseModel mockResponse = new RegistrationSummaryResponseModel(
                eventId, 2, 0, 10, 8, 20.0, attendees);
        when(eventService.getRegistrationSummary(eventId)).thenReturn(mockResponse);

        // act
        RegistrationSummaryResponseModel result = eventController.getRegistrationSummary(eventId);

        // assert
        assertEquals(2, result.getAttendees().size());
        assertEquals("John Doe", result.getAttendees().get(0).getUserName());
        assertEquals("Jane Smith", result.getAttendees().get(1).getUserName());
    }

    @Test
    void getRegistrationSummary_includesRegistrationMetrics() throws EventNotFoundException {
        // arrange
        long eventId = 3L;
        RegistrationSummaryResponseModel mockResponse = new RegistrationSummaryResponseModel(
                eventId, 5, 2, 10, 3, 70.0, new ArrayList<>());
        when(eventService.getRegistrationSummary(eventId)).thenReturn(mockResponse);

        // act
        RegistrationSummaryResponseModel result = eventController.getRegistrationSummary(eventId);

        // assert
        assertEquals(5, result.getTotalRegistrations());
        assertEquals(2, result.getWaitlistedCount());
        assertEquals(10, result.getMaxCapacity());
        assertEquals(3, result.getRemainingSpots());
        assertEquals(70.0, result.getPercentageFull());
    }

    @Test
    void getRegistrationSummary_handlesEmptyAttendeeList() throws EventNotFoundException {
        // arrange
        long eventId = 4L;
        RegistrationSummaryResponseModel mockResponse = new RegistrationSummaryResponseModel(
                eventId, 0, 0, 10, 10, 0.0, new ArrayList<>());
        when(eventService.getRegistrationSummary(eventId)).thenReturn(mockResponse);

        // act
        RegistrationSummaryResponseModel result = eventController.getRegistrationSummary(eventId);

        // assert
        assertEquals(0, result.getAttendees().size());
        assertEquals(0, result.getTotalRegistrations());
    }

    @Test
    void getRegistrationSummary_displaysActiveAndInactiveMembers() throws EventNotFoundException {
        // arrange
        long eventId = 5L;
        List<AttendeeResponseModel> attendees = new ArrayList<>();
        
        AttendeeResponseModel activeAttendee = new AttendeeResponseModel();
        activeAttendee.setUserName("Active Member");
        activeAttendee.setMemberStatus("ACTIVE");
        
        AttendeeResponseModel inactiveAttendee = new AttendeeResponseModel();
        inactiveAttendee.setUserName("Inactive Member");
        inactiveAttendee.setMemberStatus("INACTIVE");
        
        attendees.add(activeAttendee);
        attendees.add(inactiveAttendee);
        
        RegistrationSummaryResponseModel mockResponse = new RegistrationSummaryResponseModel(
                eventId, 2, 0, 10, 8, 20.0, attendees);
        when(eventService.getRegistrationSummary(eventId)).thenReturn(mockResponse);

        // act
        RegistrationSummaryResponseModel result = eventController.getRegistrationSummary(eventId);

        // assert
        assertEquals(2, result.getAttendees().size());
        assertEquals("ACTIVE", result.getAttendees().get(0).getMemberStatus());
        assertEquals("INACTIVE", result.getAttendees().get(1).getMemberStatus());
    }

    @Test
    void getRegistrationSummary_includesUserDetails() throws EventNotFoundException {
        // arrange
        long eventId = 6L;
        List<AttendeeResponseModel> attendees = new ArrayList<>();
        AttendeeResponseModel attendee = new AttendeeResponseModel();
        attendee.setUserName("Test User");
        attendee.setUserEmail("test@example.com");
        attendees.add(attendee);
        
        RegistrationSummaryResponseModel mockResponse = new RegistrationSummaryResponseModel(
                eventId, 1, 0, 10, 9, 10.0, attendees);
        when(eventService.getRegistrationSummary(eventId)).thenReturn(mockResponse);

        // act
        RegistrationSummaryResponseModel result = eventController.getRegistrationSummary(eventId);

        // assert
        assertEquals("Test User", result.getAttendees().get(0).getUserName());
        assertEquals("test@example.com", result.getAttendees().get(0).getUserEmail());
    }

    @Test
    void getRegistrationSummary_includesWaitlistedStatus() throws EventNotFoundException {
        // arrange
        long eventId = 7L;
        List<AttendeeResponseModel> attendees = new ArrayList<>();
        AttendeeResponseModel attendee = new AttendeeResponseModel();
        attendee.setUserName("Waitlisted User");
        attendee.setRegistrationStatus("WAITLISTED");
        attendee.setWaitlistedPosition(1);
        attendees.add(attendee);
        
        RegistrationSummaryResponseModel mockResponse = new RegistrationSummaryResponseModel(
                eventId, 1, 1, 10, 9, 10.0, attendees);
        when(eventService.getRegistrationSummary(eventId)).thenReturn(mockResponse);

        // act
        RegistrationSummaryResponseModel result = eventController.getRegistrationSummary(eventId);

        // assert
        assertEquals("WAITLISTED", result.getAttendees().get(0).getRegistrationStatus());
        assertEquals(1, result.getWaitlistedCount());
    }

    @Test
    void getRegistrationSummary_preservesAttendeeOrder() throws EventNotFoundException {
        // arrange
        long eventId = 8L;
        List<AttendeeResponseModel> attendees = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            AttendeeResponseModel attendee = new AttendeeResponseModel();
            attendee.setUserName("User " + i);
            attendees.add(attendee);
        }
        
        RegistrationSummaryResponseModel mockResponse = new RegistrationSummaryResponseModel(
                eventId, 3, 0, 10, 7, 30.0, attendees);
        when(eventService.getRegistrationSummary(eventId)).thenReturn(mockResponse);

        // act
        RegistrationSummaryResponseModel result = eventController.getRegistrationSummary(eventId);

        // assert
        assertEquals(3, result.getAttendees().size());
        assertEquals("User 1", result.getAttendees().get(0).getUserName());
        assertEquals("User 2", result.getAttendees().get(1).getUserName());
        assertEquals("User 3", result.getAttendees().get(2).getUserName());
    }

    @Test
    void getRegistrationSummary_returnsModificationOfServiceOutput() throws EventNotFoundException {
        // arrange
        long eventId = 9L;
        RegistrationSummaryResponseModel mockResponse = createMockRegistrationSummary(eventId, 5);
        when(eventService.getRegistrationSummary(anyLong())).thenReturn(mockResponse);

        // act
        RegistrationSummaryResponseModel result = eventController.getRegistrationSummary(eventId);

        // assert
        assertSame(mockResponse, result);
        verify(eventService).getRegistrationSummary(eventId);
    }

    private RegistrationSummaryResponseModel createMockRegistrationSummary(long eventId, int attendeeCount) {
        List<AttendeeResponseModel> attendees = new ArrayList<>();
        for (int i = 0; i < attendeeCount; i++) {
            AttendeeResponseModel attendee = new AttendeeResponseModel();
            attendee.setUserName("Attendee " + (i + 1));
            attendee.setMemberStatus("ACTIVE");
            attendees.add(attendee);
        }
        return new RegistrationSummaryResponseModel(
                eventId,
                attendeeCount,
                0,
                10,
                10 - attendeeCount,
                (attendeeCount * 100.0) / 10,
                attendees
        );
    }
}
