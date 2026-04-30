package com.example.HR_Management.repository;

import com.example.HR_Management.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

import java.math.BigDecimal;
import java.util.List;

@RepositoryRestResource(path = "department", collectionResourceRel = "department")
public interface DepartmentRepository extends JpaRepository<Department, BigDecimal> {

    @RestResource(path = "byName", rel = "byName")
    List<Department> findByDepartmentName(@Param("name") String name);

    
    @RestResource(path = "byLocation", rel = "byLocation")
    List<Department> findByLocationId(@Param("locationId") BigDecimal locationId);

    boolean existsById(BigDecimal id);
}