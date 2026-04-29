package com.example.HR_Management.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "departments") 
public class Department {

    @Id
    @NotNull(message = "Department ID is mandatory")  
    @Column(name = "department_id", precision = 4, scale = 0) 
    private BigDecimal departmentId;
    
    @NotBlank(message = "Department name is mandatory")
    @Size(max = 30, message = "Name cannot exceed 30 characters")
    @Pattern(regexp = "^[^0-9]*$", message = "Department name cannot contain numbers")
    @Column(name = "department_name", nullable = false, length = 30) 
    private String departmentName;

    @NotNull(message = "Location is mandatory")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id") 
    private Location location;

    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id", 
            referencedColumnName = "employee_id",
            columnDefinition = "DECIMAL(6,0)") 
    private Employee manager;

    @OneToMany(mappedBy = "department")
    private List<Employee> employees = new ArrayList<>(); 

    public Department() {}

    public Department(BigDecimal departmentId, String departmentName) {
        this.departmentId = departmentId;
        this.departmentName = departmentName;
    }

    
    public BigDecimal getDepartmentId() { return departmentId; }
    public void setDepartmentId(BigDecimal departmentId) { this.departmentId = departmentId; }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public Location getLocation() { return location; }
    public void setLocation(Location location) { this.location = location; }

    public Employee getManager() { return manager; }
    public void setManager(Employee manager) { this.manager = manager; }

    public List<Employee> getEmployees() { return employees; }
    public void setEmployees(List<Employee> employees) { this.employees = employees; }
}