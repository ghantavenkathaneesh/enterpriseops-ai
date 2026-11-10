package com.enterpriseops.audit;

import com.enterpriseops.security.CurrentUser;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
    private final AuditLogRepository repo;

    public AuditService(AuditLogRepository repo) { this.repo = repo; }

    /** Logs an action by the currently authenticated user (or by the system if none). */
    public void log(String action, String entityType, Long entityId, String details) {
        logAs(CurrentUser.find().map(u -> u.getUserId()).orElse(null), action, entityType, entityId, details);
    }

    public void logAs(Long actorId, String action, String entityType, Long entityId, String details) {
        AuditLog a = new AuditLog();
        a.setActorId(actorId); a.setAction(action); a.setEntityType(entityType); a.setEntityId(entityId);
        a.setDetails(details == null ? null : details.substring(0, Math.min(details.length(), 500)));
        repo.save(a);
    }
}
