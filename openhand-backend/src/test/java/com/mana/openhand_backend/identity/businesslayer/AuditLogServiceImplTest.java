package com.mana.openhand_backend.identity.businesslayer;

import com.mana.openhand_backend.identity.dataaccesslayer.AuditLog;
import com.mana.openhand_backend.identity.dataaccesslayer.AuditLogRepository;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuditLogServiceImplTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogServiceImpl service;

    @Test
    void exportAuditLogsToCsv_escapesSpecialCharacters() {
        AuditLog log = new AuditLog(10L, "user@example.com", "ROLE_MEMBER", "ROLE_ADMIN",
                "admin@example.com", LocalDateTime.of(2025, 1, 1, 10, 0), "127.0.0.1",
                "Agent,With,Comma", "ADMIN_CONSOLE");
        ReflectionTestUtils.setField(log, "id", 99L);

        when(auditLogRepository.findAll(any(Specification.class))).thenReturn(List.of(log));

        String csv = service.exportAuditLogsToCsv(null, null, null, null);
        assertTrue(csv.contains("ID,Timestamp"));
        assertTrue(csv.contains("\"Agent,With,Comma\""));
    }

    @Test
    void logAccess_buildsContextForChangesAndAccess() {
        service.logAccess("admin", "127.0.0.1", "JUnit", "john@example.com", "CHANGES");
        service.logAccess("admin", "127.0.0.1", "JUnit", "", "ACCESS");

        verify(auditLogRepository, times(2)).save(any(AuditLog.class));
    }

    @Test
    void getAuditLogs_buildsSpecification_withSearchDatesAndAccessType() {
        Page<AuditLog> empty = new PageImpl<>(List.of());
        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(empty);

        service.getAuditLogs("query", LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 2),
                "ACCESS", PageRequest.of(0, 10));

        ArgumentCaptor<Specification<AuditLog>> specCaptor = ArgumentCaptor.forClass(Specification.class);
        verify(auditLogRepository).findAll(specCaptor.capture(), any(PageRequest.class));

        Specification<AuditLog> spec = specCaptor.getValue();

        Root<AuditLog> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Predicate predicate = mock(Predicate.class);

        @SuppressWarnings("unchecked")
        Path<Object> path = mock(Path.class);
        @SuppressWarnings("unchecked")
        Expression<String> expression = mock(Expression.class);
        when(root.get(anyString())).thenReturn(path);
        when(cb.lower(any())).thenReturn(expression);
        when(cb.like(any(), anyString())).thenReturn(predicate);
        when(cb.or(any(Predicate.class), any(Predicate.class), any(Predicate.class), any(Predicate.class)))
                .thenReturn(predicate);
        when(cb.greaterThanOrEqualTo(any(), any(LocalDateTime.class))).thenReturn(predicate);
        when(cb.lessThanOrEqualTo(any(), any(LocalDateTime.class))).thenReturn(predicate);
        when(cb.equal(any(), any())).thenReturn(predicate);
        when(cb.and(any(Predicate[].class))).thenReturn(predicate);

        assertNotNull(spec.toPredicate(root, query, cb));
        verify(cb).equal(any(), eq("AUDIT_ACCESS"));
    }

    @Test
    void getAuditLogs_buildsSpecification_forChangesType() {
        Page<AuditLog> empty = new PageImpl<>(List.of());
        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(empty);

        service.getAuditLogs(null, null, null, "CHANGES", PageRequest.of(0, 10));

        ArgumentCaptor<Specification<AuditLog>> specCaptor = ArgumentCaptor.forClass(Specification.class);
        verify(auditLogRepository).findAll(specCaptor.capture(), any(PageRequest.class));

        Specification<AuditLog> spec = specCaptor.getValue();

        Root<AuditLog> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Predicate predicate = mock(Predicate.class);

        @SuppressWarnings("unchecked")
        Path<Object> path = mock(Path.class);
        when(root.get(anyString())).thenReturn(path);
        when(cb.notEqual(any(), any())).thenReturn(predicate);
        when(cb.and(any(Predicate[].class))).thenReturn(predicate);

        assertNotNull(spec.toPredicate(root, query, cb));
        verify(cb).notEqual(any(), eq("AUDIT_ACCESS"));
    }
}
