package com.payroll.repository;

import com.payroll.entity.SalaryComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface SalaryComponentRepository extends JpaRepository<SalaryComponent, UUID> {
    List<SalaryComponent> findByCompanyIdOrderByCodeAsc(UUID companyId);
}
