package com.enterpriseops.asset;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class AssetDtos {
    private AssetDtos() {}

    public record AssetRequest(@NotBlank @Size(max = 40) String assetTag, @NotBlank @Size(max = 150) String name,
                               @NotNull Asset.Type assetType, Asset.Status status, Long ownerId, @Size(max = 100) String location) {}
    public record AssetResponse(Long id, String assetTag, String name, Asset.Type assetType, Asset.Status status,
                                Long ownerId, String ownerName, String location) {
        public static AssetResponse from(Asset a) {
            return new AssetResponse(a.getId(), a.getAssetTag(), a.getName(), a.getAssetType(), a.getStatus(),
                a.getOwner() == null ? null : a.getOwner().getId(), a.getOwner() == null ? null : a.getOwner().getFullName(), a.getLocation());
        }
    }
}
