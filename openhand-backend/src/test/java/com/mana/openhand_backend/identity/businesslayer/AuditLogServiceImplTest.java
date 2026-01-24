package com.mana.openhand_backend.identity.businesslayer;

import com.mana.openhand_backend.identity.dataaccesslayer.AuditLog;
import com.mana.openhand_backend.identity.dataaccesslayer.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceImplTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogServiceImpl auditLogService;

    @Test
    void logRoleChange_savesAuditLog() {
        auditLogService.logRoleChange(1L, "user@example.com", "ROLE_MEMBER", "ROLE_ADMIN",
                "admin", "127.0.0.1", "agent", "ADMIN_CONSOLE");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertEquals(1L, saved.getAffectedUserId());
        assertEquals("user@example.com", saved.getAffectedUserEmail());
        assertEquals("ROLE_MEMBER", saved.getPreviousRole());
        assertEquals("ROLE_ADMIN", saved.getNewRole());
        assertEquals("admin", saved.getChangedBy());
        assertNotNull(saved.getChangedAt());
        assertEquals("127.0.0.1", saved.getIpAddress());
        assertEquals("agent", saved.getUserAgent());
        assertEquals("ADMIN_CONSOLE", saved.getSource());
    }

    @Test
    void getAuditLogs_delegatesToRepository() {
        Page<AuditLog> page = new PageImpl<>(List.of());
        when(auditLogRepository.findAll(anySpecification(), any(Pageable.class))).thenReturn(page);

        Page<AuditLog> result = auditLogService.getAuditLogs("query", LocalDate.now(), null, "CHANGES",
                Pageable.unpaged());

        assertEquals(0, result.getTotalElements());
        verify(auditLogRepository).findAll(anySpecification(), any(Pageable.class));
    }

    @Test
    void exportAuditLogsToCsv_escapesValues() {
        AuditLog log = new AuditLog(
                1L,
                "user@example.com",
                "ROLE_MEMBER",
                "ROLE_ADMIN",
                "admin",
                LocalDateTime.of(2025, 1, 1, 9, 0),
                "127.0.0.1",
                "Agent, \"Test\"",
                "ADMIN_CONSOLE"
        );
        ReflectionTestUtils.setField(log, "id", 7L);
        when(auditLogRepository.findAll(anySpecification())).thenReturn(List.of(log));

        String csv = auditLogService.exportAuditLogsToCsv("", null, null, "");

        assertTrue(csv.contains("ID,Timestamp,Changed By,Affected User"));
        assertTrue(csv.contains("7,"));
        assertTrue(csv.contains("\"Agent, \"\"Test\"\"\""));
    }

    @Test
    void logAccess_withSearchContext_buildsContextMessage() {
        auditLogService.logAccess("admin", "10.0.0.1", "agent", "email=jane", "CHANGES");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertEquals("Searched User Changes: email=jane", saved.getAffectedUserEmail());
        assertEquals("AUDIT_ACCESS", saved.getSource());
    }

    @Test
    void logAccess_whenAccessTypeWithoutSearch_usesAccessLabel() {
        auditLogService.logAccess("admin", "10.0.0.1", "agent", "", "ACCESS");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertEquals("Viewed Admin Access Logs", saved.getAffectedUserEmail());
        assertEquals("AUDIT_ACCESS", saved.getSource());
    }

    @Test
    void logAccess_whenTypeNull_usesDefaultLabel() {
        auditLogService.logAccess("admin", "10.0.0.1", "agent", null, null);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertEquals("Viewed Logs", saved.getAffectedUserEmail());
        assertEquals("AUDIT_ACCESS", saved.getSource());
    }

    private Specification<AuditLog> anySpecification() {
        return any();
    }
}
