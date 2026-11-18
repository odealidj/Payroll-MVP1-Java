package com.payroll.repository;

import com.payroll.entity.PayrollRun;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface PayrollRunRepository extends JpaRepository<PayrollRun, UUID> {
    List<PayrollRun> findByCompanyIdOrderByCreatedAtDesc(UUID companyId);
    List<PayrollRun> findByPayrollPeriodIdOrderByCreatedAtAsc(UUID payrollPeriodId);
}
