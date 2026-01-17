package com.mana.openhand_backend.identity.businesslayer;

import com.mana.openhand_backend.identity.dataaccesslayer.AuditLog;
import com.mana.openhand_backend.identity.dataaccesslayer.AuditLogRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogServiceImpl(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    public void logRoleChange(Long affectedUserId, String affectedUserEmail, String previousRole, String newRole,
            String changedBy, String ipAddress, String userAgent, String source) {
        AuditLog log = new AuditLog(affectedUserId, affectedUserEmail, previousRole, newRole, changedBy,
                LocalDateTime.now(), ipAddress, userAgent, source);
        auditLogRepository.save(log);
    }

    @Override
    public Page<AuditLog> getAuditLogs(String search, LocalDate from, LocalDate to, String type, Pageable pageable) {
        return auditLogRepository.findAll(createSpecification(search, from, to, type), pageable);
    }

    @Override
    public String exportAuditLogsToCsv(String search, LocalDate from, LocalDate to, String type) {
        List<AuditLog> logs = auditLogRepository.findAll(createSpecification(search, from, to, type));
        StringBuilder csv = new StringBuilder();
        csv.append(
                "ID,Timestamp,Changed By,Affected User,Affected User ID,Previous Role,New Role,Source,IP Address,User Agent\n");

        for (AuditLog log : logs) {
            csv.append(escape(log.getId())).append(",")
                    .append(escape(log.getChangedAt())).append(",")
                    .append(escape(log.getChangedBy())).append(",")
                    .append(escape(log.getAffectedUserEmail())).append(",")
                    .append(escape(log.getAffectedUserId())).append(",")
                    .append(escape(log.getPreviousRole())).append(",")
                    .append(escape(log.getNewRole())).append(",")
                    .append(escape(log.getSource())).append(",")
                    .append(escape(log.getIpAddress())).append(",")
                    .append(escape(log.getUserAgent())).append("\n");
        }
        return csv.toString();
    }

    @Override
    public void logAccess(String username, String ipAddress, String userAgent, String searchContext) {
        // "Audit the auditor" - log that someone accessed the logs
        // We track this as a special "ACCESS" event
        // If there was a search, we log what they looked for in the 'affectedUser'
        // column or context

        String contextInfo = (searchContext != null && !searchContext.isEmpty()) ? "Searched: " + searchContext
                : "Viewed Logs";

        AuditLog log = new AuditLog(null, contextInfo, "N/A", "N/A", username, LocalDateTime.now(), ipAddress,
                userAgent, "AUDIT_ACCESS");
        auditLogRepository.save(log);
    }

    private Specification<AuditLog> createSpecification(String search, LocalDate from, LocalDate to, String type) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (search != null && !search.trim().isEmpty()) {
                String likePattern = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("affectedUserEmail")), likePattern),
                        cb.like(cb.lower(root.get("changedBy")), likePattern),
                        cb.like(cb.lower(root.get("newRole")), likePattern),
                        cb.like(cb.lower(root.get("previousRole")), likePattern)
                ));
            }

            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("changedAt"), from.atStartOfDay()));
            }

            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("changedAt"), to.atTime(23, 59, 59)));
            }

            if (type != null && !type.isEmpty()) {
                if ("ACCESS".equalsIgnoreCase(type)) {
                    predicates.add(cb.equal(root.get("source"), "AUDIT_ACCESS"));
                } else if ("CHANGES".equalsIgnoreCase(type)) {
                    predicates.add(cb.notEqual(root.get("source"), "AUDIT_ACCESS"));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private String escape(Object value) {
        if (value == null)
            return "";
        String str = value.toString();
        if (str.contains(",") || str.contains("\"") || str.contains("\n")) {
            return "\"" + str.replace("\"", "\"\"") + "\"";
        }
        return str;
    }
}
