package com.mana.openhand_backend.attendance.businesslayer;

import com.mana.openhand_backend.attendance.domainclientlayer.AttendanceReportResponseModel;

import java.time.LocalDate;
import java.util.List;

public interface AttendanceReportService {

    List<AttendanceReportResponseModel> getAttendanceReports(LocalDate startDate, LocalDate endDate, Long eventId);
}
