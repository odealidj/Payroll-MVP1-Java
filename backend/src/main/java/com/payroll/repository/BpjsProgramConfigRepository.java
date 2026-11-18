package com.payroll.repository;

import com.payroll.entity.BpjsProgramConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface BpjsProgramConfigRepository extends JpaRepository<BpjsProgramConfig, UUID> {
    List<BpjsProgramConfig> findByRuleSetVersionIdOrderByProgramCodeAsc(UUID ruleSetVersionId);
}
