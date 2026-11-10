package com.enterpriseops.monitoring;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity @Table(name = "system_metrics") @Getter @Setter @NoArgsConstructor
public class SystemMetric {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "asset_id", nullable = false) private Long assetId;
    @Column(name = "cpu_percent", nullable = false, precision = 5, scale = 2) private BigDecimal cpuPercent;
    @Column(name = "ram_percent", nullable = false, precision = 5, scale = 2) private BigDecimal ramPercent;
    @Column(name = "disk_percent", nullable = false, precision = 5, scale = 2) private BigDecimal diskPercent;
    @Column(name = "network_mbps", nullable = false, precision = 10, scale = 2) private BigDecimal networkMbps;
    @Column(nullable = false) private boolean simulated = true;
    @Column(name = "recorded_at", nullable = false) private Instant recordedAt;

    @PrePersist void onCreate() { if (recordedAt == null) recordedAt = Instant.now(); }
}
