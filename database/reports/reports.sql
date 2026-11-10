-- PostgreSQL reporting queries used (via JdbcTemplate) by the analytics module.

-- 1. Tickets by status / priority / category
SELECT status,   COUNT(*) AS total FROM tickets GROUP BY status   ORDER BY total DESC;
SELECT priority, COUNT(*) AS total FROM tickets GROUP BY priority ORDER BY total DESC;
SELECT category, COUNT(*) AS total FROM tickets GROUP BY category ORDER BY total DESC;

-- 2. Average resolution time (hours) for resolved tickets
SELECT ROUND(AVG(EXTRACT(EPOCH FROM (resolved_at - created_at)) / 3600)::numeric, 2) AS avg_hours
FROM tickets WHERE resolved_at IS NOT NULL;

-- 3. SLA compliance: share of resolved tickets resolved on or before due_at
SELECT COUNT(*) FILTER (WHERE resolved_at <= due_at) AS within_sla,
       COUNT(*) AS resolved_total
FROM tickets WHERE resolved_at IS NOT NULL AND due_at IS NOT NULL;

-- 4. Agent workload (open work per assignee)
SELECT u.full_name, COUNT(*) AS active_tickets
FROM tickets t JOIN users u ON u.id = t.assignee_id
WHERE t.status IN ('OPEN','IN_PROGRESS','ON_HOLD')
GROUP BY u.full_name ORDER BY active_tickets DESC;

-- 5. Daily and monthly ticket trends
SELECT date_trunc('day',   created_at)::date AS period, COUNT(*) FROM tickets GROUP BY 1 ORDER BY 1;
SELECT date_trunc('month', created_at)::date AS period, COUNT(*) FROM tickets GROUP BY 1 ORDER BY 1;

-- 6. Incident severity
SELECT severity, COUNT(*) AS total FROM incidents GROUP BY severity;

-- 7. Asset health: assets with an active monitoring incident vs healthy
SELECT a.status, COUNT(*) AS total,
       COUNT(*) FILTER (WHERE EXISTS (SELECT 1 FROM incidents i WHERE i.asset_id = a.id
                                      AND i.status IN ('OPEN','INVESTIGATING'))) AS with_active_incident
FROM assets a GROUP BY a.status;

-- 8. AI prediction statistics
SELECT kind, predicted_label, COUNT(*) AS total, ROUND(AVG(confidence)::numeric, 4) AS avg_confidence
FROM ai_predictions GROUP BY kind, predicted_label ORDER BY kind, total DESC;
