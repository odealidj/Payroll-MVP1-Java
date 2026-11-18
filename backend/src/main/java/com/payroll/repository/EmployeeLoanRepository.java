package com.payroll.repository;

import com.payroll.entity.EmployeeLoan;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface EmployeeLoanRepository extends JpaRepository<EmployeeLoan, UUID> {
    List<EmployeeLoan> findByCompanyIdAndStatus(UUID companyId, String status);
    List<EmployeeLoan> findByEmploymentIdAndStatus(UUID employmentId, String status);
}
