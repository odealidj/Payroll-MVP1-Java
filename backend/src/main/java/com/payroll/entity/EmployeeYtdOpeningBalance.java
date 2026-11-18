package com.payroll.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "employee_ytd_opening_balances")
public class EmployeeYtdOpeningBalance {
    @Id
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "employment_id", nullable = false)
    private UUID employmentId;

    @Column(name = "tax_year", nullable = false)
    private Short taxYear;

    @Column(name = "cutoff_month_count", nullable = false)
    private Short cutoffMonthCount;

    @Column(name = "ytd_gross_taxable", nullable = false, precision = 18, scale = 2)
    private BigDecimal ytdGrossTaxable;

    @Column(name = "ytd_biaya_jabatan", nullable = false, precision = 18, scale = 2)
    private BigDecimal ytdBiayaJabatan;

    @Column(name = "ytd_bpjs_worker_pension", nullable = false, precision = 18, scale = 2)
    private BigDecimal ytdBpjsWorkerPension;

    @Column(name = "ytd_pph21_withheld", nullable = false, precision = 18, scale = 2)
    private BigDecimal ytdPph21Withheld;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public UUID getEmploymentId() { return employmentId; }
    public void setEmploymentId(UUID employmentId) { this.employmentId = employmentId; }
    public Short getTaxYear() { return taxYear; }
    public void setTaxYear(Short taxYear) { this.taxYear = taxYear; }
    public Short getCutoffMonthCount() { return cutoffMonthCount; }
    public void setCutoffMonthCount(Short cutoffMonthCount) { this.cutoffMonthCount = cutoffMonthCount; }
    public BigDecimal getYtdGrossTaxable() { return ytdGrossTaxable; }
    public void setYtdGrossTaxable(BigDecimal ytdGrossTaxable) { this.ytdGrossTaxable = ytdGrossTaxable; }
    public BigDecimal getYtdBiayaJabatan() { return ytdBiayaJabatan; }
    public void setYtdBiayaJabatan(BigDecimal ytdBiayaJabatan) { this.ytdBiayaJabatan = ytdBiayaJabatan; }
    public BigDecimal getYtdBpjsWorkerPension() { return ytdBpjsWorkerPension; }
    public void setYtdBpjsWorkerPension(BigDecimal ytdBpjsWorkerPension) { this.ytdBpjsWorkerPension = ytdBpjsWorkerPension; }
    public BigDecimal getYtdPph21Withheld() { return ytdPph21Withheld; }
    public void setYtdPph21Withheld(BigDecimal ytdPph21Withheld) { this.ytdPph21Withheld = ytdPph21Withheld; }
}
