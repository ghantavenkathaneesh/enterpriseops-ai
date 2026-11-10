package com.enterpriseops.ticket;

import java.util.Map;
import java.util.Set;

public enum TicketStatus {
    OPEN, IN_PROGRESS, ON_HOLD, RESOLVED, CLOSED;

    private static final Map<TicketStatus, Set<TicketStatus>> ALLOWED = Map.of(
        OPEN, Set.of(IN_PROGRESS, ON_HOLD, RESOLVED),
        IN_PROGRESS, Set.of(ON_HOLD, RESOLVED, OPEN),
        ON_HOLD, Set.of(IN_PROGRESS, OPEN),
        RESOLVED, Set.of(CLOSED, IN_PROGRESS),   // reopen by moving back to IN_PROGRESS
        CLOSED, Set.of());

    public boolean canTransitionTo(TicketStatus next) { return ALLOWED.get(this).contains(next); }
}
