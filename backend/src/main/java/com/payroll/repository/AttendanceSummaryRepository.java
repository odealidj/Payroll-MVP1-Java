package com.payroll.repository;

import com.payroll.entity.AttendanceSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AttendanceSummaryRepository extends JpaRepository<AttendanceSummary, UUID> {
    List<AttendanceSummary> findByPayrollPeriodId(UUID payrollPeriodId);
    Optional<AttendanceSummary> findByPayrollPeriodIdAndEmploymentId(UUID payrollPeriodId, UUID employmentId);
}
