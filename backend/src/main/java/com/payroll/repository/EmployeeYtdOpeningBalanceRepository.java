package com.payroll.repository;

import com.payroll.entity.EmployeeYtdOpeningBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeYtdOpeningBalanceRepository extends JpaRepository<EmployeeYtdOpeningBalance, UUID> {
    Optional<EmployeeYtdOpeningBalance> findByEmploymentIdAndTaxYear(UUID employmentId, Short taxYear);
    List<EmployeeYtdOpeningBalance> findByCompanyIdAndTaxYear(UUID companyId, Short taxYear);
}
