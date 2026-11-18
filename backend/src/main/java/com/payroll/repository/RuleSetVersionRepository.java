package com.payroll.repository;

import com.payroll.entity.RuleSetVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface RuleSetVersionRepository extends JpaRepository<RuleSetVersion, UUID> {
    Optional<RuleSetVersion> findFirstByJurisdictionCodeAndStatusOrderByEffectiveFromDesc(String jurisdictionCode, String status);
}
