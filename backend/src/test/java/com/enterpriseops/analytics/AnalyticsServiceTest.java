package com.enterpriseops.analytics;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AnalyticsServiceTest {
    @Test void csvEscapesCommasAndQuotes() { assertEquals("\"a,\"\"b\"\"\"", AnalyticsService.csvCell("a,\"b\"")); }
    @Test void csvBlocksFormulaInjection() { assertEquals("'=SUM(A1)", AnalyticsService.csvCell("=SUM(A1)")); }
    @Test void csvNullIsEmpty() { assertEquals("", AnalyticsService.csvCell(null)); }
    @Test void csvHasHeaderAndRows() {
        String csv = AnalyticsService.toCsv(List.of("id", "title"), List.of(Map.of("id", 1, "title", "VPN down")));
        assertEquals("id,title\r\n1,VPN down\r\n", csv);
    }
}
