package com.payroll.repository;

import com.payroll.entity.CostCenter;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface CostCenterRepository extends JpaRepository<CostCenter, UUID> {
    List<CostCenter> findByCompanyId(UUID companyId);
}
