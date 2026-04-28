package com.example.HR_Management.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "employees")
public class Employee {

    @Id
    @Column(name = "employee_id", precision = 6, scale = 0)
    private Long employeeId; // [cite: 33]

    @Column(name = "first_name", length = 20)
    private String firstName; // [cite: 33]

    @Column(name = "last_name", length = 25)
    private String lastName; // [cite: 33]

    @Column(name = "email", length = 25)
    private String email; // [cite: 33]

    @Column(name = "phone_number", length = 20)
    private String phoneNumber; // [cite: 34]

    @Column(name = "hire_date")
    private LocalDate hireDate; // [cite: 34]

    // Mapped as basic types for now to avoid dependency compilation errors
    // Refactor to @ManyToOne once your teammates build the Job and Department entities
    @Column(name = "job_id", length = 10)
    private String jobId; // [cite: 35]

    @Column(name = "salary", precision = 8, scale = 2)
    private BigDecimal salary; // [cite: 36]

    @Column(name = "commission_pct", precision = 2, scale = 2)
    private BigDecimal commissionPct; // [cite: 37]

    @Column(name = "manager_id", precision = 6, scale = 0)
    private Long managerId; // [cite: 38]

    @Column(name = "department_id", precision = 4, scale = 0)
    private Long departmentId; // [cite: 38]

    // --- Getters and Setters ---

    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public LocalDate getHireDate() { return hireDate; }
    public void setHireDate(LocalDate hireDate) { this.hireDate = hireDate; }

    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }

    public BigDecimal getSalary() { return salary; }
    public void setSalary(BigDecimal salary) { this.salary = salary; }

    public BigDecimal getCommissionPct() { return commissionPct; }
    public void setCommissionPct(BigDecimal commissionPct) { this.commissionPct = commissionPct; }

    public Long getManagerId() { return managerId; }
    public void setManagerId(Long managerId) { this.managerId = managerId; }

    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
}