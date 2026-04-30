package com.example.HR_Management.repository;

import com.example.HR_Management.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@RepositoryRestResource(collectionResourceRel = "employees", path = "employees")
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
	long countByDepartment_Location_Id(Long locationId);


    List<Employee> findBySubordinatesIsNotEmpty();

  
    Employee findByEmail(String email);

   
    List<Employee> findByFirstName(String firstName);

    
    
    @Override
    @RestResource(exported = false)
    void deleteById(Long id);

    @Override
    @RestResource(exported = false)
    void delete(Employee entity);

    @Override
    @RestResource(exported = false)
    void deleteAll();
 
 Optional<Employee> findTopByDepartmentDepartmentIdOrderBySalaryDesc(BigDecimal departmentId);
 
 Optional<Employee> findTopByDepartmentDepartmentIdOrderBySalaryAsc(BigDecimal departmentId);
}