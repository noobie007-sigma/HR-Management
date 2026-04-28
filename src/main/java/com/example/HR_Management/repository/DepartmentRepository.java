package com.example.HR_Management.repository;

import com.example.HR_Management.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, BigDecimal> {

    List<Department> findByLocation_LocationId(BigDecimal locationId);

    Department findByDepartmentName(String departmentName);
}