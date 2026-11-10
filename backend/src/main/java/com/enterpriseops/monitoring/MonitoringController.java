package com.enterpriseops.monitoring;

import com.enterpriseops.monitoring.MonitoringDtos.*;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/monitoring")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER','SUPPORT_AGENT')")
public class MonitoringController {
    private final MonitoringService service;
    private final SseHub hub;

    public MonitoringController(MonitoringService service, SseHub hub) { this.service = service; this.hub = hub; }

    @PostMapping("/metrics")
    @ResponseStatus(HttpStatus.CREATED)
    public MetricResult ingest(@Valid @RequestBody MetricRequest r) { return service.ingest(r); }

    @GetMapping("/assets/{assetId}/metrics")
    public List<MetricPoint> history(@PathVariable Long assetId, @RequestParam(defaultValue = "100") int limit) {
        return service.history(assetId, limit);
    }

    @GetMapping("/latest")
    public List<Map<String, Object>> latest() { return service.latestPerAsset(); }

    @GetMapping("/anomalies")
    public List<AnomalyEvent> anomalies(@RequestParam(defaultValue = "50") int limit) { return service.recentAnomalies(limit); }

    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() { return hub.subscribe(); }
}
