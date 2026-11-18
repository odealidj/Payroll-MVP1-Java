package com.payroll.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "tax_pasal17_brackets")
public class TaxPasal17Bracket {
    @Id
    private UUID id;

    @Column(name = "rule_set_version_id", nullable = false)
    private UUID ruleSetVersionId;

    @Column(name = "layer_no", nullable = false)
    private Short layerNo;

    @Column(name = "pkp_min", nullable = false, precision = 18, scale = 2)
    private BigDecimal pkpMin;

    @Column(name = "pkp_max", precision = 18, scale = 2)
    private BigDecimal pkpMax;

    @Column(nullable = false, precision = 10, scale = 6)
    private BigDecimal rate;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getRuleSetVersionId() { return ruleSetVersionId; }
    public void setRuleSetVersionId(UUID ruleSetVersionId) { this.ruleSetVersionId = ruleSetVersionId; }
    public Short getLayerNo() { return layerNo; }
    public void setLayerNo(Short layerNo) { this.layerNo = layerNo; }
    public BigDecimal getPkpMin() { return pkpMin; }
    public void setPkpMin(BigDecimal pkpMin) { this.pkpMin = pkpMin; }
    public BigDecimal getPkpMax() { return pkpMax; }
    public void setPkpMax(BigDecimal pkpMax) { this.pkpMax = pkpMax; }
    public BigDecimal getRate() { return rate; }
    public void setRate(BigDecimal rate) { this.rate = rate; }
}
