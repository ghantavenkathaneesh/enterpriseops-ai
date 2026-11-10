package com.enterpriseops.ticket;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class TicketStatusTest {
    @Test void openCanStartProgress() { assertTrue(TicketStatus.OPEN.canTransitionTo(TicketStatus.IN_PROGRESS)); }
    @Test void openCannotBeClosedDirectly() { assertFalse(TicketStatus.OPEN.canTransitionTo(TicketStatus.CLOSED)); }
    @Test void resolvedCanBeClosedOrReopened() {
        assertTrue(TicketStatus.RESOLVED.canTransitionTo(TicketStatus.CLOSED));
        assertTrue(TicketStatus.RESOLVED.canTransitionTo(TicketStatus.IN_PROGRESS));
    }
    @Test void closedIsFinal() {
        for (TicketStatus s : TicketStatus.values()) assertFalse(TicketStatus.CLOSED.canTransitionTo(s));
    }
}
