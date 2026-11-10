package com.enterpriseops.incident;

import com.enterpriseops.asset.Asset;
import com.enterpriseops.ticket.Priority;
import com.enterpriseops.ticket.Ticket;
import com.enterpriseops.user.User;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity @Table(name = "incidents") @Getter @Setter @NoArgsConstructor
public class Incident {
    public enum Source { MANUAL, MONITORING }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private String title;
    @Column(nullable = false, columnDefinition = "text") private String description;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Priority severity;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private IncidentStatus status = IncidentStatus.OPEN;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Source source = Source.MANUAL;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "assignee_id") private User assignee;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "ticket_id") private Ticket ticket;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "asset_id") private Asset asset;
    @Column(name = "resolution_notes", columnDefinition = "text") private String resolutionNotes;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Column(name = "resolved_at") private Instant resolvedAt;

    @PrePersist void onCreate() { Instant n = Instant.now(); if (createdAt == null) createdAt = n; updatedAt = n; }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }
}
