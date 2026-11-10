package com.enterpriseops.monitoring;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity @Table(name = "anomaly_events") @Getter @Setter @NoArgsConstructor
public class AnomalyEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "metric_id", nullable = false) private Long metricId;
    @Column(name = "asset_id", nullable = false) private Long assetId;
    @Column(name = "detection_source", nullable = false) private String detectionSource;
    @Column(precision = 8, scale = 4) private BigDecimal score;
    @Column(nullable = false) private String reason;
    @Column(name = "incident_id") private Long incidentId;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    @PrePersist void onCreate() { if (createdAt == null) createdAt = Instant.now(); }
}
