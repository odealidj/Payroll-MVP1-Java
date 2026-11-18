package com.payroll.repository;

import com.payroll.entity.TaxTerBracket;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface TaxTerBracketRepository extends JpaRepository<TaxTerBracket, UUID> {
    List<TaxTerBracket> findByRuleSetVersionIdOrderByTerCategoryAscIncomeMinAsc(UUID ruleSetVersionId);
}
