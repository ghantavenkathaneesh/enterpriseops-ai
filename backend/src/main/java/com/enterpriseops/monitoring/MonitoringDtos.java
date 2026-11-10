package com.enterpriseops.monitoring;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class MonitoringDtos {
    private MonitoringDtos() {}

    public record MetricRequest(@NotNull Long assetId,
                                @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal cpu,
                                @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal ram,
                                @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal disk,
                                @NotNull @DecimalMin("0") BigDecimal networkMbps,
                                Boolean simulated) {}

    public record MetricResult(Long metricId, Long assetId, BigDecimal cpu, BigDecimal ram, BigDecimal disk,
                               BigDecimal networkMbps, boolean simulated, Instant recordedAt, boolean anomaly,
                               String detectionSource, List<String> reasons, Long incidentId, boolean incidentCreated,
                               boolean aiAvailable) {}

    public record MetricPoint(Long id, BigDecimal cpu, BigDecimal ram, BigDecimal disk, BigDecimal networkMbps,
                              boolean simulated, Instant recordedAt) {
        public static MetricPoint from(SystemMetric m) {
            return new MetricPoint(m.getId(), m.getCpuPercent(), m.getRamPercent(), m.getDiskPercent(), m.getNetworkMbps(),
                m.isSimulated(), m.getRecordedAt());
        }
    }
}
