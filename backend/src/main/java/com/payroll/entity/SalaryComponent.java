package com.payroll.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "salary_components")
public class SalaryComponent {
    @Id
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(nullable = false, length = 30)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 30)
    private String category;

    @Column(name = "is_fixed", nullable = false)
    private boolean isFixed;

    @Column(name = "is_recurring", nullable = false)
    private boolean isRecurring;

    @Column(name = "tax_treatment", nullable = false, length = 30)
    private String taxTreatment;

    @Column(name = "is_bpjs_kes_base", nullable = false)
    private boolean bpjsKesBase;

    @Column(name = "is_bpjs_jht_base", nullable = false)
    private boolean bpjsJhtBase;

    @Column(name = "is_bpjs_jkk_jkm_base", nullable = false)
    private boolean bpjsJkkJkmBase;

    @Column(name = "is_bpjs_jp_base", nullable = false)
    private boolean bpjsJpBase;

    @Column(name = "is_overtime_base", nullable = false)
    private boolean overtimeBase;

    @Column(name = "is_thr_base", nullable = false)
    private boolean thrBase;

    @Column(name = "is_pkwt_comp_base", nullable = false)
    private boolean pkwtCompBase;

    @Column(name = "prorate_method", nullable = false, length = 20)
    private String prorateMethod;

    @Column(name = "rounding_mode", nullable = false, length = 20)
    private String roundingMode;

    @Column(name = "coa_account_code", nullable = false, length = 50)
    private String coaAccountCode;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public boolean isFixed() { return isFixed; }
    public void setFixed(boolean fixed) { isFixed = fixed; }
    public boolean isRecurring() { return isRecurring; }
    public void setRecurring(boolean recurring) { isRecurring = recurring; }
    public String getTaxTreatment() { return taxTreatment; }
    public void setTaxTreatment(String taxTreatment) { this.taxTreatment = taxTreatment; }
    public boolean isBpjsKesBase() { return bpjsKesBase; }
    public void setBpjsKesBase(boolean bpjsKesBase) { this.bpjsKesBase = bpjsKesBase; }
    public boolean isBpjsJhtBase() { return bpjsJhtBase; }
    public void setBpjsJhtBase(boolean bpjsJhtBase) { this.bpjsJhtBase = bpjsJhtBase; }
    public boolean isBpjsJkkJkmBase() { return bpjsJkkJkmBase; }
    public void setBpjsJkkJkmBase(boolean bpjsJkkJkmBase) { this.bpjsJkkJkmBase = bpjsJkkJkmBase; }
    public boolean isBpjsJpBase() { return bpjsJpBase; }
    public void setBpjsJpBase(boolean bpjsJpBase) { this.bpjsJpBase = bpjsJpBase; }
    public boolean isOvertimeBase() { return overtimeBase; }
    public void setOvertimeBase(boolean overtimeBase) { this.overtimeBase = overtimeBase; }
    public boolean isThrBase() { return thrBase; }
    public void setThrBase(boolean thrBase) { this.thrBase = thrBase; }
    public boolean isPkwtCompBase() { return pkwtCompBase; }
    public void setPkwtCompBase(boolean pkwtCompBase) { this.pkwtCompBase = pkwtCompBase; }
    public String getProrateMethod() { return prorateMethod; }
    public void setProrateMethod(String prorateMethod) { this.prorateMethod = prorateMethod; }
    public String getRoundingMode() { return roundingMode; }
    public void setRoundingMode(String roundingMode) { this.roundingMode = roundingMode; }
    public String getCoaAccountCode() { return coaAccountCode; }
    public void setCoaAccountCode(String coaAccountCode) { this.coaAccountCode = coaAccountCode; }
}
