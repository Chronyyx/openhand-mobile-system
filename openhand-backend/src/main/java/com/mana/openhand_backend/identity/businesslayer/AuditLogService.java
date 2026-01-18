package com.mana.openhand_backend.identity.businesslayer;

import com.mana.openhand_backend.identity.dataaccesslayer.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface AuditLogService {
    void logRoleChange(Long affectedUserId, String affectedUserEmail, String previousRole, String newRole,
            String changedBy, String ipAddress, String userAgent, String source);

    Page<AuditLog> getAuditLogs(String search, LocalDate from, LocalDate to, String type, Pageable pageable);

    String exportAuditLogsToCsv(String search, LocalDate from, LocalDate to, String type);

    void logAccess(String username, String ipAddress, String userAgent, String searchContext, String type);
}