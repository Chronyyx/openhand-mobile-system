package com.mana.openhand_backend.attendance.utils;

import com.mana.openhand_backend.attendance.domainclientlayer.AttendanceReportResponseModel;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public final class CsvExportUtil {

    private static final String HEADER = "Event Title,Event Date,Total Attended,Total Registered,Attendance Rate (%)";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private CsvExportUtil() {
    }

    public static String toAttendanceReportCsv(List<AttendanceReportResponseModel> reports) {
        StringBuilder csv = new StringBuilder();
        csv.append(HEADER).append('\n');

        for (AttendanceReportResponseModel report : reports) {
            String eventTitle = escapeCsv(report.getEventTitle());
            String eventDate = report.getEventDate() == null ? "" : DATE_FORMATTER.format(report.getEventDate());
            int totalAttended = report.getTotalAttended() == null ? 0 : report.getTotalAttended();
            int totalRegistered = report.getTotalRegistered() == null ? 0 : report.getTotalRegistered();
            double attendanceRatePercent = ((report.getAttendanceRate() == null ? 0.0 : report.getAttendanceRate()) * 100.0);
            String attendanceRate = String.format(Locale.US, "%.2f", attendanceRatePercent);

            csv.append(eventTitle)
                    .append(',')
                    .append(escapeCsv(eventDate))
                    .append(',')
                    .append(totalAttended)
                    .append(',')
                    .append(totalRegistered)
                    .append(',')
                    .append(attendanceRate)
                    .append('\n');
        }

        return csv.toString();
    }

    private static String escapeCsv(String value) {
        if (value == null) {
            return "";
        }

        boolean shouldQuote = value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r");
        String escaped = value.replace("\"", "\"\"");
        return shouldQuote ? "\"" + escaped + "\"" : escaped;
    }
}
