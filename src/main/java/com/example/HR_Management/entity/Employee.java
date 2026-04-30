package com.example.HR_Management.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "employees")
public class Employee {

    @Id
    @Column(name = "employee_id", precision = 6, scale = 0)
    private Long employeeId;

    
    @NotBlank(message = "First name is required")
    @Size(max = 20, message = "First name cannot exceed 20 characters")
    @Column(name = "first_name", length = 20)
    private String firstName;

    
    @NotBlank(message = "Last name is required")
    @Size(max = 25, message = "Last name cannot exceed 25 characters")
    @Column(name = "last_name", length = 25)
    private String lastName;

    
    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address format")
    @Size(max = 25, message = "Email cannot exceed 25 characters")
    @Column(name = "email", length = 25, unique = true)
    private String email;

    
    @Size(max = 20, message = "Phone number cannot exceed 20 characters")
    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    
    @NotNull(message = "Hire date is required")
    @PastOrPresent(message = "Hire date cannot be in the future")
    @Column(name = "hire_date")
    private LocalDate hireDate;

    
    @NotNull(message = "Salary is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Salary must be greater than 0")
    @Column(name = "salary", precision = 8, scale = 2)
    private BigDecimal salary;

    
    @DecimalMin(value = "0.0", message = "Commission cannot be negative")
    @DecimalMax(value = "0.99", message = "Commission cannot equal or exceed 100%")
    @Column(name = "commission_pct", precision = 2, scale = 2)
    private BigDecimal commissionPct;

    
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id")
    private Job job;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private Employee manager;

    @OneToMany(mappedBy = "manager")
    @JsonIgnore 
    private List<Employee> subordinates;

    
    
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

    public BigDecimal getSalary() { return salary; }
    public void setSalary(BigDecimal salary) { this.salary = salary; }

    public BigDecimal getCommissionPct() { return commissionPct; }
    public void setCommissionPct(BigDecimal commissionPct) { this.commissionPct = commissionPct; }

    public Job getJob() { return job; }
    public void setJob(Job job) { this.job = job; }

    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }

    public Employee getManager() { return manager; }
    public void setManager(Employee manager) { this.manager = manager; }

    public List<Employee> getSubordinates() { return subordinates; }
    public void setSubordinates(List<Employee> subordinates) { this.subordinates = subordinates; }
}