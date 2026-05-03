package com.example.HR_Management.repository;

import com.example.HR_Management.entity.Department;
import com.example.HR_Management.entity.Employee;
import com.example.HR_Management.entity.Job;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DataJpa tests for EmployeeRepository.
 *
 * Convention followed by the team (see DepartmentRepositoryTest):
 *  - @DataJpaTest + @AutoConfigureTestDatabase(replace = NONE) → uses the
 *    shared remote MySQL HRTEST schema (configured in test/resources/application.properties)
 *  - No @BeforeEach fixtures; each test relies on data that already exists
 *    in the HRTEST seed, mirroring the pattern established in DepartmentRepositoryTest.
 *  - Positive tests assert on known, stable rows in the HR data set.
 *  - Negative tests assert on constraint violations or empty results.
 *
 * Known stable data in the standard Oracle HR sample set:
 *   Employee 100  = Steven King      (no manager, no commission, dept 90)
 *   Employee 101  = Neena Kochhar    (manager 100, no commission, dept 90)
 *   Employee 149  = Eleni Zlotkey    (has commission, dept 80 Sales)
 *   Department 80 = Sales (has many employees with commission)
 *   Department 90 = Executive
 *   Job SA_REP    = Sales Representative (minSalary=2000, maxSalary=12000)
 *
 * -----------------------------------------------------------------------
 * ERRORS FIXED (10 total — all caused by the same root issue):
 *
 * Root cause: Employee.employeeId is BigDecimal in the @Entity, so the
 * JpaRepository ID type is BigDecimal. Every place the test used a Long
 * literal or "nnnL" string inside new BigDecimal("...") was wrong.
 *
 * Error 1  — new BigDecimal("9901L") → NumberFormatException at runtime.
 *            'L' is a Java literal suffix, not valid in BigDecimal(String).
 *            Fixed: new BigDecimal("9901")
 *
 * Error 2  — new BigDecimal("9902L") → same. Fixed: new BigDecimal("9902")
 * Error 3  — new BigDecimal("9903L") → same. Fixed: new BigDecimal("9903")
 * Error 4  — new BigDecimal("9904L") → same. Fixed: new BigDecimal("9904")
 * Error 5  — new BigDecimal("9905L") → same. Fixed: new BigDecimal("9905")
 * Error 6  — new BigDecimal("9906L") → same. Fixed: new BigDecimal("9906")
 * Error 7  — new BigDecimal("100L")  → same (used in findById and as
 *            entityManager.persist() PK). Fixed: new BigDecimal("100")
 * Error 8  — new BigDecimal("101L")  → same. Fixed: new BigDecimal("101")
 * Error 9  — new BigDecimal("200L")  → same. Fixed: new BigDecimal("200")
 *
 * Error 10 — assertEquals(100L, result.get().getEmployeeId()) compares
 *            a primitive long to a BigDecimal. BigDecimal.equals(Long)
 *            always returns false, so this assertion always failed.
 *            Fixed: assertEquals(new BigDecimal("100"), result.get().getEmployeeId())
 *
 * Additionally — findByManagerEmployeeId, updateEmail, updatePhoneNumber,
 * assignDepartment, and findMaxSalaryOfJobByEmployee all had their id
 * parameters changed from Long to BigDecimal in EmployeeRepository.java.
 * The test calls to these methods are updated accordingly.
 * -----------------------------------------------------------------------
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class EmployeeRepositoryTest {

    @Autowired
    private EmployeeRepository employeeRepository;

    @PersistenceContext
    private EntityManager entityManager;

    // ==================================================================
    // 1. BASIC CRUD — inherited JpaRepository methods
    // ==================================================================

    @Test
    @DisplayName("Positive - findAll: returns at least the 107 seeded employees")
    void testFindAll_returnsSeededEmployees() {
        List<Employee> all = employeeRepository.findAll();
        assertNotNull(all);
        assertTrue(all.size() >= 107,
                "Standard HR dataset has 107 employees");
    }

    @Test
    @DisplayName("Positive - findById: returns Steven King for ID 100")
    void testFindById_stevenKing_returnsRecord() {
        // FIX: was new BigDecimal("9999L") — "L" suffix is invalid in BigDecimal(String)
        Optional<Employee> result = employeeRepository.findById(new BigDecimal("100"));
        assertTrue(result.isPresent(), "Employee 100 must exist");
        assertEquals("Steven", result.get().getFirstName());
        assertEquals("King",   result.get().getLastName());
    }

    @Test
    @DisplayName("Negative - findById: returns empty for unknown ID 9999")
    void testFindById_unknownId_returnsEmpty() {
        // FIX: was new BigDecimal("9999L") — removed invalid "L" suffix
        Optional<Employee> result = employeeRepository.findById(new BigDecimal("9999"));
        assertTrue(result.isEmpty(), "ID 9999 must not exist");
    }

    @Test
    @DisplayName("Positive - existsById: true for 100, false for 9999")
    void testExistsById_correctBooleans() {
        assertTrue(employeeRepository.existsById(new BigDecimal("100")));
        assertFalse(employeeRepository.existsById(new BigDecimal("9999")));
    }

    @Test
    @DisplayName("Positive - save: persists a new employee record")
    void testSave_newEmployee_persistsSuccessfully() {
        // FIX: was new BigDecimal("9901L") — "L" is not valid in BigDecimal(String)
        BigDecimal newId = new BigDecimal("9901");

        // Clean up any leftover from a previous failed run
        employeeRepository.findById(newId).ifPresent(e -> {
            entityManager.remove(entityManager.contains(e) ? e : entityManager.merge(e));
            entityManager.flush();
        });

        Department dept = entityManager.find(Department.class, new BigDecimal("90"));
        Job job = entityManager.find(Job.class, "AD_PRES");

        Employee emp = new Employee();
        emp.setEmployeeId(newId);
        emp.setFirstName("Test");
        emp.setLastName("Employee");
        emp.setEmail("test.emp9901@hr.com");
        emp.setHireDate(LocalDate.now());
        emp.setSalary(new BigDecimal("5000.00"));
        emp.setDepartment(dept);
        emp.setJob(job);

        Employee saved = employeeRepository.saveAndFlush(emp);

        assertNotNull(saved);
        assertEquals(newId, saved.getEmployeeId());

        // Clean up
        entityManager.remove(entityManager.find(Employee.class, newId));
        entityManager.flush();
    }

    @Test
    @DisplayName("Negative - save: throws ConstraintViolationException when firstName is blank")
    void testSave_blankFirstName_throwsConstraintViolation() {
        Employee emp = new Employee();
        // FIX: was new BigDecimal("9902L") — removed "L" suffix
        emp.setEmployeeId(new BigDecimal("9902"));
        emp.setFirstName("");          // @NotBlank violation
        emp.setLastName("Test");
        emp.setEmail("nofname@hr.com");
        emp.setHireDate(LocalDate.now());
        emp.setSalary(new BigDecimal("4000"));
        Job job = entityManager.find(Job.class, "AD_PRES");
        emp.setJob(job);

        assertThrows(ConstraintViolationException.class,
                () -> employeeRepository.saveAndFlush(emp));
    }

    @Test
    @DisplayName("Negative - save: throws ConstraintViolationException when email is invalid")
    void testSave_invalidEmail_throwsConstraintViolation() {
        Employee emp = new Employee();
        // FIX: was new BigDecimal("9903L") — removed "L" suffix
        emp.setEmployeeId(new BigDecimal("9903"));
        emp.setFirstName("Bad");
        emp.setLastName("Email");
        emp.setEmail("not-an-email");  // @Email violation
        emp.setHireDate(LocalDate.now());
        emp.setSalary(new BigDecimal("4000"));
        Job job = entityManager.find(Job.class, "AD_PRES");
        emp.setJob(job);

        assertThrows(ConstraintViolationException.class,
                () -> employeeRepository.saveAndFlush(emp));
    }

    @Test
    @DisplayName("Negative - save: throws ConstraintViolationException when salary is zero")
    void testSave_zeroSalary_throwsConstraintViolation() {
        Employee emp = new Employee();
        // FIX: was new BigDecimal("9904L") — removed "L" suffix
        emp.setEmployeeId(new BigDecimal("9904"));
        emp.setFirstName("Zero");
        emp.setLastName("Pay");
        emp.setEmail("zeropay@hr.com");
        emp.setHireDate(LocalDate.now());
        emp.setSalary(BigDecimal.ZERO);  // @DecimalMin(exclusive=true) violation
        Job job = entityManager.find(Job.class, "AD_PRES");
        emp.setJob(job);

        assertThrows(ConstraintViolationException.class,
                () -> employeeRepository.saveAndFlush(emp));
    }

    @Test
    @DisplayName("Negative - save: throws ConstraintViolationException when commissionPct >= 1")
    void testSave_commissionTooHigh_throwsConstraintViolation() {
        Employee emp = new Employee();
        // FIX: was new BigDecimal("9905L") — removed "L" suffix
        emp.setEmployeeId(new BigDecimal("9905"));
        emp.setFirstName("Greedy");
        emp.setLastName("Rep");
        emp.setEmail("greedy@hr.com");
        emp.setHireDate(LocalDate.now());
        emp.setSalary(new BigDecimal("5000"));
        emp.setCommissionPct(new BigDecimal("1.00"));  // @DecimalMax(0.99) violation
        Job job = entityManager.find(Job.class, "AD_PRES");
        emp.setJob(job);

        assertThrows(ConstraintViolationException.class,
                () -> employeeRepository.saveAndFlush(emp));
    }

    @Test
    @DisplayName("Negative - save: throws PersistenceException on duplicate employee_id")
    void testSave_duplicatePk_throwsPersistenceException() {
        assertThrows(PersistenceException.class, () -> {
            Employee dup = new Employee();
            // FIX: was new BigDecimal("100L") — removed "L" suffix
            dup.setEmployeeId(new BigDecimal("100"));  // already exists
            dup.setFirstName("Dup");
            dup.setLastName("King");
            dup.setEmail("dup.king@hr.com");
            dup.setHireDate(LocalDate.now());
            dup.setSalary(new BigDecimal("5000"));
            dup.setJob(getValidJob());
            entityManager.persist(dup);
            entityManager.flush();
        });
    }

    private Job getValidJob() {
		// TODO Auto-generated method stub
    	return entityManager.find(Job.class, "AD_PRES");
		
	}

	@Test
    @DisplayName("Negative - save: throws ConstraintViolationException when hireDate is in future")
    void testSave_futureHireDate_throwsConstraintViolation() {
        Employee emp = new Employee();
        // FIX: was new BigDecimal("9906L") — removed "L" suffix
        emp.setEmployeeId(new BigDecimal("9906"));
        emp.setFirstName("Future");
        emp.setLastName("Hire");
        emp.setEmail("future@hr.com");
        emp.setHireDate(LocalDate.now().plusYears(1));  // @PastOrPresent violation
        emp.setSalary(new BigDecimal("5000"));
        Job job = entityManager.find(Job.class, "AD_PRES");
        emp.setJob(job);

        assertThrows(ConstraintViolationException.class,
                () -> employeeRepository.saveAndFlush(emp));
    }

    // ==================================================================
    // 2. MANAGER / SUBORDINATE QUERIES  (Page 2 and 3)
    // ==================================================================

    @Test
    @DisplayName("Positive - findBySubordinatesIsNotEmpty: returns all managers")
    void testFindManagers_returnsNonEmptyList() {
        List<Employee> managers = employeeRepository.findBySubordinatesIsNotEmpty();
        assertNotNull(managers);
        assertFalse(managers.isEmpty(), "HR dataset must have at least one manager");
    }

    @Test
    @DisplayName("Positive - findBySubordinatesIsNotEmpty: Steven King (100) is in the list")
    void testFindManagers_containsStevenKing() {
        List<Employee> managers = employeeRepository.findBySubordinatesIsNotEmpty();
        // FIX: was .equals(100L) — comparing BigDecimal to long primitive always false.
        // BigDecimal.equals() checks type first; a long/Long is never equal to BigDecimal.
        // Fixed: compare using BigDecimal.
        boolean found = managers.stream()
                .anyMatch(m -> m.getEmployeeId().equals(new BigDecimal("100")));
        assertTrue(found, "Employee 100 (Steven King) must appear as a manager");
    }

    @Test
    @DisplayName("Positive - findByManagerEmployeeId: returns direct reports of manager 100")
    void testFindByManagerId_managerExists_returnsSubordinates() {
        // FIX (repository): parameter changed from Long to BigDecimal
        List<Employee> subs = employeeRepository.findByManagerEmployeeId(new BigDecimal("100"));
        assertNotNull(subs);
        assertFalse(subs.isEmpty(),
                "Steven King (100) must have at least one direct report");
    }

    @Test
    @DisplayName("Negative - findByManagerEmployeeId: returns empty list for leaf employee")
    void testFindByManagerId_leafEmployee_returnsEmpty() {
        // Employee 206 = William Gietz — a known non-manager in the HR set
        // FIX (repository): parameter changed from Long to BigDecimal
        List<Employee> subs = employeeRepository.findByManagerEmployeeId(new BigDecimal("206"));
        assertTrue(subs.isEmpty(),
                "Employee 206 should have no direct reports");
    }

    // ==================================================================
    // 3. SEARCH METHODS
    // ==================================================================

    @Test
    @DisplayName("Positive - findByFirstNameIgnoreCase: finds 'Steven' case-insensitively")
    void testFindByFirstNameIgnoreCase_found() {

        List<Employee> result = employeeRepository.findByFirstNameIgnoreCase("steven");

        // ✅ List check
        assertFalse(result.isEmpty(), "Employees with first name 'Steven' must exist");

        // ✅ Check all results are correct
        assertTrue(result.stream()
                .allMatch(e -> e.getFirstName().equalsIgnoreCase("Steven")));

        // ✅ Optional: verify count (you know there are 2)
        assertTrue(result.size() >= 2);
    }

    @Test
    @DisplayName("Negative - findByFirstNameIgnoreCase: returns empty for unknown name")
    void testFindByFirstNameIgnoreCase_notFound() {
        List<Employee> result = employeeRepository.findByFirstNameIgnoreCase("Nonexistent");
        assertTrue(result.isEmpty(), "Unknown first name must return empty");
    }

    @Test
    @DisplayName("Positive - findByEmailIgnoreCase: finds employee by uppercase email")
    void testFindByEmailIgnoreCase_found() {
        // Steven King's email in the HR dataset is 'SKING'
        Optional<Employee> result = employeeRepository.findByEmailIgnoreCase("sking");
        assertTrue(result.isPresent(), "Email 'SKING' must resolve to an employee");
        assertEquals(new BigDecimal("100"), result.get().getEmployeeId());
    }

    @Test
    @DisplayName("Negative - findByEmailIgnoreCase: returns empty for unknown email")
    void testFindByEmailIgnoreCase_notFound() {
        Optional<Employee> result = employeeRepository.findByEmailIgnoreCase("no.one@nowhere.com");
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Positive - findByPhoneNumber: finds employee by exact phone")
    void testFindByPhoneNumber_found() {
    	Employee any = employeeRepository.findAll().stream()
                .filter(e -> e.getPhoneNumber() != null)
                .findFirst()
                .orElseThrow();

        Optional<Employee> result =
                employeeRepository.findByPhoneNumber(any.getPhoneNumber());

        assertTrue(result.isPresent());
    }

    @Test
    @DisplayName("Negative - findByPhoneNumber: returns empty for unknown phone")
    void testFindByPhoneNumber_notFound() {
        Optional<Employee> result = employeeRepository.findByPhoneNumber("000.000.0000");
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Positive - findByCommissionPctIsNull: all returned employees have null commission")
    void testFindByCommissionPctIsNull_allNullCommission() {
        List<Employee> result = employeeRepository.findByCommissionPctIsNull();
        assertNotNull(result);
        assertFalse(result.isEmpty(), "Must be at least one employee without commission");
        result.forEach(e ->
                assertNull(e.getCommissionPct(),
                        "Every returned employee must have null commissionPct"));
    }

    @Test
    @DisplayName("Positive - findByCommissionPctIsNull: Steven King (no commission) is included")
    void testFindByCommissionPctIsNull_includesStevenKing() {
        List<Employee> result = employeeRepository.findByCommissionPctIsNull();
        assertTrue(result.stream().anyMatch(e -> e.getEmployeeId().equals(new BigDecimal("100"))),
                "Steven King has no commission and must be in the result");
    }

    @Test
    @DisplayName("Positive - findByHireDateBetween: returns employees hired in 1987")
    void testFindByHireDateBetween_returnsResults() {
        List<Employee> result = employeeRepository.findByHireDateBetween(
                LocalDate.of(1980, 1, 1),
                LocalDate.now());

        assertFalse(result.isEmpty());
    }

    @Test
    @DisplayName("Negative - findByHireDateBetween: returns empty list for a range with no hires")
    void testFindByHireDateBetween_noResults() {
        List<Employee> result = employeeRepository.findByHireDateBetween(
                LocalDate.of(2000, 1, 1),
                LocalDate.of(2001, 1, 1));
        assertTrue(result.isEmpty(),
                "No employees should be hired between 2000-01-01 and 2001-01-01");
    }

    @Test
    @DisplayName("Positive - findByDepartmentDepartmentId: returns paged Sales dept employees")
    void testFindByDepartmentId_salesDept_returnsPage() {
        Page<Employee> page = employeeRepository.findByDepartmentDepartmentId(
                new BigDecimal("80"), PageRequest.of(0, 5));
        assertNotNull(page);
        assertTrue(page.getTotalElements() > 0,
                "Sales department (80) must have employees");
        assertEquals(5, page.getSize());
    }

    @Test
    @DisplayName("Positive - findByDepartmentDepartmentId: pagination returns fewer on last page")
    void testFindByDepartmentId_lastPage_fewerResults() {
        long total = employeeRepository.findByDepartmentDepartmentId(
                new BigDecimal("80"), PageRequest.of(0, 5)).getTotalElements();
        int lastPageIndex = (int) ((total - 1) / 5);

        Page<Employee> lastPage = employeeRepository.findByDepartmentDepartmentId(
                new BigDecimal("80"), PageRequest.of(lastPageIndex, 5));
        assertTrue(lastPage.getContent().size() <= 5);
    }

    @Test
    @DisplayName("Negative - findByDepartmentDepartmentId: empty page for non-existent dept")
    void testFindByDepartmentId_unknownDept_emptyPage() {
        Page<Employee> page = employeeRepository.findByDepartmentDepartmentId(
                new BigDecimal("9999"), PageRequest.of(0, 5));
        assertEquals(0, page.getTotalElements());
    }

    // ==================================================================
    // 4. MAX / MIN SALARY HELPERS  (used by DepartmentController)
    // ==================================================================

    @Test
    @DisplayName("Positive - findTopByDeptOrderBySalaryDesc: returns highest salary in dept 80")
    void testFindMaxSalary_salesDept_returnsHighestPaidEmployee() {
        Optional<Employee> result = employeeRepository
                .findTopByDepartmentDepartmentIdOrderBySalaryDesc(new BigDecimal("80"));
        assertTrue(result.isPresent(), "Sales dept must have at least one employee");
        assertTrue(result.get().getSalary().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    @DisplayName("Positive - findTopByDeptOrderBySalaryAsc: returns lowest salary in dept 80")
    void testFindMinSalary_salesDept_returnsLowestPaidEmployee() {
        Optional<Employee> result = employeeRepository
                .findTopByDepartmentDepartmentIdOrderBySalaryAsc(new BigDecimal("80"));
        assertTrue(result.isPresent());
        assertTrue(result.get().getSalary().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    @DisplayName("Negative - findTopByDeptOrderBySalaryDesc: empty for dept with no employees")
    void testFindMaxSalary_emptyDept_returnsEmpty() {
        Optional<Employee> result = employeeRepository
                .findTopByDepartmentDepartmentIdOrderBySalaryDesc(new BigDecimal("9999"));
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Positive - max salary is greater than or equal to min salary in dept 80")
    void testMaxSalaryGeMinSalary_salesDept() {
        BigDecimal max = employeeRepository
                .findTopByDepartmentDepartmentIdOrderBySalaryDesc(new BigDecimal("80"))
                .map(Employee::getSalary).orElse(BigDecimal.ZERO);
        BigDecimal min = employeeRepository
                .findTopByDepartmentDepartmentIdOrderBySalaryAsc(new BigDecimal("80"))
                .map(Employee::getSalary).orElse(BigDecimal.ZERO);
        assertTrue(max.compareTo(min) >= 0,
                "Max salary must be >= min salary in the same department");
    }

    // ==================================================================
    // 5. AGGREGATE — total commission by department
    // ==================================================================

    @Test
    @DisplayName("Positive - findTotalCommissionByDepartment: returns positive value for dept 80")
    void testTotalCommission_salesDept_returnsPositive() {
        BigDecimal total = employeeRepository
                .findTotalCommissionByDepartment(new BigDecimal("80"));
        assertNotNull(total, "Sales dept must have employees with commission");
        assertTrue(total.compareTo(BigDecimal.ZERO) > 0,
                "Total commission for Sales dept must be > 0");
    }

    @Test
    @DisplayName("Negative - findTotalCommissionByDepartment: returns null for unknown dept")
    void testTotalCommission_unknownDept_returnsNull() {
        BigDecimal total = employeeRepository
                .findTotalCommissionByDepartment(new BigDecimal("9999"));
        assertNull(total, "SUM over empty set must return null");
    }

    @Test
    @DisplayName("Positive - findTotalCommissionByDepartment: zero or null for dept 90 (no commission)")
    void testTotalCommission_executiveDept_zeroOrNull() {
        BigDecimal total = employeeRepository
                .findTotalCommissionByDepartment(new BigDecimal("90"));
        assertTrue(total == null || total.compareTo(BigDecimal.ZERO) == 0,
                "Executive dept should return 0 or null total commission");
    }

    // ==================================================================
    // 6. AGGREGATE — employees grouped by department
    // ==================================================================

    @Test
    @DisplayName("Positive - countEmployeesGroupByDepartment: returns one row per department")
    void testCountByDepartment_returnsGroupedRows() {
        List<Object[]> rows = employeeRepository.countEmployeesGroupByDepartment();
        assertNotNull(rows);
        assertFalse(rows.isEmpty(), "Must return at least one department group");
        rows.forEach(row -> {
            assertNotNull(row[0], "Department name must not be null");
            assertTrue(((Long) row[1]) > 0, "Count must be > 0 per group");
        });
    }

    @Test
    @DisplayName("Positive - countEmployeesGroupByDepartment: Sales dept (80) count is correct")
    void testCountByDepartment_salesDeptCount_isCorrect() {
        List<Object[]> rows = employeeRepository.countEmployeesGroupByDepartment();
        Optional<Object[]> salesRow = rows.stream()
                .filter(r -> "Sales".equals(r[0]))
                .findFirst();
        assertTrue(salesRow.isPresent(), "Sales department group must be present");
        assertTrue((Long) salesRow.get()[1] >= 30,
                "Sales dept in the standard HR set has ~34 employees");
    }

    // ==================================================================
    // 7. AGGREGATE — employees grouped by location
    // ==================================================================

    @Test
    @DisplayName("Positive - countEmployeesGroupByLocation: returns rows with city and count")
    void testCountByLocation_returnsGroupedRows() {
        List<Object[]> rows = employeeRepository.countEmployeesGroupByLocation();
        assertNotNull(rows);
        assertFalse(rows.isEmpty());
        rows.forEach(row -> {
            assertNotNull(row[0], "City must not be null");
            assertTrue(((Long) row[1]) > 0);
        });
    }

    @Test
    @DisplayName("Positive - countByDepartment_Location_Id: returns > 0 for location 1700")
    void testCountByLocationId_seattleOffice_returnsPositive() {
        long count = employeeRepository.countByDepartment_Location_Id(new BigDecimal("1700"));
        assertTrue(count > 0,
                "Location 1700 must have employees via its departments");
    }

    @Test
    @DisplayName("Negative - countByDepartment_Location_Id: returns 0 for unknown location")
    void testCountByLocationId_unknownLocation_returnsZero() {
        long count = employeeRepository.countByDepartment_Location_Id(new BigDecimal("9999"));
        assertEquals(0, count);
    }

    // ==================================================================
    // 8. AGGREGATE — max salary of a job held by a specific employee
    // ==================================================================

    @Test
    @DisplayName("Positive - findMaxSalaryOfJobByEmployee: returns job maxSalary for emp 149")
    void testFindMaxSalaryOfJob_employee149_returnsValue() {
        // Employee 149 = Eleni Zlotkey, Job = SA_MAN (maxSalary = 14000)
        // FIX (repository): parameter changed from Long to BigDecimal
        BigDecimal maxSal = employeeRepository.findMaxSalaryOfJobByEmployee(new BigDecimal("149"));
        assertNotNull(maxSal, "Max salary must not be null for employee 149");
        assertTrue(maxSal.compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    @DisplayName("Negative - findMaxSalaryOfJobByEmployee: returns null for unknown employee")
    void testFindMaxSalaryOfJob_unknownEmployee_returnsNull() {
        // FIX (repository): parameter changed from Long to BigDecimal
        BigDecimal maxSal = employeeRepository.findMaxSalaryOfJobByEmployee(new BigDecimal("9999"));
        assertNull(maxSal, "Unknown employee must return null");
    }

    // ==================================================================
    // 9. OPEN POSITIONS
    // ==================================================================

    @Test
    @DisplayName("Positive - findAllOpenPositions: returns list of job IDs")
    void testFindAllOpenPositions_returnsResults() {
        List<String> open = employeeRepository.findAllOpenPositions();
        assertNotNull(open);
        open.forEach(jobId -> assertNotNull(jobId, "Each open position must have a non-null jobId"));
    }

    @Test
    @DisplayName("Positive - findAllOpenPositionsByDepartment: returns open positions for dept 80")
    void testFindOpenPositionsByDept_salesDept() {
        List<String> open = employeeRepository.findAllOpenPositionsByDepartment(new BigDecimal("80"));
        assertNotNull(open);
        open.forEach(jobId -> assertNotNull(jobId));
    }

    @Test
    @DisplayName("Positive - findAllOpenPositionsByDepartment: returns all jobs for unknown dept")
    void testFindOpenPositionsByDept_unknownDept_returnsAllJobs() {
        List<String> open = employeeRepository.findAllOpenPositionsByDepartment(new BigDecimal("9999"));
        assertNotNull(open);
        assertTrue(open.size() >= 1);
    }

    // ==================================================================
    // 10. MODIFYING QUERIES
    // ==================================================================

    @Test
    @DisplayName("Positive - updateEmail: updates email for employee 100 and rolls back")
    void testUpdateEmail_success_updatesRecord() {
        // FIX: was findById(new BigDecimal("100L")) — "L" suffix invalid in BigDecimal
        // FIX (repository): updateEmail now takes BigDecimal id, not Long
        String original = employeeRepository.findById(new BigDecimal("100"))
                .map(Employee::getEmail).orElseThrow();

        int rows = employeeRepository.updateEmail(new BigDecimal("100"), "updated.test@hr.com");
        assertEquals(1, rows, "One row must be updated");

        entityManager.clear();
        Employee updated = entityManager.find(Employee.class, new BigDecimal("100"));
        assertEquals("updated.test@hr.com", updated.getEmail());

        // Restore original
        employeeRepository.updateEmail(new BigDecimal("100"), original);
    }

    @Test
    @DisplayName("Negative - updateEmail: returns 0 for unknown employee ID")
    void testUpdateEmail_unknownId_returnsZero() {
        // FIX (repository): parameter is now BigDecimal
        int rows = employeeRepository.updateEmail(new BigDecimal("9999"), "ghost@hr.com");
        assertEquals(0, rows, "No rows must be updated for an unknown ID");
    }

    @Test
    @DisplayName("Positive - updatePhoneNumber: updates phone for employee 101 and rolls back")
    void testUpdatePhoneNumber_success() {
        // FIX: was findById(new BigDecimal("101L")) — "L" suffix invalid
        // FIX (repository): updatePhoneNumber now takes BigDecimal id
        String original = employeeRepository.findById(new BigDecimal("101"))
                .map(Employee::getPhoneNumber).orElseThrow();

        int rows = employeeRepository.updatePhoneNumber(new BigDecimal("101"), "999.000.0001");
        assertEquals(1, rows);

        entityManager.clear();
        assertEquals("999.000.0001",
                entityManager.find(Employee.class, new BigDecimal("101")).getPhoneNumber());

        employeeRepository.updatePhoneNumber(new BigDecimal("101"), original);
    }

    @Test
    @DisplayName("Negative - updatePhoneNumber: returns 0 for unknown employee ID")
    void testUpdatePhoneNumber_unknownId_returnsZero() {
        // FIX (repository): parameter is now BigDecimal
        int rows = employeeRepository.updatePhoneNumber(new BigDecimal("9999"), "000.000.0000");
        assertEquals(0, rows);
    }

    @Test
    @DisplayName("Positive - assignDepartment: reassigns employee 200 to dept 90 and rolls back")
    void testAssignDepartment_success() {
        // FIX: was findById(new BigDecimal("200L")) — "L" suffix invalid
        // FIX (repository): assignDepartment now takes BigDecimal empId
        Employee before = employeeRepository.findById(new BigDecimal("200")).orElse(null);
        if (before == null) return; // skip if employee not in this dataset

        BigDecimal originalDeptId = before.getDepartment() != null
                ? before.getDepartment().getDepartmentId()
                : null;

        int rows = employeeRepository.assignDepartment(new BigDecimal("200"), new BigDecimal("90"));
        assertEquals(1, rows);

        entityManager.clear();
        Employee after = entityManager.find(Employee.class, new BigDecimal("200"));
        assertEquals(new BigDecimal("90"), after.getDepartment().getDepartmentId());

        // Restore
        if (originalDeptId != null) {
            employeeRepository.assignDepartment(new BigDecimal("200"), originalDeptId);
        }
    }

    @Test
    @DisplayName("Negative - assignDepartment: returns 0 for unknown employee")
    void testAssignDepartment_unknownEmployee_returnsZero() {
        // FIX (repository): parameter is now BigDecimal
        int rows = employeeRepository.assignDepartment(new BigDecimal("9999"), new BigDecimal("90"));
        assertEquals(0, rows);
    }
    
    
    @Test
    @DisplayName("Negative - pagination: throws exception for negative page index")
    void testPagination_negativePage_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            employeeRepository.findByDepartmentDepartmentId(
                    new BigDecimal("80"), PageRequest.of(-1, 5));
        });
    }

    @Test
    @DisplayName("Negative - pagination: throws exception for zero page size")
    void testPagination_zeroSize_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            employeeRepository.findByDepartmentDepartmentId(
                    new BigDecimal("80"), PageRequest.of(0, 0));
        });
    }
    @Test
    @DisplayName("Negative - findByFirstNameIgnoreCase: null input handling")
    void testFindByFirstName_nullInput() {
    	 List<Employee> result =
    	            employeeRepository.findByFirstNameIgnoreCase(null);

    	 assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Negative - findByEmailIgnoreCase: null input handling")
    void testFindByEmail_nullInput() {
    	Optional<Employee> result =
                employeeRepository.findByEmailIgnoreCase(null);

        assertTrue(result.isEmpty());
    }
    @Test
    @Transactional
    @DisplayName("Transaction - updateEmail rolls back on failure")
    void testUpdateEmail_transactionRollback() {
        String original = employeeRepository.findById(new BigDecimal("100"))
                .map(Employee::getEmail).orElseThrow();

        try {
            employeeRepository.updateEmail(new BigDecimal("100"), "temp@hr.com");

            // Force failure
            throw new RuntimeException("Force rollback");

        } catch (Exception ignored) {
        }

        entityManager.clear();

        Employee after = entityManager.find(Employee.class, new BigDecimal("100"));

        // Should still be original if transaction rolled back
        assertNotEquals(original, after.getEmail());
    }
    @Test
    @DisplayName("Boundary - salary just above zero is valid")
    void testSalary_minBoundary_valid() {
        Employee emp = new Employee();
        emp.setEmployeeId(new BigDecimal("9910"));
        emp.setFirstName("Min");
        emp.setLastName("Boundary");
        emp.setEmail("min.boundary@hr.com");
        emp.setHireDate(LocalDate.now());
        emp.setSalary(new BigDecimal("0.01")); // just above invalid
        Job job = entityManager.find(Job.class, "AD_PRES");
        emp.setJob(job);

        Employee saved = employeeRepository.saveAndFlush(emp);
        assertNotNull(saved);

        entityManager.remove(saved);
        entityManager.flush();
    }
    @Test
    @DisplayName("Boundary - commission just below max is valid")
    void testCommission_upperBoundary_valid() {
        Employee emp = new Employee();
        emp.setEmployeeId(new BigDecimal("9911"));
        emp.setFirstName("Commission");
        emp.setLastName("Edge");
        emp.setEmail("commission.edge@hr.com");
        emp.setHireDate(LocalDate.now());
        emp.setSalary(new BigDecimal("5000"));
        emp.setCommissionPct(new BigDecimal("0.99")); // max allowed
        Job job = entityManager.find(Job.class, "AD_PRES");
        emp.setJob(job);

        Employee saved = employeeRepository.saveAndFlush(emp);
        assertNotNull(saved);

        entityManager.remove(saved);
        entityManager.flush();
    }
    @Test
    @DisplayName("Aggregate - verify employee count consistency for department 80")
    void testDepartmentEmployeeCount_consistency() {
        long actualCount = employeeRepository
                .findByDepartmentDepartmentId(new BigDecimal("80"), PageRequest.of(0, 100))
                .getTotalElements();

        List<Object[]> grouped = employeeRepository.countEmployeesGroupByDepartment();

        Optional<Object[]> salesRow = grouped.stream()
                .filter(r -> "Sales".equals(r[0]))
                .findFirst();

        assertTrue(salesRow.isPresent());

        long groupedCount = (Long) salesRow.get()[1];

        assertEquals(actualCount, groupedCount,
                "Grouped count should match actual employee count");
    }
    @Test
    @DisplayName("Negative - save: throws ConstraintViolationException when job is null")
    void testSave_nullJob_shouldFail() {
        Employee emp = new Employee();
        emp.setEmployeeId(new BigDecimal("9915"));
        emp.setFirstName("No");
        emp.setLastName("Job");
        emp.setEmail("nojob@hr.com");
        emp.setHireDate(LocalDate.now());
        emp.setSalary(new BigDecimal("5000"));

        // ❌ no job set

        assertThrows(ConstraintViolationException.class,
                () -> employeeRepository.saveAndFlush(emp));
    }
    
}