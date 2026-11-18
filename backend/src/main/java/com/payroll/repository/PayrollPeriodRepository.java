package com.payroll.repository;

import com.payroll.entity.PayrollPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface PayrollPeriodRepository extends JpaRepository<PayrollPeriod, UUID> {
    List<PayrollPeriod> findByCompanyIdOrderByPeriodYearAscPeriodMonthAsc(UUID companyId);
}
