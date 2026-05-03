package com.example.HR_Management.controller;


import com.example.HR_Management.repository.EmployeeRepository;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Custom REST controller for aggregate and non-entity endpoints.
 *
 * WHY THIS IS NEEDED:
 *   Spring Data REST can only serialise results into HAL JSON when the
 *   return type is an @Entity, Page<@Entity>, or List<@Entity>.
 *   The following repository methods return non-entity types:
 *
 *     findTotalCommissionByDepartment  → BigDecimal
 *     countEmployeesGroupByDepartment  → List<Object[]>
 *     countEmployeesGroupByLocation    → List<Object[]>
 *     findMaxSalaryOfJobByEmployee     → BigDecimal
 *     findAllOpenPositions             → List<String>
 *     findAllOpenPositionsByDepartment → List<String>
 *
 *   When SDR tries to expose these via @RepositoryRestResource /search/,
 *   it throws:
 *     MappingException: Couldn't find PersistentEntity for type class java.lang.String
 *     MappingException: Couldn't find PersistentEntity for type class [Ljava.lang.Object;
 *
 *   These endpoints MUST be handled by a custom @RestController that
 *   returns plain JSON (not HAL), bypassing SDR serialisation entirely.
 *
 * URL DESIGN:
 *   All URLs match the spec paths exactly. They do NOT conflict with the
 *   SDR /api/v1/employees collection because they use sub-paths that SDR
 *   does not claim (SDR claims /api/v1/employees and /api/v1/employees/{id}).
 *
 * NOTE: The SDR @RepositoryRestResource search entries for these methods
 *   (countByDepartment, countByLocation, totalCommission, maxSalaryOfJob,
 *   openPositions, openPositionsByDept) should be kept in the repo with
 *   @RestResource(exported = false) so SDR does not try to expose them,
 *   OR left as-is and these controller paths take precedence (Spring MVC
 *   routes to the most specific handler first).
 */
// @RepositoryRestController registers this controller inside Spring Data REST's
// RepositoryRestHandlerMapping. Within that mapping, literal segments like
// /employees/countByDepartment always beat the generic /{id} wildcard, so SDR
// no longer steals these requests before they reach our methods.
//
// The base path (/api/v1) is prepended automatically from
// spring.data.rest.base-path, so we only declare the resource-relative path.
@RestController
@RequestMapping("api/v1/employees")
public class EmployeeAggregateController {

    private final EmployeeRepository employeeRepository;

    public EmployeeAggregateController(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    // ================================================================
    // Spec: GET /api/v1/employees/findTotalCommissionIssuedToEmployee/{dept_id}
    // Mapped here as: GET /api/v1/employees/totalCommission/{deptId}
    // Returns: { "departmentId": 80, "totalCommission": 12345.67 }
    // ================================================================

    @GetMapping("/totalCommission/{deptId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getTotalCommission(
            @PathVariable BigDecimal deptId) {

        BigDecimal total = employeeRepository.findTotalCommissionByDepartment(deptId);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("departmentId", deptId);
        response.put("totalCommission", total != null ? total : BigDecimal.ZERO);
        return ResponseEntity.ok(response);
    }

    // ================================================================
    // Spec: GET /api/v1/employees/employees_departmentwise_count
    // Mapped here as: GET /api/v1/employees/countByDepartment
    // Returns: [ { "departmentName": "Sales", "employeeCount": 34 }, ... ]
    // ================================================================

    @GetMapping("/countByDepartment")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> countByDepartment() {

        List<Object[]> rows = employeeRepository.countEmployeesGroupByDepartment();

        List<Map<String, Object>> result = rows.stream()
                .map(row -> {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("departmentName", row[0]);
                    entry.put("employeeCount", row[1]);
                    return entry;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    // ================================================================
    // Spec: GET /api/v1/employees/locationwisecountofemployees
    // Mapped here as: GET /api/v1/employees/countByLocation
    // Returns: [ { "city": "Seattle", "employeeCount": 20 }, ... ]
    // ================================================================

    @GetMapping("/countByLocation")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> countByLocation() {

        List<Object[]> rows = employeeRepository.countEmployeesGroupByLocation();

        List<Map<String, Object>> result = rows.stream()
                .map(row -> {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("city", row[0]);
                    entry.put("employeeCount", row[1]);
                    return entry;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    // ================================================================
    // Spec: GET /api/v1/employees/{empid}/findmaxsalaryofjob
    // Mapped here as: GET /api/v1/employees/{empId}/maxSalaryOfJob
    // Returns: { "employeeId": 149, "maxSalaryOfJob": 14000.00 }
    // ================================================================

    @GetMapping("/{empId}/maxSalaryOfJob")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getMaxSalaryOfJob(
            @PathVariable BigDecimal empId) {

        // Verify employee exists first
        employeeRepository.findById(empId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee" + empId));

        BigDecimal maxSalary = employeeRepository
                .findMaxSalaryOfJobByEmployee(empId);

        if (maxSalary == null) {
            throw new ResourceNotFoundException("Job max salary for Employee" + empId);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("employeeId", empId);
        response.put("maxSalaryOfJob", maxSalary);
        return ResponseEntity.ok(response);
    }

    // ================================================================
    // Spec: GET /api/v1/employees/findAllOpenPositions
    // Mapped here as: GET /api/v1/employees/openPositions
    // Returns: { "openPositions": ["JOB_ID_1", "JOB_ID_2", ...] }
    // ================================================================

    @GetMapping("/openPositions")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getAllOpenPositions() {

        List<String> positions = employeeRepository.findAllOpenPositions();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("openPositions", positions);
        response.put("count", positions.size());
        return ResponseEntity.ok(response);
    }

    // ================================================================
    // Spec: GET /api/v1/employees/findAllOpenPositions/{department_id}
    // Mapped here as: GET /api/v1/employees/openPositions/{deptId}
    // Returns: { "departmentId": 80, "openPositions": [...], "count": 5 }
    // ================================================================

    @GetMapping("/openPositions/{deptId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getOpenPositionsByDept(
            @PathVariable BigDecimal deptId) {

        List<String> positions = employeeRepository
                .findAllOpenPositionsByDepartment(deptId);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("departmentId", deptId);
        response.put("openPositions", positions);
        response.put("count", positions.size());
        return ResponseEntity.ok(response);
    }
}