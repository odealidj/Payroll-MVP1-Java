package com.payroll.repository;

import com.payroll.entity.Journal;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface JournalRepository extends JpaRepository<Journal, UUID> {
    Optional<Journal> findByPayrollRunId(UUID payrollRunId);
}
