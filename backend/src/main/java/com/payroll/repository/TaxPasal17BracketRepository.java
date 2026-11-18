package com.payroll.repository;

import com.payroll.entity.TaxPasal17Bracket;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface TaxPasal17BracketRepository extends JpaRepository<TaxPasal17Bracket, UUID> {
    List<TaxPasal17Bracket> findByRuleSetVersionIdOrderByLayerNoAsc(UUID ruleSetVersionId);
}
