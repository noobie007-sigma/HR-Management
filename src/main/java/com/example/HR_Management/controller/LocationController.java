package com.example.HR_Management.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.HR_Management.repository.EmployeeRepository;

@RestController
@RequestMapping("/api/v1/locations")
public class LocationController {

    private final EmployeeRepository employeeRepository;

    public LocationController(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @GetMapping("/{id}/employee-count")
    public Map<String, Object> getEmployeeCount(@PathVariable("id") Long locationId) {

        long count = employeeRepository.countByDepartment_Location_Id(locationId);

        return Map.of(
                "locationId", locationId,
                "employeeCount", count
        );
    }
}