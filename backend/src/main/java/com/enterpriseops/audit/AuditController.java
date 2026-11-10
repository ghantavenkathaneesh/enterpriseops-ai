package com.enterpriseops.audit;

import com.enterpriseops.common.PageResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/audit-logs")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class AuditController {
    private final AuditLogRepository repo;

    public AuditController(AuditLogRepository repo) { this.repo = repo; }

    @GetMapping
    public PageResponse<AuditLog> list(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {
        return PageResponse.of(repo.findAll(PageRequest.of(page, Math.min(size, 200), Sort.by(Sort.Direction.DESC, "createdAt"))), a -> a);
    }
}
