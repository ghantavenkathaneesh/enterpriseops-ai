package com.enterpriseops.monitoring;

import com.enterpriseops.ai.AiClient;
import com.enterpriseops.ai.AiPrediction;
import com.enterpriseops.ai.AiPredictionRepository;
import com.enterpriseops.asset.Asset;
import com.enterpriseops.asset.AssetRepository;
import com.enterpriseops.common.NotFoundException;
import com.enterpriseops.incident.Incident;
import com.enterpriseops.incident.IncidentRepository;
import com.enterpriseops.incident.IncidentService;
import com.enterpriseops.incident.IncidentStatus;
import com.enterpriseops.monitoring.MonitoringDtos.*;
import com.enterpriseops.ticket.Priority;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Pipeline: metric in -> (AI anomaly check, outside any DB transaction) -> persist metric + anomaly + incident
 * in one transaction -> push to dashboards over SSE. If the AI service is down, threshold rules still apply.
 */
@Service
public class MonitoringService {
    private static final Logger log = LoggerFactory.getLogger(MonitoringService.class);
    static final double CPU_LIMIT = 90, RAM_LIMIT = 90, DISK_LIMIT = 90, NETWORK_LIMIT = 500;

    private final SystemMetricRepository metrics;
    private final AnomalyEventRepository anomalies;
    private final AssetRepository assets;
    private final IncidentRepository incidents;
    private final IncidentService incidentService;
    private final AiPredictionRepository predictions;
    private final AiClient ai;
    private final SseHub sse;
    private final TransactionTemplate tx;

    public MonitoringService(SystemMetricRepository metrics, AnomalyEventRepository anomalies, AssetRepository assets,
                             IncidentRepository incidents, IncidentService incidentService, AiPredictionRepository predictions,
                             AiClient ai, SseHub sse, TransactionTemplate tx) {
        this.metrics = metrics; this.anomalies = anomalies; this.assets = assets; this.incidents = incidents;
        this.incidentService = incidentService; this.predictions = predictions; this.ai = ai; this.sse = sse; this.tx = tx;
    }

    public MetricResult ingest(MetricRequest r) {
        Asset asset = assets.findById(r.assetId()).orElseThrow(() -> new NotFoundException("Asset", r.assetId()));
        Optional<AiClient.AnomalyResult> ml = ai.detectAnomaly(r.cpu().doubleValue(), r.ram().doubleValue(),
            r.disk().doubleValue(), r.networkMbps().doubleValue());
        MetricResult result = tx.execute(status -> persist(r, asset, ml));
        sse.publish("metric", result);
        if (result.incidentCreated()) sse.publish("incident", Map.of("incidentId", result.incidentId(), "assetId", asset.getId(), "assetName", asset.getName()));
        return result;
    }

    private MetricResult persist(MetricRequest r, Asset asset, Optional<AiClient.AnomalyResult> ml) {
        SystemMetric m = new SystemMetric();
        m.setAssetId(asset.getId());
        m.setCpuPercent(r.cpu()); m.setRamPercent(r.ram()); m.setDiskPercent(r.disk()); m.setNetworkMbps(r.networkMbps());
        m.setSimulated(r.simulated() == null || r.simulated());
        m = metrics.save(m);

        List<String> reasons = thresholdReasons(r);
        boolean mlAnomaly = ml.map(AiClient.AnomalyResult::anomaly).orElse(false);
        Long incidentId = null;
        boolean created = false;
        String source = null;

        if (!reasons.isEmpty() || mlAnomaly) {
            source = mlAnomaly ? "ML" : "THRESHOLD";
            if (mlAnomaly) reasons.add("ML model flagged an unusual combination of readings");
            AnomalyEvent ev = new AnomalyEvent();
            ev.setMetricId(m.getId()); ev.setAssetId(asset.getId()); ev.setDetectionSource(source);
            ev.setReason(String.join("; ", reasons));
            ml.ifPresent(a -> ev.setScore(BigDecimal.valueOf(a.score()).setScale(4, RoundingMode.HALF_UP)));
            ml.filter(AiClient.AnomalyResult::anomaly)
              .ifPresent(a -> predictions.save(AiPrediction.anomaly("anomaly", a.score(), a.modelVersion())));

            // Dedupe: reuse the active incident for this asset instead of opening a new one every reading.
            Optional<Incident> existing = incidents.findFirstByAssetIdAndSourceAndStatusIn(asset.getId(),
                Incident.Source.MONITORING, List.of(IncidentStatus.OPEN, IncidentStatus.INVESTIGATING));
            if (existing.isPresent()) {
                incidentId = existing.get().getId();
            } else {
                Priority sev = severity(reasons.size(), mlAnomaly, r);
                String simNote = m.isSimulated() ? " [SIMULATED DATA]" : "";
                Incident inc = incidentService.createFromMonitoring(asset, sev,
                    "Abnormal metrics on " + asset.getName() + simNote,
                    "Detected by " + source + ". " + String.join("; ", reasons) + ". Readings: cpu=" + r.cpu() + "%, ram=" + r.ram()
                        + "%, disk=" + r.disk() + "%, network=" + r.networkMbps() + " Mbps.");
                incidentId = inc.getId();
                created = true;
            }
            ev.setIncidentId(incidentId);
            anomalies.save(ev);
        }
        return new MetricResult(m.getId(), asset.getId(), r.cpu(), r.ram(), r.disk(), r.networkMbps(), m.isSimulated(),
            m.getRecordedAt(), source != null, source, reasons, incidentId, created, ml.isPresent());
    }

    static List<String> thresholdReasons(MetricRequest r) {
        List<String> out = new ArrayList<>();
        if (r.cpu().doubleValue() >= CPU_LIMIT) out.add("CPU at or above " + (int) CPU_LIMIT + "%");
        if (r.ram().doubleValue() >= RAM_LIMIT) out.add("RAM at or above " + (int) RAM_LIMIT + "%");
        if (r.disk().doubleValue() >= DISK_LIMIT) out.add("Disk at or above " + (int) DISK_LIMIT + "%");
        if (r.networkMbps().doubleValue() >= NETWORK_LIMIT) out.add("Network traffic at or above " + (int) NETWORK_LIMIT + " Mbps");
        return out;
    }

    static Priority severity(int reasonCount, boolean ml, MetricRequest r) {
        if (reasonCount >= 3) return Priority.CRITICAL;
        double worst = Math.max(r.cpu().doubleValue(), Math.max(r.ram().doubleValue(), r.disk().doubleValue()));
        if (reasonCount >= 2 || worst >= 95 || (ml && reasonCount >= 1)) return Priority.HIGH;
        return Priority.MEDIUM;
    }

    public List<MetricPoint> history(Long assetId, int limit) {
        return metrics.findByAssetIdOrderByRecordedAtDesc(assetId, PageRequest.of(0, Math.min(Math.max(limit, 1), 500)))
            .stream().map(MetricPoint::from).toList();
    }

    public List<Map<String, Object>> latestPerAsset() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Asset a : assets.findAll()) {
            metrics.findFirstByAssetIdOrderByRecordedAtDesc(a.getId()).ifPresent(m ->
                out.add(Map.of("assetId", a.getId(), "assetName", a.getName(), "assetType", a.getAssetType().name(), "latest", MetricPoint.from(m))));
        }
        return out;
    }

    public List<AnomalyEvent> recentAnomalies(int limit) {
        return anomalies.findAllByOrderByCreatedAtDesc(PageRequest.of(0, Math.min(Math.max(limit, 1), 200)));
    }
}
