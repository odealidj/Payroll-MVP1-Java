package com.payroll.repository;

import com.payroll.entity.SimulatorOverride;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SimulatorOverrideRepository extends JpaRepository<SimulatorOverride, UUID> {
    List<SimulatorOverride> findByPayrollPeriodId(UUID payrollPeriodId);
    Optional<SimulatorOverride> findByPayrollPeriodIdAndEmploymentId(UUID payrollPeriodId, UUID employmentId);
}
