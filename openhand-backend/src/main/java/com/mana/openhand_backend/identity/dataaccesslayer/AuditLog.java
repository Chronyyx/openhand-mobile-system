package com.mana.openhand_backend.identity.dataaccesslayer;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "affected_user_id")
    private Long affectedUserId;

    @Column(name = "affected_user_email", nullable = false)
    private String affectedUserEmail;

    @Column(name = "previous_role")
    private String previousRole;

    @Column(name = "new_role")
    private String newRole;

    @Column(name = "changed_by", nullable = false)
    private String changedBy;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "source")
    private String source;

    public AuditLog() {
    }

    public AuditLog(Long affectedUserId, String affectedUserEmail, String previousRole, String newRole, String changedBy, LocalDateTime changedAt, String ipAddress, String userAgent, String source) {
        this.affectedUserId = affectedUserId;
        this.affectedUserEmail = affectedUserEmail;
        this.previousRole = previousRole;
        this.newRole = newRole;
        this.changedBy = changedBy;
        this.changedAt = changedAt;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.source = source;
    }

    public Long getId() {
        return id;
    }

    public Long getAffectedUserId() {
        return affectedUserId;
    }

    public String getAffectedUserEmail() {
        return affectedUserEmail;
    }

    public String getPreviousRole() {
        return previousRole;
    }

    public String getNewRole() {
        return newRole;
    }

    public String getChangedBy() {
        return changedBy;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getSource() {
        return source;
    }
}
