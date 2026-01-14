package com.mana.openhand_backend.events.businesslayer;

import com.mana.openhand_backend.events.dataaccesslayer.Event;
import com.mana.openhand_backend.events.dataaccesslayer.EventRepository;
import com.mana.openhand_backend.events.dataaccesslayer.EventStatus;
import com.mana.openhand_backend.events.domainclientlayer.RegistrationSummaryResponseModel;
import com.mana.openhand_backend.identity.dataaccesslayer.MemberStatus;
import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.registrations.dataaccesslayer.Registration;
import com.mana.openhand_backend.registrations.dataaccesslayer.RegistrationRepository;
import com.mana.openhand_backend.registrations.dataaccesslayer.RegistrationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceImplMemberStatusTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private RegistrationRepository registrationRepository;

    @InjectMocks
    private EventServiceImpl eventService;

    private Event testEvent;
    private User activeUser;
    private User inactiveUser;

    @BeforeEach
    void setUp() {
        testEvent = new Event(
                "Test Event",
                "Test Description",
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2),
                "Test Location",
                "Test Address",
                EventStatus.OPEN,
                10,
                2,
                "General");

        activeUser = new User();
        activeUser.setName("Active Member");
        activeUser.setEmail("active@example.com");
        activeUser.setMemberStatus(MemberStatus.ACTIVE);

        inactiveUser = new User();
        inactiveUser.setName("Inactive Member");
        inactiveUser.setEmail("inactive@example.com");
        inactiveUser.setMemberStatus(MemberStatus.INACTIVE);

        when(eventRepository.findById(anyLong())).thenReturn(java.util.Optional.of(testEvent));
        when(registrationRepository.countByEventIdAndStatus(anyLong(), any(RegistrationStatus.class))).thenReturn(0L);
    }

    @Test
    void getRegistrationSummary_withAttendees_populatesAttendeeListWithMemberStatus() {
        // arrange
        Registration confirmedReg = new Registration(activeUser, testEvent, RegistrationStatus.CONFIRMED, LocalDateTime.now().minusDays(1));
        confirmedReg.setConfirmedAt(LocalDateTime.now().minusDays(1));

        Registration waitlistedReg = new Registration(inactiveUser, testEvent, RegistrationStatus.WAITLISTED, LocalDateTime.now());
        waitlistedReg.setWaitlistedPosition(1);

        List<Registration> registrations = Arrays.asList(confirmedReg, waitlistedReg);

        when(registrationRepository.findByEventIdAndStatusIn(
                anyLong(),
                anyList()
        )).thenReturn(registrations);

        // act
        RegistrationSummaryResponseModel result = eventService.getRegistrationSummary(1L);

        // assert
        assertNotNull(result);
        assertNotNull(result.getAttendees());
        assertEquals(2, result.getAttendees().size());
        assertEquals("ACTIVE", result.getAttendees().get(0).getMemberStatus());
        assertEquals("INACTIVE", result.getAttendees().get(1).getMemberStatus());
    }

    @Test
    void getRegistrationSummary_withNoAttendees_returnsEmptyAttendeeList() {
        // arrange
        when(registrationRepository.findByEventIdAndStatusIn(
                anyLong(),
                anyList()
        )).thenReturn(Arrays.asList());

        // act
        RegistrationSummaryResponseModel result = eventService.getRegistrationSummary(1L);

        // assert
        assertNotNull(result);
        assertNotNull(result.getAttendees());
        assertEquals(0, result.getAttendees().size());
    }

    @Test
    void getRegistrationSummary_attendeeListIncludesWaitlistedPosition() {
        // arrange
        Registration waitlistedReg = new Registration(activeUser, testEvent, RegistrationStatus.WAITLISTED, LocalDateTime.now());
        waitlistedReg.setWaitlistedPosition(3);

        when(registrationRepository.findByEventIdAndStatusIn(
                anyLong(),
                anyList()
        )).thenReturn(Arrays.asList(waitlistedReg));

        // act
        RegistrationSummaryResponseModel result = eventService.getRegistrationSummary(1L);

        // assert
        assertNotNull(result.getAttendees());
        assertEquals(1, result.getAttendees().size());
        assertEquals(3, result.getAttendees().get(0).getWaitlistedPosition());
    }

    @Test
    void getRegistrationSummary_attendeeListIncludesTimestamps() {
        // arrange
        LocalDateTime requestedTime = LocalDateTime.now().minusDays(2);
        LocalDateTime confirmedTime = LocalDateTime.now().minusDays(1);

        Registration reg = new Registration(activeUser, testEvent, RegistrationStatus.CONFIRMED, requestedTime);
        reg.setConfirmedAt(confirmedTime);

        when(registrationRepository.findByEventIdAndStatusIn(
                anyLong(),
                anyList()
        )).thenReturn(Arrays.asList(reg));

        // act
        RegistrationSummaryResponseModel result = eventService.getRegistrationSummary(1L);

        // assert
        assertNotNull(result.getAttendees());
        assertNotNull(result.getAttendees().get(0).getRequestedAt());
        assertNotNull(result.getAttendees().get(0).getConfirmedAt());
    }

    @Test
    void getRegistrationSummary_withMixedActiveAndInactiveMembers_bothDisplayedCorrectly() {
        // arrange
        User active1 = new User();
        active1.setName("Active1");
        active1.setEmail("active1@test.com");
        active1.setMemberStatus(MemberStatus.ACTIVE);

        User inactive1 = new User();
        inactive1.setName("Inactive1");
        inactive1.setEmail("inactive1@test.com");
        inactive1.setMemberStatus(MemberStatus.INACTIVE);

        User active2 = new User();
        active2.setName("Active2");
        active2.setEmail("active2@test.com");
        active2.setMemberStatus(MemberStatus.ACTIVE);

        Registration reg1 = new Registration(active1, testEvent, RegistrationStatus.CONFIRMED, LocalDateTime.now());
        Registration reg2 = new Registration(inactive1, testEvent, RegistrationStatus.CONFIRMED, LocalDateTime.now());
        Registration reg3 = new Registration(active2, testEvent, RegistrationStatus.WAITLISTED, LocalDateTime.now());
        reg3.setWaitlistedPosition(1);

        when(registrationRepository.findByEventIdAndStatusIn(
                anyLong(),
                anyList()
        )).thenReturn(Arrays.asList(reg1, reg2, reg3));

        // act
        RegistrationSummaryResponseModel result = eventService.getRegistrationSummary(1L);

        // assert
        assertEquals(3, result.getAttendees().size());
        assertEquals("ACTIVE", result.getAttendees().get(0).getMemberStatus());
        assertEquals("INACTIVE", result.getAttendees().get(1).getMemberStatus());
        assertEquals("ACTIVE", result.getAttendees().get(2).getMemberStatus());
    }
}
