package com.enterpriseops.asset;

import com.enterpriseops.user.User;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity @Table(name = "assets") @Getter @Setter @NoArgsConstructor
public class Asset {
    public enum Type { SERVER, LAPTOP, NETWORK, DATABASE, OTHER }
    public enum Status { ACTIVE, MAINTENANCE, RETIRED }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "asset_tag", nullable = false, unique = true) private String assetTag;
    @Column(nullable = false) private String name;
    @Enumerated(EnumType.STRING) @Column(name = "asset_type", nullable = false) private Type assetType;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Status status = Status.ACTIVE;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "owner_id") private User owner;
    private String location;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    @PrePersist void onCreate() { if (createdAt == null) createdAt = Instant.now(); }
}
