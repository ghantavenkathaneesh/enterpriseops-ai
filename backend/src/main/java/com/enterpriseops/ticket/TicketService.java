package com.enterpriseops.ticket;

import com.enterpriseops.ai.AiClient;
import com.enterpriseops.ai.AiPrediction;
import com.enterpriseops.ai.AiPredictionRepository;
import com.enterpriseops.asset.AssetRepository;
import com.enterpriseops.audit.AuditService;
import com.enterpriseops.common.BadRequestException;
import com.enterpriseops.common.NotFoundException;
import com.enterpriseops.common.PageResponse;
import com.enterpriseops.notification.NotificationService;
import com.enterpriseops.security.AppUserDetails;
import com.enterpriseops.ticket.TicketDtos.*;
import com.enterpriseops.user.User;
import com.enterpriseops.user.UserRepository;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketService {
    private static final Set<String> ASSIGNABLE_ROLES = Set.of("ADMIN", "MANAGER", "SUPPORT_AGENT");

    private final TicketRepository tickets;
    private final TicketCommentRepository comments;
    private final SlaPolicyRepository sla;
    private final UserRepository users;
    private final AssetRepository assets;
    private final AiClient ai;
    private final AiPredictionRepository predictions;
    private final NotificationService notifications;
    private final AuditService audit;

    public TicketService(TicketRepository tickets, TicketCommentRepository comments, SlaPolicyRepository sla,
                         UserRepository users, AssetRepository assets, AiClient ai, AiPredictionRepository predictions,
                         NotificationService notifications, AuditService audit) {
        this.tickets = tickets; this.comments = comments; this.sla = sla; this.users = users; this.assets = assets;
        this.ai = ai; this.predictions = predictions; this.notifications = notifications; this.audit = audit;
    }

    @Transactional
    public TicketResponse create(CreateTicketRequest r, AppUserDetails me) {
        Ticket t = new Ticket();
        t.setTitle(r.title());
        t.setDescription(r.description());
        Priority p = r.priority() == null ? Priority.MEDIUM : r.priority();
        t.setPriority(p);
        t.setReporter(users.getReferenceById(me.getUserId()));

        Optional<AiClient.Classification> cls = Optional.empty();
        String category = r.category();
        String text = r.title() + " " + r.description();
        if (category == null || category.isBlank()) {
            cls = ai.classify(text);                       // falls back to "other" if the AI service is down
            category = cls.map(AiClient.Classification::category).orElse("other");
        }
        t.setCategory(category.toLowerCase());
        if (r.assetId() != null) {
            t.setAsset(assets.findById(r.assetId()).orElseThrow(() -> new NotFoundException("Asset", r.assetId())));
        }
        sla.findByPriority(p).ifPresent(s -> t.setDueAt(Instant.now().plus(s.getResolutionMinutes(), ChronoUnit.MINUTES)));
        Ticket saved = tickets.save(t);
        cls.ifPresent(c -> predictions.save(AiPrediction.classification(saved.getId(), text, c.category(), c.confidence(), c.modelVersion())));
        audit.log("TICKET_CREATED", "Ticket", saved.getId(), "priority=" + p + ", category=" + saved.getCategory());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<TicketResponse> search(TicketStatus status, Priority priority, String category, Long assigneeId,
                                               String q, Pageable pageable, AppUserDetails me) {
        Long reporterOnly = me.isEmployee() ? me.getUserId() : null;   // employees only ever see their own tickets
        Specification<Ticket> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (status != null) ps.add(cb.equal(root.get("status"), status));
            if (priority != null) ps.add(cb.equal(root.get("priority"), priority));
            if (category != null && !category.isBlank()) ps.add(cb.equal(root.get("category"), category.toLowerCase()));
            if (assigneeId != null) ps.add(cb.equal(root.get("assignee").get("id"), assigneeId));
            if (reporterOnly != null) ps.add(cb.equal(root.get("reporter").get("id"), reporterOnly));
            if (q != null && !q.isBlank()) {
                String like = "%" + q.toLowerCase() + "%";
                ps.add(cb.or(cb.like(cb.lower(root.get("title")), like), cb.like(cb.lower(root.get("description")), like)));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        return PageResponse.of(tickets.findAll(spec, pageable), this::toResponse);
    }

    @Transactional(readOnly = true)
    public TicketResponse get(Long id, AppUserDetails me) { return toResponse(load(id, me)); }

    @Transactional
    public TicketResponse update(Long id, UpdateTicketRequest r) {
        Ticket t = tickets.findById(id).orElseThrow(() -> new NotFoundException("Ticket", id));
        if (r.title() != null && !r.title().isBlank()) t.setTitle(r.title());
        if (r.description() != null && !r.description().isBlank()) t.setDescription(r.description());
        if (r.category() != null && !r.category().isBlank()) t.setCategory(r.category().toLowerCase());
        if (r.priority() != null && r.priority() != t.getPriority()) {
            t.setPriority(r.priority());
            sla.findByPriority(r.priority()).ifPresent(s -> t.setDueAt(t.getCreatedAt().plus(s.getResolutionMinutes(), ChronoUnit.MINUTES)));
        }
        audit.log("TICKET_UPDATED", "Ticket", id, null);
        return toResponse(tickets.save(t));
    }

    @Transactional
    public TicketResponse assign(Long id, Long assigneeId) {
        Ticket t = tickets.findById(id).orElseThrow(() -> new NotFoundException("Ticket", id));
        User agent = users.findById(assigneeId).orElseThrow(() -> new NotFoundException("User", assigneeId));
        if (!agent.isActive() || !ASSIGNABLE_ROLES.contains(agent.getRole().getName())) {
            throw new BadRequestException("Tickets can only be assigned to active staff members");
        }
        t.setAssignee(agent);
        Ticket saved = tickets.save(t);
        notifications.notifyUser(agent.getId(), "TICKET_ASSIGNED", "Ticket #" + id + " was assigned to you: " + t.getTitle(), "/tickets/" + id);
        audit.log("TICKET_ASSIGNED", "Ticket", id, "assignee=" + assigneeId);
        return toResponse(saved);
    }

    @Transactional
    public TicketResponse changeStatus(Long id, TicketStatus next) {
        Ticket t = tickets.findById(id).orElseThrow(() -> new NotFoundException("Ticket", id));
        TicketStatus current = t.getStatus();
        if (current == next) return toResponse(t);
        if (!current.canTransitionTo(next)) {
            throw new BadRequestException("Cannot move a ticket from " + current + " to " + next);
        }
        t.setStatus(next);
        if (next == TicketStatus.RESOLVED) t.setResolvedAt(Instant.now());
        else if (current == TicketStatus.RESOLVED && next == TicketStatus.IN_PROGRESS) t.setResolvedAt(null);
        Ticket saved = tickets.save(t);
        if (next == TicketStatus.RESOLVED) {
            notifications.notifyUser(t.getReporter().getId(), "TICKET_RESOLVED", "Your ticket #" + id + " was resolved", "/tickets/" + id);
        }
        audit.log("TICKET_STATUS", "Ticket", id, current + " -> " + next);
        return toResponse(saved);
    }

    @Transactional
    public CommentResponse addComment(Long ticketId, String body, AppUserDetails me) {
        Ticket t = load(ticketId, me);
        TicketComment c = new TicketComment();
        c.setTicket(t);
        c.setAuthor(users.getReferenceById(me.getUserId()));
        c.setBody(body);
        TicketComment saved = comments.save(c);
        audit.log("TICKET_COMMENTED", "Ticket", ticketId, null);
        return new CommentResponse(saved.getId(), me.getUserId(), users.getReferenceById(me.getUserId()).getFullName(), body, saved.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> comments(Long ticketId, AppUserDetails me) {
        load(ticketId, me);
        return comments.findByTicketIdOrderByCreatedAtAsc(ticketId).stream()
            .map(c -> new CommentResponse(c.getId(), c.getAuthor().getId(), c.getAuthor().getFullName(), c.getBody(), c.getCreatedAt())).toList();
    }

    private Ticket load(Long id, AppUserDetails me) {
        Ticket t = tickets.findById(id).orElseThrow(() -> new NotFoundException("Ticket", id));
        if (me.isEmployee() && !t.getReporter().getId().equals(me.getUserId())) {
            throw new AccessDeniedException("Not your ticket");
        }
        return t;
    }

    TicketResponse toResponse(Ticket t) {
        User a = t.getAssignee();
        boolean breached = t.getDueAt() != null &&
            (t.getResolvedAt() != null ? t.getResolvedAt().isAfter(t.getDueAt()) : Instant.now().isAfter(t.getDueAt()));
        return new TicketResponse(t.getId(), t.getTitle(), t.getDescription(), t.getCategory(), t.getPriority(), t.getStatus(),
            t.getReporter().getId(), t.getReporter().getFullName(),
            a == null ? null : a.getId(), a == null ? null : a.getFullName(),
            t.getAsset() == null ? null : t.getAsset().getId(), t.getAsset() == null ? null : t.getAsset().getName(),
            t.getDueAt(), t.getCreatedAt(), t.getUpdatedAt(), t.getResolvedAt(), breached);
    }
}
