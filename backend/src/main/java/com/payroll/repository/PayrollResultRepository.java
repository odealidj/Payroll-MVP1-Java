package com.payroll.repository;

import com.payroll.entity.PayrollResult;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PayrollResultRepository extends JpaRepository<PayrollResult, UUID> {
    List<PayrollResult> findByPayrollRunIdOrderByEmployeeNumberAsc(UUID payrollRunId);
    Optional<PayrollResult> findByPayrollRunIdAndId(UUID payrollRunId, UUID id);
}
