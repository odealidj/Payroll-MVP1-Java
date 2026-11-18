package com.payroll.repository;

import com.payroll.entity.WorkLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface WorkLocationRepository extends JpaRepository<WorkLocation, UUID> {
    List<WorkLocation> findByCompanyId(UUID companyId);
}
