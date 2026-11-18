package com.payroll.repository;

import com.payroll.entity.OvertimeMultiplierRule;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface OvertimeMultiplierRuleRepository extends JpaRepository<OvertimeMultiplierRule, UUID> {
    List<OvertimeMultiplierRule> findByRuleSetVersionIdOrderByHourFromAsc(UUID ruleSetVersionId);
}
