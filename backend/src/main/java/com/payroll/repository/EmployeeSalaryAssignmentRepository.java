package com.payroll.repository;

import com.payroll.entity.EmployeeSalaryAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface EmployeeSalaryAssignmentRepository extends JpaRepository<EmployeeSalaryAssignment, UUID> {
    List<EmployeeSalaryAssignment> findByEmploymentIdOrderByValidFromAsc(UUID employmentId);
    List<EmployeeSalaryAssignment> findByCompanyIdOrderByValidFromAsc(UUID companyId);
}
