package com.example.HR_Management.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.HR_Management.entity.Employee;
import com.example.HR_Management.repository.EmployeeRepository;

@RestController
@RequestMapping("/api/v1/country")
public class CountryController {

    private final EmployeeRepository employeeRepository;

    public CountryController(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    // GET /api/v1/country/{countryId}/employees
    // Returns all employees in the given country plus a total count.
    //
    // The repository method uses the "Employee.withDepartmentLocationCountry"
    // entity graph, so department + location + country + job are all JOIN-fetched
    // in a single SQL query — no lazy loading happens here at all.
    //
    // IMPORTANT: We map each Employee to a plain Map before returning.
    // Do NOT return the Employee entity directly — see recursion warning below.
    @GetMapping("/{countryId}/employees")
    public ResponseEntity<Map<String, Object>> getEmployeesByCountry(
            @PathVariable String countryId) {

        List<Employee> employees =
            employeeRepository.findByDepartment_Location_Country_CountryId(countryId);
        long count =
                employeeRepository.countByDepartment_Location_Country_CountryId(countryId);

            // Map each Employee to a safe flat structure.
            // Because the entity graph fetched department + job eagerly,
            // e.getDepartment() and e.getJob() are safe to call here without
            // triggering additional queries or lazy-load exceptions.
            List<Map<String, Object>> employeeDtos = employees.stream().map(e -> {
                Map<String, Object> dto = new LinkedHashMap<>();
                dto.put("employeeId",     e.getEmployeeId());
                dto.put("firstName",      e.getFirstName());
                dto.put("lastName",       e.getLastName());
                dto.put("email",          e.getEmail());
                dto.put("phoneNumber",    e.getPhoneNumber());
                dto.put("hireDate",       e.getHireDate());
                dto.put("salary",         e.getSalary());
                dto.put("jobTitle",       e.getJob() != null ? e.getJob().getJobTitle() : null);
                dto.put("departmentName", e.getDepartment() != null
                                            ? e.getDepartment().getDepartmentName() : null);
                return dto;
            }).collect(Collectors.toList());

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("countryId",     countryId);
            result.put("employeeCount", count);
            result.put("employees",     employeeDtos);

            return ResponseEntity.ok(result);
        }
    }