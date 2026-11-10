package com.enterpriseops.analytics;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/analytics")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER','SUPPORT_AGENT')")
public class AnalyticsController {
    private final AnalyticsService s;

    public AnalyticsController(AnalyticsService s) { this.s = s; }

    @GetMapping("/summary") public Map<String, Object> summary() { return s.summary(); }
    @GetMapping("/tickets/by-status") public List<Map<String, Object>> byStatus() { return s.ticketsByStatus(); }
    @GetMapping("/tickets/by-priority") public List<Map<String, Object>> byPriority() { return s.ticketsByPriority(); }
    @GetMapping("/tickets/by-category") public List<Map<String, Object>> byCategory() { return s.ticketsByCategory(); }
    @GetMapping("/tickets/trend") public List<Map<String, Object>> trend(@RequestParam(defaultValue = "day") String granularity) { return s.trend(granularity); }
    @GetMapping("/tickets/sla") public Map<String, Object> sla() { return s.slaCompliance(); }
    @GetMapping("/agents/workload") public List<Map<String, Object>> workload() { return s.agentWorkload(); }
    @GetMapping("/incidents/severity") public List<Map<String, Object>> severity() { return s.incidentSeverity(); }
    @GetMapping("/assets/health") public List<Map<String, Object>> assetHealth() { return s.assetHealth(); }
    @GetMapping("/ai/stats") public List<Map<String, Object>> aiStats() { return s.aiStats(); }

    @GetMapping("/export/tickets.csv")
    public ResponseEntity<String> exportTickets() { return csv("tickets.csv", s.ticketsCsv()); }

    @GetMapping("/export/incidents.csv")
    public ResponseEntity<String> exportIncidents() { return csv("incidents.csv", s.incidentsCsv()); }

    private ResponseEntity<String> csv(String name, String body) {
        return ResponseEntity.ok().contentType(new MediaType("text", "csv"))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + name + "\"").body(body);
    }
}
