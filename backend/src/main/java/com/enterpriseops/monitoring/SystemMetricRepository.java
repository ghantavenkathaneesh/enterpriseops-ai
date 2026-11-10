package com.enterpriseops.monitoring;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemMetricRepository extends JpaRepository<SystemMetric, Long> {
    List<SystemMetric> findByAssetIdOrderByRecordedAtDesc(Long assetId, Pageable pageable);
    Optional<SystemMetric> findFirstByAssetIdOrderByRecordedAtDesc(Long assetId);
}
