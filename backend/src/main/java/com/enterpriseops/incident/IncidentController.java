package com.enterpriseops.incident;

import com.enterpriseops.common.PageResponse;
import com.enterpriseops.incident.IncidentDtos.*;
import com.enterpriseops.ticket.Priority;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/incidents")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER','SUPPORT_AGENT')")
public class IncidentController {
    private final IncidentService service;

    public IncidentController(IncidentService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IncidentResponse create(@Valid @RequestBody CreateIncidentRequest r) { return service.create(r); }

    @GetMapping
    public PageResponse<IncidentResponse> search(@RequestParam(required = false) IncidentStatus status,
                                                 @RequestParam(required = false) Priority severity,
                                                 @RequestParam(required = false) Long assetId,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "20") int size) {
        return service.search(status, severity, assetId, PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @GetMapping("/{id}")
    public IncidentResponse get(@PathVariable Long id) { return service.get(id); }

    @PatchMapping("/{id}/assign")
    public IncidentResponse assign(@PathVariable Long id, @Valid @RequestBody IncidentAssignRequest r) { return service.assign(id, r.assigneeId()); }

    @PatchMapping("/{id}/status")
    public IncidentResponse status(@PathVariable Long id, @Valid @RequestBody IncidentStatusRequest r) {
        return service.changeStatus(id, r.status(), r.resolutionNotes());
    }
}
