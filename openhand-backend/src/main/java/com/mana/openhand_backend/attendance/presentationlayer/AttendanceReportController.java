package com.mana.openhand_backend.attendance.presentationlayer;

import com.mana.openhand_backend.attendance.businesslayer.AttendanceReportService;
import com.mana.openhand_backend.attendance.domainclientlayer.AttendanceReportResponseModel;
import com.mana.openhand_backend.attendance.utils.CsvExportUtil;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/admin/attendance-reports")
@PreAuthorize("hasRole('ADMIN')")
public class AttendanceReportController {

    private final AttendanceReportService attendanceReportService;

    public AttendanceReportController(AttendanceReportService attendanceReportService) {
        this.attendanceReportService = attendanceReportService;
    }

    @GetMapping
    public List<AttendanceReportResponseModel> getAttendanceReports(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long eventId) {
        validateDateRange(startDate, endDate);
        return attendanceReportService.getAttendanceReports(startDate, endDate, eventId);
    }

    @GetMapping("/export")
    public ResponseEntity<String> exportAttendanceReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long eventId) {
        validateDateRange(startDate, endDate);

        List<AttendanceReportResponseModel> reportData = attendanceReportService.getAttendanceReports(startDate, endDate, eventId);
        String csvContent = CsvExportUtil.toAttendanceReportCsv(reportData);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"attendance-report.csv\"")
                .body(csvContent);
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate must be before or equal to endDate");
        }
    }
}
