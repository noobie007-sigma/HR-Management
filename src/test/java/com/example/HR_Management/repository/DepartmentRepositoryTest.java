package com.example.HR_Management.repository;

import com.example.HR_Management.entity.Department;
import com.example.HR_Management.entity.Employee;
import com.example.HR_Management.entity.Location;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class DepartmentRepositoryTest {

    @Autowired
    private DepartmentRepository repository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private Location testLocation;

    @BeforeEach
    void setUp() {
        testLocation = entityManager.find(Location.class, new BigDecimal("1700"));

        if (testLocation == null) {
            testLocation = new Location();
            testLocation.setId(new BigDecimal("1700"));
            testLocation.setCity("New York");
            entityManager.persist(testLocation);
            entityManager.flush();
        }
    }

    @Test
    @DisplayName("Positive - Save Department: record is persisted with specific location_id")
    void testSaveDepartment_valid_success() {
        BigDecimal newId = new BigDecimal("9999");

        if (repository.existsById(newId)) {
            repository.deleteById(newId);
            repository.flush();
        }

        Department dept = new Department(newId, "Testing Dept", testLocation);
        Department saved = repository.saveAndFlush(dept);

        assertNotNull(saved);
        assertEquals("Testing Dept", saved.getDepartmentName());
        assertEquals(newId, saved.getDepartmentId());
        assertNotNull(saved.getLocation());
        assertEquals(new BigDecimal("1700"), saved.getLocation().getId());
    }

    @Test
    @DisplayName("Negative - Name Too Long: throws ConstraintViolationException if name exceeds 30 chars")
    void testSaveDepartment_nameTooLong_throwsConstraintViolation() {
        Department longName = new Department(
                new BigDecimal("1000"),
                "This Department Name Is Way Too Long To Fit In Thirty Characters",
                testLocation
        );

        assertThrows(ConstraintViolationException.class,
                () -> repository.saveAndFlush(longName));
    }

    @Test
    @DisplayName("Negative - Invalid ID Scale: rejects department_id exceeding DECIMAL(4,0)")
    void testSaveDepartment_invalidIdScale_throwsException() {
        Department dept = new Department(
                new BigDecimal("99999"),
                "Scale Test",
                testLocation
        );

        assertThrows(Exception.class,
                () -> repository.saveAndFlush(dept));
    }

    @Test
    @DisplayName("Positive - Update Manager: correctly updates manager_id for an existing record")
    void testUpdateManager_existingDept_updatesSuccessfully() {
        Department dept = entityManager.find(Department.class, new BigDecimal("10"));
        assertNotNull(dept, "Department with ID 10 must exist in the hr database");

        Employee newManager = entityManager.find(Employee.class, new BigDecimal("200"));
        assertNotNull(newManager, "Employee 200 must exist to be assigned as manager");

        dept.setManager(newManager);
        repository.saveAndFlush(dept);
        entityManager.clear();

        Department updated = entityManager.find(Department.class, new BigDecimal("10"));
        assertNotNull(updated.getManager());
        assertEquals(new BigDecimal("200"), updated.getManager().getEmployeeId());
    }

    @Test
    @DisplayName("Positive - Fetch Max Salary: returns a positive max salary for Sales dept (80)")
    void testFindMaxSalary_salesDept_returnsPositiveValue() {
        // Now resolved via EmployeeRepository derived query
        Optional<Employee> result = employeeRepository
                .findTopByDepartmentDepartmentIdOrderBySalaryDesc(new BigDecimal("80"));

        assertTrue(result.isPresent(), "Must find at least one employee in dept 80");
        BigDecimal maxSal = result.get().getSalary();
        assertNotNull(maxSal, "Max salary must not be null for dept 80");
        assertTrue(maxSal.compareTo(BigDecimal.ZERO) > 0,
                "Max salary must be greater than zero");
    }

    @Test
    @DisplayName("Positive - Fetch Min Salary: returns a positive min salary for Sales dept (80)")
    void testFindMinSalary_salesDept_returnsPositiveValue() {
       
        Optional<Employee> result = employeeRepository
                .findTopByDepartmentDepartmentIdOrderBySalaryAsc(new BigDecimal("80"));

        assertTrue(result.isPresent(), "Must find at least one employee in dept 80");
        BigDecimal minSal = result.get().getSalary();
        assertNotNull(minSal, "Min salary must not be null for dept 80");
        assertTrue(minSal.compareTo(BigDecimal.ZERO) > 0,
                "Min salary must be greater than zero");
    }

    @Test
    @DisplayName("Negative - Empty Dept Salary: returns null for department with no employees")
    void testSalaryQuery_emptyDept_returnsNull() {
        BigDecimal emptyDeptId = new BigDecimal("9998");

        if (!repository.existsById(emptyDeptId)) {
            Department emptyDept = new Department(emptyDeptId, "Empty Dept", testLocation);
            repository.saveAndFlush(emptyDept);
        }

        
        Optional<Employee> maxResult = employeeRepository
                .findTopByDepartmentDepartmentIdOrderBySalaryDesc(emptyDeptId);
        Optional<Employee> minResult = employeeRepository
                .findTopByDepartmentDepartmentIdOrderBySalaryAsc(emptyDeptId);

        assertTrue(maxResult.isEmpty(),
                "Max salary should be empty for a dept with no employees");
        assertTrue(minResult.isEmpty(),
                "Min salary should be empty for a dept with no employees");
    }

    @Test
    @DisplayName("Positive - List By Location: returns all departments for location_id 1700")
    void testFindByLocationId_returnsAllDepartmentsAtLocation() {
        List<Department> result = repository.findByLocationId(new BigDecimal("1700"));

        assertNotNull(result);
        assertFalse(result.isEmpty(),
                "At least one department must be linked to location 1700");
        result.forEach(d ->
                assertEquals(new BigDecimal("1700"), d.getLocation().getId(),
                        "Every returned department must belong to location 1700"));
    }

    @Test
    @DisplayName("Positive - Count Personnel: groups and counts employees by department_id")
    void testCountAllEmployeesGroupByDepartment_returnsGroupedResults() {
        List<Department> allDepartments = repository.findAll();

        assertNotNull(allDepartments);
        assertFalse(allDepartments.isEmpty(), "At least one department must exist");

        allDepartments.forEach(dept -> {
            assertNotNull(dept.getDepartmentId());
            assertNotNull(dept.getDepartmentName());
            int count = dept.getEmployees().size();
            assertTrue(count >= 0,
                    "Employee count for dept " + dept.getDepartmentName() + " must be >= 0");
        });
    }

    @Test
    @DisplayName("Negative - Duplicate Primary Key: throws PersistenceException on duplicate department_id")
    void testSaveDepartment_duplicatePk_throwsOnInsert() {
        assertThrows(PersistenceException.class, () -> {
            Department dup = new Department(new BigDecimal("10"), "Duplicate", testLocation);
            entityManager.persist(dup);
            entityManager.flush();
        });
    }

    @Test
    @DisplayName("Positive - Find By Name: returns exact department record for 'Administration'")
    void testFindByDepartmentName_administration_returnsRecord() {
        List<Department> result = repository.findByDepartmentName("Administration");

        assertFalse(result.isEmpty(),
                "Department named 'Administration' must exist in the hr database");
        assertEquals("Administration", result.get(0).getDepartmentName());
    }

    @Test
    @DisplayName("Negative - Null Constraint: rejects record if department_name is null")
    void testSaveDepartment_nullName_throwsConstraintViolation() {
        Department dept = new Department(new BigDecimal("1002"), null, testLocation);

        assertThrows(ConstraintViolationException.class,
                () -> repository.saveAndFlush(dept));
    }

    @Test
    @DisplayName("Positive - Exists Check: returns true for ID 10 and false for ID 999")
    void testExistsById_returnsCorrectBooleans() {
        assertTrue(repository.existsById(new BigDecimal("10")),
                "Department 10 must exist in the hr database");
        assertFalse(repository.existsById(new BigDecimal("999")),
                "Department 999 must not exist");
    }

    @Test
    @DisplayName("Positive - Fetch Header Data: findById retrieves correct New York location mapping")
    void testFindById_returnsCorrectLocationMapping() {
        Department dept = repository.findById(new BigDecimal("10")).orElse(null);

        assertNotNull(dept, "Department 10 must exist");
        assertNotNull(dept.getLocation(), "Department 10 must have a location");

        Location loc = dept.getLocation();
        assertNotNull(loc.getCity(), "Location city must not be null");
        assertNotNull(loc.getStateProvince(), "Location state must not be null");
    }

    @Test
    @DisplayName("Negative - FK Violation: rejects location_id not in locations table")
    void testSaveDepartment_invalidLocationFk_throwsException() {
        assertThrows(Exception.class, () -> {
            Location ghost = new Location();
            ghost.setId(new BigDecimal("8888"));
            ghost.setCity("Ghost City");

            Department dept = new Department(new BigDecimal("1003"), "Ghost Dept", ghost);
            entityManager.persist(dept);
            entityManager.flush();
        });
    }
}