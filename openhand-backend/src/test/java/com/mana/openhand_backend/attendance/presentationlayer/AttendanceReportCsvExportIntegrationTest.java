package com.mana.openhand_backend.attendance.presentationlayer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mana.openhand_backend.events.dataaccesslayer.Event;
import com.mana.openhand_backend.events.dataaccesslayer.EventRepository;
import com.mana.openhand_backend.events.dataaccesslayer.EventStatus;
import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.identity.dataaccesslayer.UserRepository;
import com.mana.openhand_backend.notifications.dataaccesslayer.NotificationPreferenceRepository;
import com.mana.openhand_backend.registrations.dataaccesslayer.Registration;
import com.mana.openhand_backend.registrations.dataaccesslayer.RegistrationRepository;
import com.mana.openhand_backend.registrations.dataaccesslayer.RegistrationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = true)
@TestPropertySource(locations = "classpath:application-test.properties")
class AttendanceReportCsvExportIntegrationTest {

    private static final String CSV_HEADER = "Event Title,Event Date,Total Attended,Total Registered,Attendance Rate (%)";
    private static final DateTimeFormatter JSON_DATE_OUTPUT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private RegistrationRepository registrationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationPreferenceRepository notificationPreferenceRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    @Transactional
    void setUp() {
        registrationRepository.deleteAll();
        registrationRepository.flush();
        eventRepository.deleteAll();
        eventRepository.flush();
        notificationPreferenceRepository.deleteAll();
        notificationPreferenceRepository.flush();
        userRepository.deleteAll();
        userRepository.flush();
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    @Transactional
    void adminCanDownloadCsv_returns200AndCsvContentType() throws Exception {
        Event event = createEvent("CSV Event", LocalDateTime.of(2026, 1, 15, 9, 30));
        createRegistration(event, true, RegistrationStatus.CONFIRMED);

        mockMvc.perform(get("/api/admin/attendance-reports/export")
                        .param("startDate", "2026-01-01")
                        .param("endDate", "2026-01-31"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(content().string(startsWith(CSV_HEADER)));
    }

    @Test
    @WithMockUser(username = "member@example.com", roles = "MEMBER")
    void nonAdminGets403() throws Exception {
        mockMvc.perform(get("/api/admin/attendance-reports/export")
                        .param("startDate", "2026-01-01")
                        .param("endDate", "2026-01-31"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    @Transactional
    void csvRowsMatchReportEndpointValues() throws Exception {
        Event eventOne = createEvent("Gala, Montreal", LocalDateTime.of(2026, 2, 10, 10, 0));
        Event eventTwo = createEvent("Workshop \"A\"", LocalDateTime.of(2026, 2, 11, 14, 15));

        createRegistration(eventOne, true, RegistrationStatus.CONFIRMED);
        createRegistration(eventOne, false, RegistrationStatus.CONFIRMED);
        createRegistration(eventTwo, true, RegistrationStatus.CONFIRMED);

        MvcResult jsonResult = mockMvc.perform(get("/api/admin/attendance-reports")
                        .param("startDate", "2026-02-01")
                        .param("endDate", "2026-02-28"))
                .andExpect(status().isOk())
                .andReturn();

        MvcResult csvResult = mockMvc.perform(get("/api/admin/attendance-reports/export")
                        .param("startDate", "2026-02-01")
                        .param("endDate", "2026-02-28"))
                .andExpect(status().isOk())
                .andReturn();

        List<Map<String, Object>> jsonRows = objectMapper.readValue(
                jsonResult.getResponse().getContentAsString(),
                new TypeReference<>() {
                }
        );
        List<Map<String, String>> csvRows = parseCsv(csvResult.getResponse().getContentAsString());

        assertEquals(jsonRows.size(), csvRows.size());
        for (int i = 0; i < jsonRows.size(); i++) {
            Map<String, Object> jsonRow = jsonRows.get(i);
            Map<String, String> csvRow = csvRows.get(i);

            assertEquals(String.valueOf(jsonRow.get("eventTitle")), csvRow.get("Event Title"));
            assertEquals(
                    formatJsonDate(String.valueOf(jsonRow.get("eventDate"))),
                    csvRow.get("Event Date")
            );
            assertEquals(String.valueOf(jsonRow.get("totalAttended")), csvRow.get("Total Attended"));
            assertEquals(String.valueOf(jsonRow.get("totalRegistered")), csvRow.get("Total Registered"));

            double attendanceRateRatio = ((Number) jsonRow.get("attendanceRate")).doubleValue();
            String expectedPercent = String.format(java.util.Locale.US, "%.2f", attendanceRateRatio * 100.0);
            assertEquals(expectedPercent, csvRow.get("Attendance Rate (%)"));
        }
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void emptyResultReturnsHeaderOnlyCsv() throws Exception {
        MvcResult csvResult = mockMvc.perform(get("/api/admin/attendance-reports/export")
                        .param("startDate", "2026-06-01")
                        .param("endDate", "2026-06-30"))
                .andExpect(status().isOk())
                .andReturn();

        String csv = csvResult.getResponse().getContentAsString();
        List<String> nonEmptyLines = csv.lines().filter(line -> !line.isBlank()).toList();

        assertEquals(1, nonEmptyLines.size());
        assertEquals(CSV_HEADER, nonEmptyLines.get(0));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    @Transactional
    void dateFilteringAppliesToCsv() throws Exception {
        Event inRange = createEvent("In Range Event", LocalDateTime.of(2026, 3, 10, 11, 0));
        createEvent("Out Of Range Event", LocalDateTime.of(2026, 4, 10, 11, 0));
        createRegistration(inRange, true, RegistrationStatus.CONFIRMED);

        String csv = mockMvc.perform(get("/api/admin/attendance-reports/export")
                        .param("startDate", "2026-03-01")
                        .param("endDate", "2026-03-31"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        List<Map<String, String>> rows = parseCsv(csv);
        assertEquals(1, rows.size());
        assertEquals("In Range Event", rows.get(0).get("Event Title"));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    @Transactional
    void eventIdFilteringAppliesToCsv() throws Exception {
        Event target = createEvent("Target Event", LocalDateTime.of(2026, 5, 8, 10, 0));
        Event other = createEvent("Other Event", LocalDateTime.of(2026, 5, 9, 10, 0));
        createRegistration(target, true, RegistrationStatus.CONFIRMED);
        createRegistration(other, true, RegistrationStatus.CONFIRMED);

        String csv = mockMvc.perform(get("/api/admin/attendance-reports/export")
                        .param("startDate", "2026-05-01")
                        .param("endDate", "2026-05-31")
                        .param("eventId", String.valueOf(target.getId())))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        List<Map<String, String>> rows = parseCsv(csv);
        assertEquals(1, rows.size());
        assertEquals("Target Event", rows.get(0).get("Event Title"));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void filenameHeaderPresent() throws Exception {
        mockMvc.perform(get("/api/admin/attendance-reports/export")
                        .param("startDate", "2026-01-01")
                        .param("endDate", "2026-01-31"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"attendance-report.csv\""));
    }

    private Event createEvent(String title, LocalDateTime startDateTime) {
        Event event = new Event(
                title,
                "CSV export test event",
                startDateTime,
                startDateTime.plusHours(2),
                "MANA Center",
                "1910 Test Blvd",
                EventStatus.OPEN,
                100,
                0,
                "General"
        );
        return eventRepository.saveAndFlush(event);
    }

    private void createRegistration(Event event, boolean checkedIn, RegistrationStatus status) {
        User user = new User();
        user.setEmail("user-" + System.nanoTime() + "@example.com");
        user.setPasswordHash("hashedPassword");
        user.setRoles(new HashSet<>());
        User savedUser = userRepository.saveAndFlush(user);

        Registration registration = new Registration(savedUser, event);
        registration.setStatus(status);
        registration.setRequestedAt(LocalDateTime.now());
        if (status == RegistrationStatus.CONFIRMED) {
            registration.setConfirmedAt(LocalDateTime.now());
        }
        if (checkedIn) {
            registration.setCheckedInAt(LocalDateTime.now());
        }
        if (status == RegistrationStatus.CANCELLED) {
            registration.setCancelledAt(LocalDateTime.now());
        }
        registrationRepository.saveAndFlush(registration);
    }

    private String formatJsonDate(String jsonDateTime) {
        return LocalDateTime.parse(jsonDateTime).format(JSON_DATE_OUTPUT);
    }

    private List<Map<String, String>> parseCsv(String csv) {
        List<String> lines = csv.lines().filter(line -> !line.isBlank()).toList();
        assertFalse(lines.isEmpty());

        List<String> headerColumns = parseCsvLine(lines.get(0));
        assertEquals(CSV_HEADER, String.join(",", headerColumns));

        List<Map<String, String>> rows = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            List<String> rowValues = parseCsvLine(lines.get(i));
            assertEquals(headerColumns.size(), rowValues.size());

            Map<String, String> row = new HashMap<>();
            for (int j = 0; j < headerColumns.size(); j++) {
                row.put(headerColumns.get(j), rowValues.get(j));
            }
            rows.add(row);
        }

        return rows;
    }

    private List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }

        values.add(current.toString());
        assertNotNull(values);
        assertTrue(values.size() >= 1);
        return values;
    }
}
