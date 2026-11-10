package com.enterpriseops.incident;

import com.enterpriseops.asset.Asset;
import com.enterpriseops.asset.AssetRepository;
import com.enterpriseops.audit.AuditService;
import com.enterpriseops.common.BadRequestException;
import com.enterpriseops.common.NotFoundException;
import com.enterpriseops.common.PageResponse;
import com.enterpriseops.incident.IncidentDtos.*;
import com.enterpriseops.notification.NotificationService;
import com.enterpriseops.ticket.Priority;
import com.enterpriseops.ticket.TicketRepository;
import com.enterpriseops.user.User;
import com.enterpriseops.user.UserRepository;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IncidentService {
    private static final Set<String> ASSIGNABLE_ROLES = Set.of("ADMIN", "MANAGER", "SUPPORT_AGENT");

    private final IncidentRepository incidents;
    private final AssetRepository assets;
    private final TicketRepository tickets;
    private final UserRepository users;
    private final NotificationService notifications;
    private final AuditService audit;

    public IncidentService(IncidentRepository incidents, AssetRepository assets, TicketRepository tickets, UserRepository users,
                           NotificationService notifications, AuditService audit) {
        this.incidents = incidents; this.assets = assets; this.tickets = tickets; this.users = users;
        this.notifications = notifications; this.audit = audit;
    }

    @Transactional
    public IncidentResponse create(CreateIncidentRequest r) {
        Incident i = new Incident();
        i.setTitle(r.title()); i.setDescription(r.description()); i.setSeverity(r.severity());
        if (r.assetId() != null) i.setAsset(assets.findById(r.assetId()).orElseThrow(() -> new NotFoundException("Asset", r.assetId())));
        if (r.ticketId() != null) i.setTicket(tickets.findById(r.ticketId()).orElseThrow(() -> new NotFoundException("Ticket", r.ticketId())));
        Incident saved = incidents.save(i);
        audit.log("INCIDENT_CREATED", "Incident", saved.getId(), "severity=" + r.severity());
        return IncidentResponse.from(saved);
    }

    /** Called by the monitoring pipeline inside its transaction. Returns the existing active incident if one exists (dedupe). */
    @Transactional
    public Incident createFromMonitoring(Asset asset, Priority severity, String title, String description) {
        Incident i = new Incident();
        i.setTitle(title); i.setDescription(description); i.setSeverity(severity);
        i.setSource(Incident.Source.MONITORING); i.setAsset(asset);
        Incident saved = incidents.save(i);
        audit.log("INCIDENT_AUTO_CREATED", "Incident", saved.getId(), "asset=" + asset.getId());
        notifications.notifyRoles(List.of("ADMIN", "MANAGER", "SUPPORT_AGENT"), "INCIDENT_ALERT",
            "[" + severity + "] " + title, "/incidents/" + saved.getId());
        return saved;
    }

    @Transactional(readOnly = true)
    public PageResponse<IncidentResponse> search(IncidentStatus status, Priority severity, Long assetId, Pageable pageable) {
        Specification<Incident> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (status != null) ps.add(cb.equal(root.get("status"), status));
            if (severity != null) ps.add(cb.equal(root.get("severity"), severity));
            if (assetId != null) ps.add(cb.equal(root.get("asset").get("id"), assetId));
            return cb.and(ps.toArray(new Predicate[0]));
        };
        return PageResponse.of(incidents.findAll(spec, pageable), IncidentResponse::from);
    }

    @Transactional(readOnly = true)
    public IncidentResponse get(Long id) { return IncidentResponse.from(find(id)); }

    @Transactional
    public IncidentResponse assign(Long id, Long assigneeId) {
        Incident i = find(id);
        User u = users.findById(assigneeId).orElseThrow(() -> new NotFoundException("User", assigneeId));
        if (!u.isActive() || !ASSIGNABLE_ROLES.contains(u.getRole().getName())) {
            throw new BadRequestException("Incidents can only be assigned to active staff members");
        }
        i.setAssignee(u);
        notifications.notifyUser(u.getId(), "INCIDENT_ASSIGNED", "Incident #" + id + " was assigned to you", "/incidents/" + id);
        audit.log("INCIDENT_ASSIGNED", "Incident", id, "assignee=" + assigneeId);
        return IncidentResponse.from(incidents.save(i));
    }

    @Transactional
    public IncidentResponse changeStatus(Long id, IncidentStatus next, String resolutionNotes) {
        Incident i = find(id);
        IncidentStatus current = i.getStatus();
        if (current != next && !current.canTransitionTo(next)) {
            throw new BadRequestException("Cannot move an incident from " + current + " to " + next);
        }
        if (next == IncidentStatus.RESOLVED) {
            if (resolutionNotes == null || resolutionNotes.isBlank()) {
                throw new BadRequestException("Resolution notes are required to resolve an incident");
            }
            i.setResolutionNotes(resolutionNotes);
            i.setResolvedAt(Instant.now());
        } else if (current == IncidentStatus.RESOLVED && next == IncidentStatus.INVESTIGATING) {
            i.setResolvedAt(null);
        }
        i.setStatus(next);
        audit.log("INCIDENT_STATUS", "Incident", id, current + " -> " + next);
        return IncidentResponse.from(incidents.save(i));
    }

    private Incident find(Long id) { return incidents.findById(id).orElseThrow(() -> new NotFoundException("Incident", id)); }
}
