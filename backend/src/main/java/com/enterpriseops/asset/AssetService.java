package com.enterpriseops.asset;

import com.enterpriseops.asset.AssetDtos.*;
import com.enterpriseops.audit.AuditService;
import com.enterpriseops.common.BadRequestException;
import com.enterpriseops.common.NotFoundException;
import com.enterpriseops.common.PageResponse;
import com.enterpriseops.user.UserRepository;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssetService {
    private final AssetRepository assets;
    private final UserRepository users;
    private final AuditService audit;

    public AssetService(AssetRepository assets, UserRepository users, AuditService audit) {
        this.assets = assets; this.users = users; this.audit = audit;
    }

    @Transactional(readOnly = true)
    public PageResponse<AssetResponse> search(String q, Asset.Type type, Asset.Status status, Pageable pageable) {
        Specification<Asset> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (q != null && !q.isBlank()) {
                String like = "%" + q.toLowerCase() + "%";
                ps.add(cb.or(cb.like(cb.lower(root.get("name")), like), cb.like(cb.lower(root.get("assetTag")), like)));
            }
            if (type != null) ps.add(cb.equal(root.get("assetType"), type));
            if (status != null) ps.add(cb.equal(root.get("status"), status));
            return cb.and(ps.toArray(new Predicate[0]));
        };
        return PageResponse.of(assets.findAll(spec, pageable), AssetResponse::from);
    }

    @Transactional(readOnly = true)
    public AssetResponse get(Long id) { return AssetResponse.from(find(id)); }

    @Transactional
    public AssetResponse create(AssetRequest r) {
        if (assets.existsByAssetTagIgnoreCase(r.assetTag())) throw new BadRequestException("Asset tag already exists");
        Asset a = new Asset();
        apply(a, r);
        Asset saved = assets.save(a);
        audit.log("ASSET_CREATED", "Asset", saved.getId(), r.assetTag());
        return AssetResponse.from(saved);
    }

    @Transactional
    public AssetResponse update(Long id, AssetRequest r) {
        Asset a = find(id);
        apply(a, r);
        audit.log("ASSET_UPDATED", "Asset", id, null);
        return AssetResponse.from(assets.save(a));
    }

    private void apply(Asset a, AssetRequest r) {
        a.setAssetTag(r.assetTag());
        a.setName(r.name());
        a.setAssetType(r.assetType());
        if (r.status() != null) a.setStatus(r.status());
        a.setLocation(r.location());
        a.setOwner(r.ownerId() == null ? null
            : users.findById(r.ownerId()).orElseThrow(() -> new NotFoundException("User", r.ownerId())));
    }

    private Asset find(Long id) { return assets.findById(id).orElseThrow(() -> new NotFoundException("Asset", id)); }
}
