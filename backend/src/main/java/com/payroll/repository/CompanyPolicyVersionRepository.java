package com.payroll.repository;

import com.payroll.entity.CompanyPolicyVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface CompanyPolicyVersionRepository extends JpaRepository<CompanyPolicyVersion, UUID> {
    Optional<CompanyPolicyVersion> findFirstByCompanyIdAndStatusOrderByEffectiveFromDesc(UUID companyId, String status);
}
