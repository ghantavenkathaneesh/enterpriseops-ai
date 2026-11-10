package com.enterpriseops.incident;

import java.util.Collection;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface IncidentRepository extends JpaRepository<Incident, Long>, JpaSpecificationExecutor<Incident> {
    Optional<Incident> findFirstByAssetIdAndSourceAndStatusIn(Long assetId, Incident.Source source, Collection<IncidentStatus> statuses);
}
