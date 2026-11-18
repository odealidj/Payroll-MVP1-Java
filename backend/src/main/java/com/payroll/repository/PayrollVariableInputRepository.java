package com.payroll.repository;

import com.payroll.entity.PayrollVariableInput;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface PayrollVariableInputRepository extends JpaRepository<PayrollVariableInput, UUID> {
    List<PayrollVariableInput> findByPayrollPeriodId(UUID payrollPeriodId);
}
