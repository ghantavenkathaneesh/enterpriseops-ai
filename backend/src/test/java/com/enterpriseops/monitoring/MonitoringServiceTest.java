package com.enterpriseops.monitoring;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.enterpriseops.ai.AiClient;
import com.enterpriseops.ai.AiPredictionRepository;
import com.enterpriseops.asset.Asset;
import com.enterpriseops.asset.AssetRepository;
import com.enterpriseops.incident.Incident;
import com.enterpriseops.incident.IncidentRepository;
import com.enterpriseops.incident.IncidentService;
import com.enterpriseops.monitoring.MonitoringDtos.MetricRequest;
import com.enterpriseops.monitoring.MonitoringDtos.MetricResult;
import com.enterpriseops.ticket.Priority;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

class MonitoringServiceTest {
    SystemMetricRepository metrics = mock(SystemMetricRepository.class);
    AnomalyEventRepository anomalies = mock(AnomalyEventRepository.class);
    AssetRepository assets = mock(AssetRepository.class);
    IncidentRepository incidents = mock(IncidentRepository.class);
    IncidentService incidentService = mock(IncidentService.class);
    AiPredictionRepository predictions = mock(AiPredictionRepository.class);
    AiClient ai = mock(AiClient.class);
    SseHub sse = mock(SseHub.class);
    TransactionTemplate tx = mock(TransactionTemplate.class);
    MonitoringService service;
    Asset asset;

    private static void setId(Object o, Long id) throws Exception {
        Field f = o.getClass().getDeclaredField("id");
        f.setAccessible(true);
        f.set(o, id);
    }

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws Exception {
        service = new MonitoringService(metrics, anomalies, assets, incidents, incidentService, predictions, ai, sse, tx);
        asset = new Asset();
        asset.setName("Web Server 1"); asset.setAssetType(Asset.Type.SERVER);
        setId(asset, 1L);
        when(assets.findById(1L)).thenReturn(Optional.of(asset));
        when(tx.execute(any())).thenAnswer(inv -> ((TransactionCallback<Object>) inv.getArgument(0)).doInTransaction(null));
        when(metrics.save(any(SystemMetric.class))).thenAnswer(inv -> {
            SystemMetric m = inv.getArgument(0);
            setId(m, 10L);
            return m;
        });
    }

    private static MetricRequest req(double cpu, double ram, double disk, double net) {
        return new MetricRequest(1L, BigDecimal.valueOf(cpu), BigDecimal.valueOf(ram), BigDecimal.valueOf(disk), BigDecimal.valueOf(net), true);
    }

    @Test
    void normalReadingCreatesNoIncident() {
        when(ai.detectAnomaly(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
            .thenReturn(Optional.of(new AiClient.AnomalyResult(false, 0.1, "v1")));
        MetricResult r = service.ingest(req(40, 55, 60, 120));
        assertFalse(r.anomaly());
        verifyNoInteractions(incidentService);
        verify(anomalies, never()).save(any());
    }

    @Test
    void aiOutageStillRaisesThresholdIncident() throws Exception {
        when(ai.detectAnomaly(anyDouble(), anyDouble(), anyDouble(), anyDouble())).thenReturn(Optional.empty());
        when(incidents.findFirstByAssetIdAndSourceAndStatusIn(eq(1L), any(), any(Collection.class))).thenReturn(Optional.empty());
        Incident created = new Incident();
        setId(created, 77L);
        when(incidentService.createFromMonitoring(any(), any(), any(), any())).thenReturn(created);

        MetricResult r = service.ingest(req(97, 50, 60, 100));

        assertTrue(r.anomaly());
        assertEquals("THRESHOLD", r.detectionSource());
        assertFalse(r.aiAvailable());
        assertTrue(r.incidentCreated());
        assertEquals(77L, r.incidentId());
    }

    @Test
    void repeatedAnomalyReusesActiveIncident() throws Exception {
        when(ai.detectAnomaly(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
            .thenReturn(Optional.of(new AiClient.AnomalyResult(true, -0.2, "v1")));
        Incident existing = new Incident();
        setId(existing, 5L);
        when(incidents.findFirstByAssetIdAndSourceAndStatusIn(eq(1L), any(), any(Collection.class))).thenReturn(Optional.of(existing));

        MetricResult r = service.ingest(req(95, 95, 60, 400));

        assertFalse(r.incidentCreated());
        assertEquals(5L, r.incidentId());
        verify(incidentService, never()).createFromMonitoring(any(), any(), any(), any());
    }

    @Test
    void severityScalesWithNumberOfBreaches() {
        assertEquals(Priority.CRITICAL, MonitoringService.severity(3, false, req(99, 99, 99, 100)));
        assertEquals(Priority.HIGH, MonitoringService.severity(1, false, req(96, 50, 50, 100)));
        assertEquals(Priority.MEDIUM, MonitoringService.severity(1, false, req(91, 50, 50, 100)));
    }

    @Test
    void thresholdReasonsListEachBreach() {
        List<String> reasons = MonitoringService.thresholdReasons(req(91, 92, 10, 600));
        assertEquals(3, reasons.size());
    }
}
