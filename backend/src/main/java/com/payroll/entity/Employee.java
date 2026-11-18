package com.payroll.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "employees")
public class Employee {
    @Id
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(nullable = false, length = 5)
    private String nationality;

    @Column(name = "nik_masked", nullable = false, length = 25)
    private String nikMasked;

    @Column(name = "npwp_masked", nullable = false, length = 25)
    private String npwpMasked;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(name = "bank_code", nullable = false, length = 20)
    private String bankCode;

    @Column(name = "bank_account_masked", nullable = false, length = 30)
    private String bankAccountMasked;

    @Column(name = "bank_verification_status", nullable = false, length = 20)
    private String bankVerificationStatus;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }
    public String getNationality() { return nationality; }
    public void setNationality(String nationality) { this.nationality = nationality; }
    public String getNikMasked() { return nikMasked; }
    public void setNikMasked(String nikMasked) { this.nikMasked = nikMasked; }
    public String getNpwpMasked() { return npwpMasked; }
    public void setNpwpMasked(String npwpMasked) { this.npwpMasked = npwpMasked; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getBankCode() { return bankCode; }
    public void setBankCode(String bankCode) { this.bankCode = bankCode; }
    public String getBankAccountMasked() { return bankAccountMasked; }
    public void setBankAccountMasked(String bankAccountMasked) { this.bankAccountMasked = bankAccountMasked; }
    public String getBankVerificationStatus() { return bankVerificationStatus; }
    public void setBankVerificationStatus(String bankVerificationStatus) { this.bankVerificationStatus = bankVerificationStatus; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
