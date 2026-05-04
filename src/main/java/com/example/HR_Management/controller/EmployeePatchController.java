package com.example.HR_Management.controller;

import com.example.HR_Management.entity.Employee;
import com.example.HR_Management.repository.EmployeeRepository;
import org.springframework.data.rest.webmvc.RepositoryRestController;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

@RepositoryRestController
public class EmployeePatchController {

    private final EmployeeRepository employeeRepository;

    public EmployeePatchController(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @PatchMapping("/employees/{id}/editable")
    @Transactional
    public ResponseEntity<?> patchEditable(
            @PathVariable BigDecimal id,
            @RequestBody Map<String, Object> fields
    ) {
        Optional<Employee> opt = employeeRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Employee emp = opt.get();

        if (fields.containsKey("email")) {
            String email = (String) fields.get("email");
            if (email == null || !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
                return ResponseEntity.badRequest().body("Invalid email format.");
            }
            emp.setEmail(email);
        }

        if (fields.containsKey("phoneNumber")) {
            emp.setPhoneNumber((String) fields.get("phoneNumber"));
        }

        if (fields.containsKey("salary")) {
            Object salaryVal = fields.get("salary");
            BigDecimal salary = new BigDecimal(salaryVal.toString());
            if (salary.compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.badRequest().body("Salary must be greater than 0.");
            }
            emp.setSalary(salary);
        }

        employeeRepository.save(emp);
        return ResponseEntity.ok().build();
    }
}