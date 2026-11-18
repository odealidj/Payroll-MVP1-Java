package com.payroll.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "journal_lines")
public class JournalLine {
    @Id
    private UUID id;

    @Column(name = "journal_id", nullable = false)
    private UUID journalId;

    @Column(name = "line_no", nullable = false)
    private int lineNo;

    @Column(name = "coa_account_code", nullable = false, length = 50)
    private String coaAccountCode;

    @Column(name = "account_name", nullable = false, length = 120)
    private String accountName;

    @Column(name = "cost_center_id", nullable = false)
    private UUID costCenterId;

    @Column(name = "cost_center_code", nullable = false, length = 30)
    private String costCenterCode;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal debit;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal credit;

    @Column(nullable = false)
    private String description;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getJournalId() { return journalId; }
    public void setJournalId(UUID journalId) { this.journalId = journalId; }
    public int getLineNo() { return lineNo; }
    public void setLineNo(int lineNo) { this.lineNo = lineNo; }
    public String getCoaAccountCode() { return coaAccountCode; }
    public void setCoaAccountCode(String coaAccountCode) { this.coaAccountCode = coaAccountCode; }
    public String getAccountName() { return accountName; }
    public void setAccountName(String accountName) { this.accountName = accountName; }
    public UUID getCostCenterId() { return costCenterId; }
    public void setCostCenterId(UUID costCenterId) { this.costCenterId = costCenterId; }
    public String getCostCenterCode() { return costCenterCode; }
    public void setCostCenterCode(String costCenterCode) { this.costCenterCode = costCenterCode; }
    public BigDecimal getDebit() { return debit; }
    public void setDebit(BigDecimal debit) { this.debit = debit; }
    public BigDecimal getCredit() { return credit; }
    public void setCredit(BigDecimal credit) { this.credit = credit; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
