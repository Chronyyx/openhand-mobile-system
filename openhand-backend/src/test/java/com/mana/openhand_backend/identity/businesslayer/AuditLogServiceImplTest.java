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

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

    @Test
    void logAccess_withChangesSearchContext_buildsSearchDetails() {
        auditLogService.logAccess("admin", "127.0.0.1", "Agent", "john@example.com", "CHANGES");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertTrue(saved.getAffectedUserEmail().contains("Searched User Changes"));
        assertTrue(saved.getAffectedUserEmail().contains("john@example.com"));
        assertEquals("admin", saved.getChangedBy());
        assertEquals("AUDIT_ACCESS", saved.getSource());
    }

    @Test
    void logAccess_withAccessAndNoSearch_usesBaseAction() {
        auditLogService.logAccess("admin", "127.0.0.1", "Agent", "", "ACCESS");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertEquals("Viewed Admin Access Logs", saved.getAffectedUserEmail());
    }

    @Test
    void createSpecification_withSearchFiltersAndAccessType_executesPredicateBranches() {
        Page<AuditLog> page = new PageImpl<>(Collections.emptyList());
        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        auditLogService.getAuditLogs("john", LocalDate.now().minusDays(1), LocalDate.now(), "ACCESS",
                Pageable.unpaged());

        ArgumentCaptor<Specification<AuditLog>> specCaptor = ArgumentCaptor.forClass(Specification.class);
        verify(auditLogRepository).findAll(specCaptor.capture(), any(Pageable.class));

        Specification<AuditLog> spec = specCaptor.getValue();
        Root<AuditLog> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Predicate predicate = mock(Predicate.class);

        Path<String> emailPath = mock(Path.class);
        Path<String> changedByPath = mock(Path.class);
        Path<String> newRolePath = mock(Path.class);
        Path<String> previousRolePath = mock(Path.class);
        Path<LocalDateTime> changedAtPath = mock(Path.class);
        Path<String> sourcePath = mock(Path.class);

        lenient().when(root.get(eq("affectedUserEmail"))).thenReturn((Path) emailPath);
        lenient().when(root.get(eq("changedBy"))).thenReturn((Path) changedByPath);
        lenient().when(root.get(eq("newRole"))).thenReturn((Path) newRolePath);
        lenient().when(root.get(eq("previousRole"))).thenReturn((Path) previousRolePath);
        lenient().when(root.get(eq("changedAt"))).thenReturn((Path) changedAtPath);
        lenient().when(root.get(eq("source"))).thenReturn((Path) sourcePath);

        lenient().when(cb.lower(emailPath)).thenReturn(emailPath);
        lenient().when(cb.lower(changedByPath)).thenReturn(changedByPath);
        lenient().when(cb.lower(newRolePath)).thenReturn(newRolePath);
        lenient().when(cb.lower(previousRolePath)).thenReturn(previousRolePath);
        lenient().when(cb.like(any(Expression.class), any(String.class))).thenReturn(predicate);
        lenient().when(cb.or(any(Predicate.class), any(Predicate.class), any(Predicate.class), any(Predicate.class)))
                .thenReturn(predicate);
        lenient().when(cb.greaterThanOrEqualTo(any(Expression.class), any(LocalDateTime.class))).thenReturn(predicate);
        lenient().when(cb.lessThanOrEqualTo(any(Expression.class), any(LocalDateTime.class))).thenReturn(predicate);
        lenient().when(cb.equal(any(Expression.class), any())).thenReturn(predicate);
        lenient().when(cb.and(any(Predicate[].class))).thenReturn(predicate);

        spec.toPredicate(root, query, cb);

        verify(cb).equal(sourcePath, "AUDIT_ACCESS");
        verify(cb).and(any(Predicate[].class));
    }

    @Test
    void createSpecification_withChangesTypeAndEmptySearch_executesBranches() {
        Page<AuditLog> page = new PageImpl<>(Collections.emptyList());
        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        auditLogService.getAuditLogs(" ", null, null, "CHANGES", Pageable.unpaged());

        ArgumentCaptor<Specification<AuditLog>> specCaptor = ArgumentCaptor.forClass(Specification.class);
        verify(auditLogRepository).findAll(specCaptor.capture(), any(Pageable.class));

        Specification<AuditLog> spec = specCaptor.getValue();
        Root<AuditLog> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Predicate predicate = mock(Predicate.class);

        Path<String> sourcePath = mock(Path.class);
        lenient().when(root.get(eq("source"))).thenReturn((Path) sourcePath);
        lenient().when(cb.notEqual(any(Expression.class), any())).thenReturn(predicate);
        lenient().when(cb.and(any(Predicate[].class))).thenReturn(predicate);

        spec.toPredicate(root, query, cb);

        verify(cb).notEqual(sourcePath, "AUDIT_ACCESS");
    }
}
