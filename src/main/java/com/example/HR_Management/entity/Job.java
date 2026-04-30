package com.example.HR_Management.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "jobs")
public class Job {

    @Id
    @Column(name = "job_id", length = 10)
    @NotBlank(message = "Job ID is required")
    @Size(max = 10, message = "Job ID must be <= 10 characters")
    private String jobId;

    @Column(name = "job_title", length = 50, nullable = false)
    @NotBlank(message = "Job title is required")
    @Size(max = 50, message = "Job title must be <= 50 characters")
    private String jobTitle;

    @Column(name = "min_salary")
    @Min(value = 0, message = "Min salary must be >= 0")
    private BigDecimal minSalary;

    @Column(name = "max_salary")
    @Min(value = 0, message = "Max salary must be >= 0")
    private BigDecimal maxSalary;

   
    @OneToMany(mappedBy = "job", fetch = FetchType.LAZY)
    @JsonIgnore   // prevents infinite recursion
    private List<Employee> employees;
    
    @AssertTrue(message = "Min salary must be <= Max salary")
    public boolean isSalaryValid() {
        if (minSalary == null || maxSalary == null) return true;
        return minSalary.compareTo(maxSalary)<=0;
    }


 
	public String getJobId() {
		return jobId;
	}

	public void setJobId(String jobId) {
		this.jobId = jobId;
	}

	public String getJobTitle() {
		return jobTitle;
	}

	public void setJobTitle(String jobTitle) {
		this.jobTitle = jobTitle;
	}

	public BigDecimal getMinSalary() {
		return minSalary;
	}

	public void setMinSalary(BigDecimal minSalary) {
		this.minSalary = minSalary;
	}

	public BigDecimal getMaxSalary() {
		return maxSalary;
	}

	public void setMaxSalary(BigDecimal maxSalary) {
		this.maxSalary = maxSalary;
	}

	public List<Employee> getEmployees() {
		return employees;
	}

	public void setEmployees(List<Employee> employees) {
		this.employees = employees;
	}
    
}