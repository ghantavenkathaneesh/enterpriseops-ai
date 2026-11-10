package com.enterpriseops.incident;

import java.util.Map;
import java.util.Set;

public enum IncidentStatus {
    OPEN, INVESTIGATING, RESOLVED, CLOSED;

    private static final Map<IncidentStatus, Set<IncidentStatus>> ALLOWED = Map.of(
        OPEN, Set.of(INVESTIGATING, RESOLVED),
        INVESTIGATING, Set.of(RESOLVED, OPEN),
        RESOLVED, Set.of(CLOSED, INVESTIGATING),
        CLOSED, Set.of());

    public boolean canTransitionTo(IncidentStatus next) { return ALLOWED.get(this).contains(next); }
    public boolean isActive() { return this == OPEN || this == INVESTIGATING; }
}
