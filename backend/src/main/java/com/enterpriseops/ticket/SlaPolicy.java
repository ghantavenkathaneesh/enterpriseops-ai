package com.enterpriseops.ticket;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity @Table(name = "sla_policies") @Getter @NoArgsConstructor
public class SlaPolicy {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING) @Column(nullable = false, unique = true)
    private Priority priority;
    @Column(name = "response_minutes", nullable = false) private int responseMinutes;
    @Column(name = "resolution_minutes", nullable = false) private int resolutionMinutes;
}
