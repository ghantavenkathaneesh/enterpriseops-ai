package com.enterpriseops.incident;

import com.enterpriseops.ticket.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class IncidentDtos {
    private IncidentDtos() {}

    public record CreateIncidentRequest(@NotBlank @Size(max = 200) String title, @NotBlank @Size(max = 5000) String description,
                                        @NotNull Priority severity, Long ticketId, Long assetId) {}
    public record IncidentStatusRequest(@NotNull IncidentStatus status, @Size(max = 5000) String resolutionNotes) {}
    public record IncidentAssignRequest(@NotNull Long assigneeId) {}
    public record IncidentResponse(Long id, String title, String description, Priority severity, IncidentStatus status,
                                   Incident.Source source, Long assigneeId, String assigneeName, Long ticketId, Long assetId,
                                   String assetName, String resolutionNotes, Instant createdAt, Instant resolvedAt) {
        public static IncidentResponse from(Incident i) {
            return new IncidentResponse(i.getId(), i.getTitle(), i.getDescription(), i.getSeverity(), i.getStatus(), i.getSource(),
                i.getAssignee() == null ? null : i.getAssignee().getId(), i.getAssignee() == null ? null : i.getAssignee().getFullName(),
                i.getTicket() == null ? null : i.getTicket().getId(),
                i.getAsset() == null ? null : i.getAsset().getId(), i.getAsset() == null ? null : i.getAsset().getName(),
                i.getResolutionNotes(), i.getCreatedAt(), i.getResolvedAt());
        }
    }
}
