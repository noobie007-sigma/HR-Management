package com.example.HR_Management.repository;

import com.example.HR_Management.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import java.math.BigDecimal;
import java.util.List;

@RepositoryRestResource(collectionResourceRel = "department", path = "department")
public interface DepartmentRepository extends JpaRepository<Department, BigDecimal> {

    List<Department> findByLocation_Id(Long locationId);

    Department findByDepartmentName(String departmentName);
}