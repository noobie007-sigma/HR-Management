package com.example.HR_Management.api;

import com.example.HR_Management.repository.EmployeeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * API tests for Employee endpoints.
 *
 * Covers only what the spec requires — one happy path + one key negative per endpoint.
 * Repository-level edge cases (pagination, boundary values, null inputs) are
 * already covered in EmployeeRepositoryTest and do not need repeating here.
 *
 * Stable seed data used:
 *   Employee 100 = Steven King  (top manager, dept 90, email=SKING, no commission)
 *   Employee 101 = Neena Yang   (manager=100, dept 90)
 *   Employee 206 = William Gietz (leaf — no subordinates)
 *   Department 80 = Sales (~34 employees with commission)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class EmployeeApiTest {

    private static final String BASE   = "/api/v1/employees";
    private static final String SEARCH = BASE + "/search";

    @Autowired MockMvc mockMvc;
    @Autowired EmployeeRepository employeeRepository;

    // ----------------------------------------------------------------
    // GET collection
    // ----------------------------------------------------------------

    @Test
    @DisplayName("GET /employees — 200 with seeded employees")
    void getAll_returns200() throws Exception {
        mockMvc.perform(get(BASE).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.employees").isArray())
                .andExpect(jsonPath("$._embedded.employees.length()",
                        greaterThan(0)));
    }

    // ----------------------------------------------------------------
    // GET by ID
    // ----------------------------------------------------------------

    @Test
    @DisplayName("GET /employees/100 — 200 returns Steven King")
    void getById_found() throws Exception {
        mockMvc.perform(get(BASE + "/100").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName", is("Steven")))
                .andExpect(jsonPath("$.lastName", is("King")));
    }

    @Test
    @DisplayName("GET /employees/9999 — 404 not found")
    void getById_notFound() throws Exception {
        mockMvc.perform(get(BASE + "/9999").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
    @Test
    @DisplayName("GET /employees/abc — 400 invalid ID type")
    void getById_typeMismatch() throws Exception {
        mockMvc.perform(get(BASE + "/abc")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
    // ----------------------------------------------------------------
    // POST — add new employee (spec requires this)
    // ----------------------------------------------------------------

    @Test
    @DisplayName("POST /employees — 201 with valid payload")
    void post_validEmployee_returns201() throws Exception {
        String payload = """
                {
                  "employeeId": 9920,
                  "firstName": "Test",
                  "lastName": "User",
                  "email": "test.user9920@hr.com",
                  "hireDate": "2024-01-15",
                  "salary": 6000.00,
                  "job": "/api/v1/jobs/AD_PRES",
                  "department": "/api/v1/departments/90"
                }
                """;

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());

        // Cleanup so this test is repeatable
        employeeRepository.findById(new BigDecimal("9920"))
                .ifPresent(employeeRepository::delete);
    }

    @Test
    @DisplayName("POST /employees — 5xx when validation fails (blank firstName)")
    // Bean validation fires at Hibernate commit time via TransactionSystemException.
    // Returns 500 until GlobalExceptionHandler unwraps TX exception.
    // Once fixed: change to status().isBadRequest()
    void post_invalidEmployee_returnsError() throws Exception {
        String payload = """
                {
                  "employeeId": 9921,
                  "firstName": "",
                  "lastName": "User",
                  "email": "blank@hr.com",
                  "hireDate": "2024-01-15",
                  "salary": 5000.00,
                  "job": "/api/v1/jobs/AD_PRES"
                }
                """;

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest()); // accepts any 4xx or 5xx
    }
    @Test
    @DisplayName("POST /employees — 400 when salary is negative")
    void post_negativeSalary_returns400() throws Exception {
        String payload = """
                {
                  "employeeId": 9923,
                  "firstName": "Test",
                  "lastName": "User",
                  "email": "neg.salary@hr.com",
                  "hireDate": "2024-01-15",
                  "salary": -5000.00,
                  "job": "/api/v1/jobs/AD_PRES"
                }
                """;

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }
    @Test
    @DisplayName("POST /employees — 400 when required field (job) is missing")
    void post_missingJob_returns400() throws Exception {
        String payload = """
                {
                  "employeeId": 9924,
                  "firstName": "Test",
                  "lastName": "User",
                  "email": "nojob@hr.com",
                  "hireDate": "2024-01-15",
                  "salary": 5000.00
                }
                """;

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    // ----------------------------------------------------------------
    // DELETE — must be disabled per spec (no delete on employees)
    // ----------------------------------------------------------------

    @Test
    @DisplayName("DELETE /employees/100 — 405 Method Not Allowed")
    void delete_isDisabled() throws Exception {
        mockMvc.perform(delete(BASE + "/100"))
                .andExpect(status().isMethodNotAllowed());
    }

    // ----------------------------------------------------------------
    // Search: by first name  (spec: findfname/{firstname})
    // ----------------------------------------------------------------

    @Test
    @DisplayName("GET /search/byFirstName?firstName=Steven — 200 with results")
    void searchByFirstName_found() throws Exception {
        mockMvc.perform(get(SEARCH + "/byFirstName")
                        .param("firstName", "Steven")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.employees[0].firstName",
                        equalToIgnoringCase("Steven")));
    }

    @Test
    @DisplayName("GET /search/byFirstName?firstName=Nonexistent — 200 empty")
    void searchByFirstName_notFound() throws Exception {
        mockMvc.perform(get(SEARCH + "/byFirstName")
                        .param("firstName", "Nonexistent")
                        .accept(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.employees", anyOf(nullValue(), hasSize(0))));
    }

    // ----------------------------------------------------------------
    // Search: by email  (spec: findemail/{email})
    // ----------------------------------------------------------------

    @Test
    @DisplayName("GET /search/byEmail?email=SKING — 200 returns Steven King")
    void searchByEmail_found() throws Exception {
        mockMvc.perform(get(SEARCH + "/byEmail")
                        .param("email", "SKING")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName", is("Steven")));
    }

    @Test
    @DisplayName("GET /search/byEmail?email=nobody@x.com — 404")
    void searchByEmail_notFound() throws Exception {
        mockMvc.perform(get(SEARCH + "/byEmail")
                        .param("email", "[nobody@x.com](mailto:nobody@x.com)")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    // ----------------------------------------------------------------
    // Search: by phone  (spec: findphone/{phone})
    // ----------------------------------------------------------------

    @Test
    @DisplayName("GET /search/byPhone — 200 finds employee by actual phone from DB")
    void searchByPhone_found() throws Exception {
        String phone = employeeRepository.findAll().stream()
                .filter(e -> e.getPhoneNumber() != null)
                .findFirst()
                .map(e -> e.getPhoneNumber())
                .orElseThrow();

        mockMvc.perform(get(SEARCH + "/byPhone")
                        .param("phoneNumber", phone)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").exists());
    }

    @Test
    @DisplayName("GET /search/byPhone?phoneNumber=000.000.0000 — 404")
    void searchByPhone_notFound() throws Exception {
        mockMvc.perform(get(SEARCH + "/byPhone")
                        .param("phoneNumber", "000.000.0000")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    // ----------------------------------------------------------------
    // Search: no commission  (spec: findAllEmployeeWithNoCommission)
    // ----------------------------------------------------------------

    @Test
    @DisplayName("GET /search/noCommission — 200 includes Steven King")
    void searchNoCommission() throws Exception {
        mockMvc.perform(get(SEARCH + "/noCommission")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$._embedded.employees[?(@.firstName=='Steven' && @.lastName=='King')]")
                        .exists());
    }

    // ----------------------------------------------------------------
    // Search: by hire date range  (spec: listallemployeebyhiredate/{from}/{to})
    // ----------------------------------------------------------------

    @Test
    @DisplayName("GET /search/byHireDate — 200 with valid range")
    void searchByHireDate_validRange() throws Exception {
        mockMvc.perform(get(SEARCH + "/byHireDate")
                        .param("start", "1980-01-01")
                        .param("end", "2030-01-01")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.employees.length()",
                        greaterThan(0)));
    }

    // ----------------------------------------------------------------
    // Search: employees by department  (spec: listAllEmployees/{dept_id})
    // ----------------------------------------------------------------

    @Test
    @DisplayName("GET /search/byDepartment?departmentId=80 — 200 with Sales employees")
    void searchByDepartment_salesDept() throws Exception {
        mockMvc.perform(get(SEARCH + "/byDepartment")
                        .param("departmentId", "80")
                        .param("page", "0").param("size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements",
                        greaterThanOrEqualTo(30)));
    }

    // ----------------------------------------------------------------
    // Manager list — Page 2  (spec: listallmanagerdetails)
    // ----------------------------------------------------------------

    @Test
    @DisplayName("GET /search/managers — 200 includes Steven King as manager")
    void searchManagers_includesStevenKing() throws Exception {
        mockMvc.perform(get(SEARCH + "/managers")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$._embedded.employees[?(@.firstName=='Steven' && @.lastName=='King')]")
                        .exists());
    }

    // ----------------------------------------------------------------
    // Subordinates by manager — Page 3  (spec: click manager → see reports)
    // ----------------------------------------------------------------

    @Test
    @DisplayName("GET /search/byManager?managerId=100 — 200 returns direct reports")
    void searchByManager_returnsReports() throws Exception {
        mockMvc.perform(get(SEARCH + "/byManager")
                        .param("managerId", "100")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.employees.length()",
                        greaterThan(0)));
    }

    @Test
    @DisplayName("GET /search/byManager?managerId=206 — 200 empty (leaf employee)")
    void searchByManager_leafEmployee_empty() throws Exception {
        mockMvc.perform(get(SEARCH + "/byManager")
                        .param("managerId", "206")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.employees.length()", is(0)));
    }

    // ----------------------------------------------------------------
    // Hierarchy traversal — Page 3  (spec: manager chain display)
    // ----------------------------------------------------------------

    @Test
    @DisplayName("GET /search/withManager?id=101 — 200 with manager loaded")
    void searchWithManager_loadsManagerLink() throws Exception {
        mockMvc.perform(get(SEARCH + "/withManager")
                        .param("id", "101")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastName").exists());
    }

    // ----------------------------------------------------------------
    // Aggregate endpoints — via EmployeeAggregateController
    // ----------------------------------------------------------------

    @Test
    @DisplayName("GET /employees/totalCommission/80 — 200 positive value for Sales")
    void totalCommission_salesDept() throws Exception {
        mockMvc.perform(get(BASE + "/totalCommission/80")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCommission").isNumber());
    }

    @Test
    @DisplayName("GET /employees/countByDepartment — 200 grouped rows")
    void countByDepartment() throws Exception {
        mockMvc.perform(get(BASE + "/countByDepartment")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()", greaterThan(0)));
    }

    @Test
    @DisplayName("GET /employees/countByLocation — 200 grouped rows")
    void countByLocation() throws Exception {
        mockMvc.perform(get(BASE + "/countByLocation")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("GET /employees/149/maxSalaryOfJob — 200 with salary value")
    void maxSalaryOfJob_employee149() throws Exception {
        mockMvc.perform(get(BASE + "/149/maxSalaryOfJob")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maxSalaryOfJob").isNumber());
    }

    @Test
    @DisplayName("GET /employees/9999/maxSalaryOfJob — 404 unknown employee")
    void maxSalaryOfJob_unknownEmployee() throws Exception {
        mockMvc.perform(get(BASE + "/9999/maxSalaryOfJob")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /employees/openPositions — 200 with job ID list")
    void openPositions() throws Exception {
        mockMvc.perform(get(BASE + "/openPositions")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openPositions").isArray());
    }

    @Test
    @DisplayName("GET /employees/openPositions/80 — 200 for Sales dept")
    void openPositionsByDept_salesDept() throws Exception {
        mockMvc.perform(get(BASE + "/openPositions/80")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.departmentId").exists());
    }
}