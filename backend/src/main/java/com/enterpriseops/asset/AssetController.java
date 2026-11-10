package com.enterpriseops.asset;

import com.enterpriseops.asset.AssetDtos.*;
import com.enterpriseops.common.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/assets")
public class AssetController {
    private final AssetService service;

    public AssetController(AssetService service) { this.service = service; }

    @GetMapping
    public PageResponse<AssetResponse> search(@RequestParam(required = false) String q,
                                              @RequestParam(required = false) Asset.Type type,
                                              @RequestParam(required = false) Asset.Status status,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        return service.search(q, type, status, PageRequest.of(page, Math.min(size, 100), Sort.by("name")));
    }

    @GetMapping("/{id}")
    public AssetResponse get(@PathVariable Long id) { return service.get(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public AssetResponse create(@Valid @RequestBody AssetRequest r) { return service.create(r); }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public AssetResponse update(@PathVariable Long id, @Valid @RequestBody AssetRequest r) { return service.update(id, r); }
}
