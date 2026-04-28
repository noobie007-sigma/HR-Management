package com.example.HR_Management.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "departments") // [cite: 3, 23]
public class Department {

    @Id
    @Column(name = "department_id", precision = 4, scale = 0) // [cite: 3, 23, 43]
    private BigDecimal departmentId;

    @Column(name = "department_name", nullable = false, length = 30) // [cite: 3, 23, 45]
    private String departmentName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id") // [cite: 3, 23, 46]
    private Location location;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id") // [cite: 3, 23, 45]
    private Employee manager;

    @OneToMany(mappedBy = "department")
    private List<Employee> employees = new ArrayList<>(); // Initialize to avoid NullPointerException

    public Department() {}

    public Department(BigDecimal departmentId, String departmentName) {
        this.departmentId = departmentId;
        this.departmentName = departmentName;
    }

    // Getters and Setters
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