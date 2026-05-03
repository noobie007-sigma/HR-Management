package com.example.HR_Management.repository;

import com.example.HR_Management.entity.Employee;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RepositoryRestResource(collectionResourceRel = "employees", path = "employees")
public interface EmployeeRepository extends JpaRepository<Employee, BigDecimal> {
    @RestResource(path = "managers", rel = "managers")
    List<Employee> findBySubordinatesIsNotEmpty();
    @RestResource(path = "byManager", rel = "byManager")
    List<Employee> findByManagerEmployeeId(@Param("managerId") BigDecimal managerId);

    @RestResource(path = "withManager", rel = "withManager")
    @Query("SELECT e FROM Employee e LEFT JOIN FETCH e.manager WHERE e.employeeId = :id")
    Optional<Employee> findByIdWithManager(@Param("id") BigDecimal id);
    @RestResource(path = "byFirstName", rel = "byFirstName")
    List<Employee> findByFirstNameIgnoreCase(@Param("firstName") String firstName);

    @RestResource(path = "byEmail", rel = "byEmail")
    Optional<Employee> findByEmailIgnoreCase(@Param("email") String email);

    @RestResource(path = "byPhone", rel = "byPhone")
    Optional<Employee> findByPhoneNumber(@Param("phoneNumber") String phoneNumber);


    @RestResource(path = "noCommission", rel = "noCommission")
    List<Employee> findByCommissionPctIsNull();


    @RestResource(path = "byHireDate", rel = "byHireDate")
    List<Employee> findByHireDateBetween(
            @Param("start") LocalDate start,
            @Param("end")   LocalDate end);


    @RestResource(path = "byDepartment", rel = "byDepartment")
    Page<Employee> findByDepartmentDepartmentId(
            @Param("departmentId") BigDecimal departmentId,
            Pageable pageable);

    Optional<Employee> findTopByDepartmentDepartmentIdOrderBySalaryDesc(BigDecimal departmentId);

    Optional<Employee> findTopByDepartmentDepartmentIdOrderBySalaryAsc(BigDecimal departmentId);

    @RestResource(path = "totalCommission", rel = "totalCommission", exported = false)
    @Query("SELECT SUM(e.salary * COALESCE(e.commissionPct, 0)) " +
           "FROM Employee e WHERE e.department.departmentId = :deptId")
    BigDecimal findTotalCommissionByDepartment(@Param("deptId") BigDecimal deptId);

    
    @RestResource(path = "countByDepartment", rel = "countByDepartment", exported = false)
    @Query("SELECT d.departmentName, COUNT(e) " +
           "FROM Employee e JOIN e.department d " +
           "GROUP BY d.departmentName")
    List<Object[]> countEmployeesGroupByDepartment();
    long countByDepartment_Location_Id(BigDecimal locationId);

    @RestResource(path = "countByLocation", rel = "countByLocation", exported = false)
    @Query("SELECT l.city, COUNT(e) " +
           "FROM Employee e JOIN e.department d JOIN d.location l " +
           "GROUP BY l.city")
    List<Object[]> countEmployeesGroupByLocation();


    @RestResource(path = "maxSalaryOfJob", rel = "maxSalaryOfJob", exported = false)
    @Query("SELECT e.job.maxSalary FROM Employee e WHERE e.employeeId = :id")
    BigDecimal findMaxSalaryOfJobByEmployee(@Param("id") BigDecimal id);

    @RestResource(path = "openPositions", rel = "openPositions", exported = false)
    @Query("SELECT j.jobId FROM Job j " +
           "WHERE j.jobId NOT IN " +
           "(SELECT DISTINCT e.job.jobId FROM Employee e WHERE e.job IS NOT NULL)")
    List<String> findAllOpenPositions();

    @RestResource(path = "openPositionsByDept", rel = "openPositionsByDept", exported = false)
    @Query("SELECT j.jobId FROM Job j " +
           "WHERE j.jobId NOT IN (" +
           "  SELECT DISTINCT e.job.jobId FROM Employee e " +
           "  WHERE e.job IS NOT NULL AND e.department.departmentId = :deptId" +
           ")")
    List<String> findAllOpenPositionsByDepartment(@Param("deptId") BigDecimal deptId);


    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @RestResource(exported = false)
    @Query("UPDATE Employee e SET e.email = :email WHERE e.employeeId = :id")
    int updateEmail(@Param("id") BigDecimal id, @Param("email") String email);


    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @RestResource(exported = false)
    @Query("UPDATE Employee e SET e.phoneNumber = :phone WHERE e.employeeId = :id")
    int updatePhoneNumber(@Param("id") BigDecimal id, @Param("phone") String phone);


    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @RestResource(exported = false)
    @Query("UPDATE Employee e SET e.department.departmentId = :deptId WHERE e.employeeId = :empId")
    int assignDepartment(@Param("empId") BigDecimal empId,
                         @Param("deptId") BigDecimal deptId);


    @Override
    @RestResource(exported = false)
    void deleteById(BigDecimal id);

    @Override
    @RestResource(exported = false)
    void delete(Employee entity);

    @Override
    @RestResource(exported = false)
    void deleteAll();

	
}