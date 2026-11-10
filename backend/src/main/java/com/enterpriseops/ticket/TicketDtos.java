package com.enterpriseops.ticket;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class TicketDtos {
    private TicketDtos() {}

    public record CreateTicketRequest(@NotBlank @Size(max = 200) String title, @NotBlank @Size(max = 5000) String description,
                                      String category, Priority priority, Long assetId) {}
    public record UpdateTicketRequest(@Size(max = 200) String title, @Size(max = 5000) String description,
                                      String category, Priority priority) {}
    public record AssignRequest(@NotNull Long assigneeId) {}
    public record StatusRequest(@NotNull TicketStatus status) {}
    public record CommentRequest(@NotBlank @Size(max = 4000) String body) {}
    public record CommentResponse(Long id, Long authorId, String authorName, String body, Instant createdAt) {}
    public record TicketResponse(Long id, String title, String description, String category, Priority priority,
                                 TicketStatus status, Long reporterId, String reporterName, Long assigneeId,
                                 String assigneeName, Long assetId, String assetName, Instant dueAt,
                                 Instant createdAt, Instant updatedAt, Instant resolvedAt, boolean slaBreached) {}
}
