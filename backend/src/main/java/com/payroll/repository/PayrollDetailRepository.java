package com.payroll.repository;

import com.payroll.entity.PayrollDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface PayrollDetailRepository extends JpaRepository<PayrollDetail, UUID> {
    List<PayrollDetail> findByPayrollResultId(UUID payrollResultId);
}
