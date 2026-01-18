package com.mana.openhand_backend.identity.businesslayer;

import com.mana.openhand_backend.identity.dataaccesslayer.AuditLog;
import com.mana.openhand_backend.identity.dataaccesslayer.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceImplTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    private AuditLogServiceImpl auditLogService;

    @BeforeEach
    void setUp() {
        auditLogService = new AuditLogServiceImpl(auditLogRepository);
    }

    @Test
    void logRoleChange_savesAuditLog() {
        auditLogService.logRoleChange(1L, "user@example.com", "OLD", "NEW", "admin", "127.0.0.1", "Agent", "API");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertEquals("user@example.com", saved.getAffectedUserEmail());
        assertEquals("OLD", saved.getPreviousRole());
        assertEquals("NEW", saved.getNewRole());
        assertEquals("admin", saved.getChangedBy());
        assertEquals("127.0.0.1", saved.getIpAddress());
    }

    @Test
    void getAuditLogs_returnsPage() {
        Page<AuditLog> page = new PageImpl<>(Collections.emptyList());
        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        Page<AuditLog> result = auditLogService.getAuditLogs("search", null, null, null, Pageable.unpaged());

        assertSame(page, result);
    }

    @Test
    void exportAuditLogsToCsv_returnsCsvString() {
        AuditLog log = new AuditLog(1L, "user@example.com", "OLD", "NEW", "admin", LocalDateTime.now(), "127.0.0.1",
                "Agent", "API");
        when(auditLogRepository.findAll(any(Specification.class))).thenReturn(List.of(log));

        String csv = auditLogService.exportAuditLogsToCsv(null, null, null, null);

        assertTrue(csv.contains("ID,Timestamp"));
        assertTrue(csv.contains("user@example.com"));
        assertTrue(csv.contains("OLD"));
        assertTrue(csv.contains("NEW"));
    }
}
