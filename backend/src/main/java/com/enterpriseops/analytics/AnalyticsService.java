package com.enterpriseops.analytics;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/** All figures come from SQL against the live database. Nothing here is hard-coded. */
@Service
public class AnalyticsService {
    private final JdbcTemplate jdbc;

    public AnalyticsService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public Map<String, Object> summary() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("totalTickets", jdbc.queryForObject("SELECT COUNT(*) FROM tickets", Long.class));
        m.put("openTickets", jdbc.queryForObject("SELECT COUNT(*) FROM tickets WHERE status IN ('OPEN','IN_PROGRESS','ON_HOLD')", Long.class));
        m.put("resolvedTickets", jdbc.queryForObject("SELECT COUNT(*) FROM tickets WHERE status IN ('RESOLVED','CLOSED')", Long.class));
        m.put("activeIncidents", jdbc.queryForObject("SELECT COUNT(*) FROM incidents WHERE status IN ('OPEN','INVESTIGATING')", Long.class));
        m.put("avgResolutionHours", avgResolutionHours());
        m.put("slaCompliancePercent", slaCompliance().get("compliancePercent"));
        m.put("anomaliesLast24h", jdbc.queryForObject("SELECT COUNT(*) FROM anomaly_events WHERE created_at >= now() - interval '24 hours'", Long.class));
        return m;
    }

    public List<Map<String, Object>> ticketsByStatus() { return jdbc.queryForList("SELECT status AS label, COUNT(*) AS total FROM tickets GROUP BY status ORDER BY total DESC"); }
    public List<Map<String, Object>> ticketsByPriority() { return jdbc.queryForList("SELECT priority AS label, COUNT(*) AS total FROM tickets GROUP BY priority ORDER BY total DESC"); }
    public List<Map<String, Object>> ticketsByCategory() { return jdbc.queryForList("SELECT category AS label, COUNT(*) AS total FROM tickets GROUP BY category ORDER BY total DESC"); }

    public Double avgResolutionHours() {
        Double v = jdbc.queryForObject("SELECT AVG(EXTRACT(EPOCH FROM (resolved_at - created_at)) / 3600.0) FROM tickets WHERE resolved_at IS NOT NULL", Double.class);
        return v == null ? null : Math.round(v * 100.0) / 100.0;
    }

    public Map<String, Object> slaCompliance() {
        Map<String, Object> row = jdbc.queryForMap("SELECT COUNT(*) FILTER (WHERE resolved_at <= due_at) AS within_sla, COUNT(*) AS total "
            + "FROM tickets WHERE resolved_at IS NOT NULL AND due_at IS NOT NULL");
        long within = ((Number) row.get("within_sla")).longValue();
        long total = ((Number) row.get("total")).longValue();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("withinSla", within);
        out.put("resolvedTotal", total);
        out.put("compliancePercent", total == 0 ? null : Math.round(within * 10000.0 / total) / 100.0);
        return out;
    }

    public List<Map<String, Object>> agentWorkload() {
        return jdbc.queryForList("SELECT u.full_name AS label, COUNT(*) AS total FROM tickets t JOIN users u ON u.id = t.assignee_id "
            + "WHERE t.status IN ('OPEN','IN_PROGRESS','ON_HOLD') GROUP BY u.full_name ORDER BY total DESC");
    }

    public List<Map<String, Object>> trend(String granularity) {
        String unit = "month".equalsIgnoreCase(granularity) ? "month" : "day";   // whitelist: never concatenate user input
        String fmt = unit.equals("month") ? "YYYY-MM" : "YYYY-MM-DD";
        return jdbc.queryForList("SELECT to_char(date_trunc('" + unit + "', created_at AT TIME ZONE 'UTC'), '" + fmt + "') AS label, COUNT(*) AS total "
            + "FROM tickets GROUP BY 1 ORDER BY 1");
    }

    public List<Map<String, Object>> incidentSeverity() { return jdbc.queryForList("SELECT severity AS label, COUNT(*) AS total FROM incidents GROUP BY severity ORDER BY total DESC"); }

    public List<Map<String, Object>> assetHealth() {
        return jdbc.queryForList("SELECT a.id, a.name, a.status, COUNT(i.id) FILTER (WHERE i.status IN ('OPEN','INVESTIGATING')) AS active_incidents "
            + "FROM assets a LEFT JOIN incidents i ON i.asset_id = a.id GROUP BY a.id, a.name, a.status ORDER BY active_incidents DESC, a.name");
    }

    public List<Map<String, Object>> aiStats() {
        return jdbc.queryForList("SELECT kind, predicted_label AS label, COUNT(*) AS total, ROUND(AVG(confidence)::numeric, 4) AS avg_confidence "
            + "FROM ai_predictions GROUP BY kind, predicted_label ORDER BY kind, total DESC");
    }

    public String ticketsCsv() {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT t.id, t.title, t.category, t.priority, t.status, r.full_name AS reporter, "
            + "a.full_name AS assignee, t.created_at, t.resolved_at FROM tickets t JOIN users r ON r.id = t.reporter_id "
            + "LEFT JOIN users a ON a.id = t.assignee_id ORDER BY t.id");
        return toCsv(List.of("id", "title", "category", "priority", "status", "reporter", "assignee", "created_at", "resolved_at"), rows);
    }

    public String incidentsCsv() {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT i.id, i.title, i.severity, i.status, i.source, a.name AS asset, i.created_at, i.resolved_at "
            + "FROM incidents i LEFT JOIN assets a ON a.id = i.asset_id ORDER BY i.id");
        return toCsv(List.of("id", "title", "severity", "status", "source", "asset", "created_at", "resolved_at"), rows);
    }

    static String toCsv(List<String> columns, List<Map<String, Object>> rows) {
        StringBuilder sb = new StringBuilder(String.join(",", columns)).append("\r\n");
        for (Map<String, Object> row : rows) {
            for (int i = 0; i < columns.size(); i++) {
                if (i > 0) sb.append(',');
                sb.append(csvCell(row.get(columns.get(i))));
            }
            sb.append("\r\n");
        }
        return sb.toString();
    }

    static String csvCell(Object value) {
        if (value == null) return "";
        String s = value.toString();
        if (!s.isEmpty() && "=+-@".indexOf(s.charAt(0)) >= 0) s = "'" + s;   // block spreadsheet formula injection
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) s = "\"" + s.replace("\"", "\"\"") + "\"";
        return s;
    }
}
