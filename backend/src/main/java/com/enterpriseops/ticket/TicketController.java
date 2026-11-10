package com.enterpriseops.ticket;

import com.enterpriseops.common.PageResponse;
import com.enterpriseops.security.CurrentUser;
import com.enterpriseops.ticket.TicketDtos.*;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tickets")
public class TicketController {
    private static final String STAFF = "hasAnyRole('ADMIN','MANAGER','SUPPORT_AGENT')";
    private final TicketService service;

    public TicketController(TicketService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TicketResponse create(@Valid @RequestBody CreateTicketRequest r) { return service.create(r, CurrentUser.get()); }

    @GetMapping
    public PageResponse<TicketResponse> search(@RequestParam(required = false) TicketStatus status,
                                               @RequestParam(required = false) Priority priority,
                                               @RequestParam(required = false) String category,
                                               @RequestParam(required = false) Long assigneeId,
                                               @RequestParam(required = false) String q,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        return service.search(status, priority, category, assigneeId, q,
            PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "createdAt")), CurrentUser.get());
    }

    @GetMapping("/{id}")
    public TicketResponse get(@PathVariable Long id) { return service.get(id, CurrentUser.get()); }

    @PutMapping("/{id}")
    @PreAuthorize(STAFF)
    public TicketResponse update(@PathVariable Long id, @Valid @RequestBody UpdateTicketRequest r) { return service.update(id, r); }

    @PatchMapping("/{id}/assign")
    @PreAuthorize(STAFF)
    public TicketResponse assign(@PathVariable Long id, @Valid @RequestBody AssignRequest r) { return service.assign(id, r.assigneeId()); }

    @PatchMapping("/{id}/status")
    @PreAuthorize(STAFF)
    public TicketResponse status(@PathVariable Long id, @Valid @RequestBody StatusRequest r) { return service.changeStatus(id, r.status()); }

    @GetMapping("/{id}/comments")
    public List<CommentResponse> comments(@PathVariable Long id) { return service.comments(id, CurrentUser.get()); }

    @PostMapping("/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse comment(@PathVariable Long id, @Valid @RequestBody CommentRequest r) {
        return service.addComment(id, r.body(), CurrentUser.get());
    }
}
