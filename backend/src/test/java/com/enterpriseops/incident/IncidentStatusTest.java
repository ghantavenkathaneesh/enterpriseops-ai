package com.enterpriseops.incident;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class IncidentStatusTest {
    @Test void openCanBeInvestigated() { assertTrue(IncidentStatus.OPEN.canTransitionTo(IncidentStatus.INVESTIGATING)); }
    @Test void openCannotBeClosed() { assertFalse(IncidentStatus.OPEN.canTransitionTo(IncidentStatus.CLOSED)); }
    @Test void activeStatuses() {
        assertTrue(IncidentStatus.OPEN.isActive());
        assertTrue(IncidentStatus.INVESTIGATING.isActive());
        assertFalse(IncidentStatus.RESOLVED.isActive());
    }
}
