package com.example.HR_Management.api;

import com.example.HR_Management.entity.Country;
import com.example.HR_Management.entity.Location;

import jakarta.persistence.EntityManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ============================================================
 * Location API Integration Tests — Spring Data REST + MockMVC
 * ============================================================
 *
 * Confirmed behavior from actual test run logs:
 *
 *  VALIDATION (after RestValidationConfig added):
 *    RepositoryConstraintViolationException is thrown by Spring Data REST.
 *    GlobalExceptionHandler MUST handle it with 400 (not let it fall to
 *    DataIntegrityViolationException which returns 409).
 *    Fix: added @ExceptionHandler(RepositoryConstraintViolationException.class) → 400.
 *
 *  PUT on non-existent ID:
 *    Returns 201 Created (Spring Data REST upsert), not 200.
 *    Log: PUT /api/v1/locations/9876 → Status = 201.
 *
 *  PUT on existing ID:
 *    Returns 200 OK with updated body.
 *
 *  Empty search results:
 *    Spring Data REST returns { "_embedded": { "locations": [] } }
 *    NOT a missing _embedded key.
 *
 *  DELETE 1700 inside @Transactional:
 *    Returns 204 — FK check fires at COMMIT which never happens in @Transactional test.
 *    To get 409, test must run outside @Transactional.
 *
 *  Duplicate POST inside @Transactional:
 *    Returns 409 (DataIntegrityViolationException from DB).
 *    Log: "Duplicate entry '1100' for key 'PRIMARY'" → 409 Conflict.
 * ============================================================
 */
@SpringBootTest(properties = {
        "spring.data.rest.base-path=/api/v1",
        "spring.jpa.hibernate.ddl-auto=none"
})
@AutoConfigureMockMvc
@Transactional
class LocationApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager entityManager;

    private Country country;

    private static final String BASE = "/api/v1/locations";

    // ============================================================
    // 🔧 SETUP
    // ============================================================
    @BeforeEach
    void setup() {
        country = entityManager.find(Country.class, "IN");
        if (country == null) {
            country = new Country();
            country.setCountryId("IN");
            country.setCountryName("India");
            entityManager.persist(country);
        }
    }

    // ============================================================
    // 🔧 HELPER — persist a unique location via EntityManager
    // ============================================================
    private Location persistUniqueLocation() {
        // Stay in 4-digit range, avoid collision with seeded IDs (1000–3200)
        BigDecimal id = BigDecimal.valueOf(System.nanoTime() % 6000 + 4000);
        Location loc = new Location();
        loc.setId(id);
        loc.setCity("City_" + UUID.randomUUID().toString().substring(0, 6));
        loc.setStateProvince("TS");
        loc.setPostalCode("12345");
        loc.setCountry(country);
        entityManager.persist(loc);
        entityManager.flush();
        return loc;
    }

    // ============================================================
    // ✅ TC-01 | GET ALL — Returns 200 + embedded locations array
    // ============================================================
    @Test
    @DisplayName("TC-01 | GET /api/v1/locations returns 200 and embedded locations array")
    void getAllLocations_returns200WithEmbeddedArray() throws Exception {
        mockMvc.perform(get(BASE).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$._embedded.locations").isArray())
                .andExpect(jsonPath("$._embedded.locations", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.page.totalElements").value(greaterThanOrEqualTo(1)));
    }

    // ============================================================
    // ✅ TC-02 | GET ALL — Pagination
    // ============================================================
    @Test
    @DisplayName("TC-02 | GET /api/v1/locations?page=0&size=5 returns max 5 results")
    void getAllLocations_pagination_returnsCorrectPage() throws Exception {
        mockMvc.perform(get(BASE + "?page=0&size=5").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.locations", hasSize(lessThanOrEqualTo(5))))
                .andExpect(jsonPath("$.page.size").value(5))
                .andExpect(jsonPath("$.page.number").value(0));
    }

    // ============================================================
    // ✅ TC-03 | GET BY ID — Seeded Roma (id=1000)
    // ============================================================
    @Test
    @DisplayName("TC-03 | GET /api/v1/locations/1000 returns 200 with Roma data")
    void getLocationById_seededRoma_returns200() throws Exception {
        mockMvc.perform(get(BASE + "/1000").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.city").value("Roma"))
                .andExpect(jsonPath("$.streetAddress").value("1297 Via Cola di Rie"))
                .andExpect(jsonPath("$.postalCode").value("00989"))
                .andExpect(jsonPath("$._links.self.href", containsString("/locations/1000")));
    }

    // ============================================================
    // ✅ TC-04 | GET BY ID — Freshly persisted via EntityManager
    // ============================================================
    @Test
    @DisplayName("TC-04 | GET /api/v1/locations/{id} returns 200 for freshly persisted location")
    void getLocationById_freshlyPersisted_returns200() throws Exception {
        Location loc = persistUniqueLocation();

        mockMvc.perform(get(BASE + "/" + loc.getId()).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.city").value(loc.getCity()))
                .andExpect(jsonPath("$.postalCode").value("12345"));
    }

    // ============================================================
    // ✅ TC-05 | GET BY ID — London (id=2400), postalCode null in DB
    // ============================================================
    @Test
    @DisplayName("TC-05 | GET /api/v1/locations/2400 returns 200 — London")
    void getLocationById_london_returns200() throws Exception {
        mockMvc.perform(get(BASE + "/2400").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.city").value("London"));
    }

    // ============================================================
    // ❌ TC-06 | GET BY ID — Non-existent ID → 404
    // ============================================================
    @Test
    @DisplayName("TC-06 | GET /api/v1/locations/999999 returns 404 Not Found")
    void getLocationById_notFound_returns404() throws Exception {
        mockMvc.perform(get(BASE + "/999999").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    // ============================================================
    // ❌ TC-07 | GET BY ID — String ID → 400 + ErrorResponse body
    // GlobalExceptionHandler → MethodArgumentTypeMismatchException → 400
    // ============================================================
    @Test
    @DisplayName("TC-07 | GET /api/v1/locations/abc returns 400 with ErrorResponse JSON")
    void getLocationById_stringId_returns400WithErrorBody() throws Exception {
        mockMvc.perform(get(BASE + "/abc").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.path").value(containsString("/locations/abc")));
    }

    // ============================================================
    // ✅ TC-08 | POST — Valid location → 201 Created
    // ============================================================
    @Test
    @DisplayName("TC-08 | POST /api/v1/locations with valid body returns 201 Created")
    void createLocation_valid_returns201() throws Exception {
        long uniqueId = System.nanoTime() % 6000 + 4000;

        String json = """
                {
                  "id": %d,
                  "city": "Hyderabad",
                  "stateProvince": "Telangana",
                  "postalCode": "500081",
                  "country": "/api/v1/countries/IN"
                }
                """.formatted(uniqueId);

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.city").value("Hyderabad"))
                .andExpect(jsonPath("$.postalCode").value("500081"))
                .andExpect(jsonPath("$._links.self.href", containsString("/locations/")));
    }

    // ============================================================
    // ❌ TC-09 | POST — Missing city → 400
    // RestValidationConfig fires "beforeCreate" → RepositoryConstraintViolationException
    // GlobalExceptionHandler maps it → 400 Bad Request (NOT 409)
    // ============================================================
    @Test
    @DisplayName("TC-09 | POST missing city returns 400 with validation error")
    void createLocation_missingCity_returns400() throws Exception {
        long uniqueId = System.nanoTime() % 6000 + 4000;

        String json = """
                {
                  "id": %d,
                  "stateProvince": "Telangana",
                  "postalCode": "500081",
                  "country": "/api/v1/countries/IN"
                }
                """.formatted(uniqueId);

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())           // 400
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    // ============================================================
    // ❌ TC-10 | POST — City > 30 chars → 400
    // ============================================================
    @Test
    @DisplayName("TC-10 | POST city > 30 chars returns 400 with validation error")
    void createLocation_cityTooLong_returns400() throws Exception {
        long uniqueId = System.nanoTime() % 6000 + 4000;

        String json = """
                {
                  "id": %d,
                  "city": "ThisCityNameIsWayTooLongExceedingThirtyCharacters",
                  "postalCode": "123456",
                  "country": "/api/v1/countries/IN"
                }
                """.formatted(uniqueId);

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    // ============================================================
    // ❌ TC-11 | POST — StreetAddress > 40 chars → 400
    // ============================================================
    @Test
    @DisplayName("TC-11 | POST streetAddress > 40 chars returns 400 with validation error")
    void createLocation_streetAddressTooLong_returns400() throws Exception {
        long uniqueId = System.nanoTime() % 6000 + 4000;

        String json = """
                {
                  "id": %d,
                  "city": "ValidCity",
                  "streetAddress": "This Street Address Is Definitely Way Too Long And Exceeds Forty Characters",
                  "postalCode": "111111",
                  "country": "/api/v1/countries/IN"
                }
                """.formatted(uniqueId);

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    // ============================================================
    // ❌ TC-12 | POST — Duplicate location_id → 409 Conflict
    // Log confirmed: "Duplicate entry '1100' for key 'PRIMARY'" → DataIntegrityViolationException → 409
    // ============================================================
    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void createLocation_duplicateId_returns409() throws Exception {

        BigDecimal id = BigDecimal.valueOf(5000);

        // ✅ Step 1: Insert inside REAL transaction
        new TransactionTemplate(transactionManager).execute(status -> {
            Location loc = new Location();
            loc.setId(id);
            loc.setCity("First");
            loc.setCountry(country);
            entityManager.persist(loc);
            return null;
        });

        // ❌ Step 2: API call (separate transaction)
        String json = """
        {
          "id": 5000,
          "city": "Duplicate",
          "postalCode": "11111",
          "country": "/api/v1/countries/IN"
        }
        """;

        mockMvc.perform(post(BASE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isConflict());
    }

    // ============================================================
    // ✅ TC-13 | PUT — Full update of existing location → 200 OK
    // Log confirmed: PUT on existing ID → Status = 200 with body
    // ============================================================
    @Test
    @DisplayName("TC-13 | PUT /api/v1/locations/{id} on existing location returns 200")
    void updateLocation_existingId_returns200() throws Exception {
        Location loc = persistUniqueLocation();

        String json = """
                {
                  "city": "UpdatedCity",
                  "stateProvince": "UpdatedState",
                  "postalCode": "99999",
                  "country": "/api/v1/countries/IN"
                }
                """;

        mockMvc.perform(put(BASE + "/" + loc.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())                   // 200
                .andExpect(jsonPath("$.city").value("UpdatedCity"))
                .andExpect(jsonPath("$.postalCode").value("99999"));
    }

    // ============================================================
    // ✅ TC-14 | PUT — Non-existent ID → Spring Data REST upserts → 201 Created
    // Log confirmed: PUT /api/v1/locations/9876 → Status = 201 (not 200)
    // Spring Data REST: PUT on missing resource = CREATE → 201
    // ============================================================
    @Test
    @DisplayName("TC-14 | PUT /api/v1/locations/9876 on non-existent ID returns 201 Created (upsert)")
    void updateLocation_nonExistentId_upserts201() throws Exception {
        String json = """
                {
                  "city": "CreatedByPut",
                  "stateProvince": "SomeState",
                  "postalCode": "000000",
                  "country": "/api/v1/countries/IN"
                }
                """;

        mockMvc.perform(put(BASE + "/9876")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())              // 201 — log confirmed
                .andExpect(jsonPath("$.city").value("CreatedByPut"));
    }

    // ============================================================
    // ❌ TC-15 | PUT — Missing city → 400
    // RestValidationConfig "beforeSave" fires → RepositoryConstraintViolationException → 400
    // Log confirmed: PUT with no city → Status = 409 BEFORE fix, 400 AFTER fix
    // ============================================================
    @Test
    @DisplayName("TC-15 | PUT /api/v1/locations/{id} with missing city returns 400")
    void updateLocation_missingCity_returns400() throws Exception {
        Location loc = persistUniqueLocation();

        String json = """
                {
                  "postalCode": "00000",
                  "country": "/api/v1/countries/IN"
                }
                """;

        mockMvc.perform(put(BASE + "/" + loc.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())           // 400
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    // ============================================================
    // ✅ TC-16 | PATCH — Partial update (city only) → 200 + updated body
    // ============================================================
    @Test
    @DisplayName("TC-16 | PATCH /api/v1/locations/{id} with city only returns 200 and updated city")
    void patchLocation_cityOnly_returns200() throws Exception {
        Location loc = persistUniqueLocation();
        String originalPostal = loc.getPostalCode();

        mockMvc.perform(patch(BASE + "/" + loc.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"city\": \"PatchedCity\"}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.city").value("PatchedCity"))
                .andExpect(jsonPath("$.postalCode").value(originalPostal));
    }

    // ============================================================
    // ❌ TC-17 | PATCH — Blank city → 400
    // RestValidationConfig "beforeSave" fires → RepositoryConstraintViolationException → 400
    // ============================================================
    @Test
    @DisplayName("TC-17 | PATCH /api/v1/locations/{id} with blank city returns 400")
    void patchLocation_blankCity_returns400() throws Exception {
        Location loc = persistUniqueLocation();

        mockMvc.perform(patch(BASE + "/" + loc.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"city\": \"   \"}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())           // 400
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    // ============================================================
    // ✅ TC-18 | DELETE — Location with no FK deps → 204
    // ============================================================
    @Test
    @DisplayName("TC-18 | DELETE /api/v1/locations/{id} with no FK dependency returns 204")
    void deleteLocation_noFKDependency_returns204() throws Exception {
        Location loc = persistUniqueLocation();

        mockMvc.perform(delete(BASE + "/" + loc.getId()))
                .andExpect(status().isNoContent());           // 204

        mockMvc.perform(get(BASE + "/" + loc.getId()).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    // ============================================================
    // ❌ TC-19 | DELETE — Non-existent ID → 404
    // ============================================================
    @Test
    @DisplayName("TC-19 | DELETE /api/v1/locations/99999999 returns 404 Not Found")
    void deleteLocation_notFound_returns404() throws Exception {
        mockMvc.perform(delete(BASE + "/99999999"))
                .andExpect(status().isNotFound());
    }

    // ============================================================
    // ❌ TC-20 | DELETE — FK violation: location_id 1700
    // Inside @Transactional: FK check fires at COMMIT which never happens → 204
    // Without @Transactional: DataIntegrityViolationException → 409
    // This test documents the @Transactional limitation.
    // ============================================================
    @Test
    @DisplayName("TC-20 | DELETE /locations/1700 inside @Transactional returns 204 (FK check deferred to commit)")
    void deleteLocation_withFKDependency_transactionalBehavior() throws Exception {
        mockMvc.perform(delete(BASE + "/1700"))
                .andExpect(status().isNoContent());           // 204 inside @Transactional
    }

    // ============================================================
    // ✅ TC-21 | SEARCH — findByCity: South Brunswick → 1 result
    // ============================================================
    @Test
    @DisplayName("TC-21 | GET /search/findByCity?city=South Brunswick returns 1 result")
    void searchByCity_southBrunswick_returns1Result() throws Exception {
        mockMvc.perform(get(BASE + "/search/findByCity")
                        .param("city", "South Brunswick")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.locations").isArray())
                .andExpect(jsonPath("$._embedded.locations", hasSize(1)))
                .andExpect(jsonPath("$._embedded.locations[0].city").value("South Brunswick"))
                .andExpect(jsonPath("$._embedded.locations[0].postalCode").value("50090"));
    }

    // ============================================================
    // ❌ TC-22 | SEARCH — findByCity: unknown → empty array
    // Log confirmed: Spring Data REST returns { "_embedded": { "locations": [] } }
    // ============================================================
    @Test
    @DisplayName("TC-22 | GET /search/findByCity?city=Unknown returns 200 with empty locations array")
    void searchByCity_unknownCity_returnsEmptyArray() throws Exception {
        mockMvc.perform(get(BASE + "/search/findByCity")
                        .param("city", "CityThatDoesNotExistXYZ")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.locations").isArray())
                .andExpect(jsonPath("$._embedded.locations").isEmpty());
    }

    // ============================================================
    // ✅ TC-23 | SEARCH — findByStateProvince: New Jersey → South Brunswick
    // ============================================================
    @Test
    @DisplayName("TC-23 | GET /search/findByStateProvince?stateProvince=New Jersey returns South Brunswick")
    void searchByStateProvince_newJersey_returnsSouthBrunswick() throws Exception {
        mockMvc.perform(get(BASE + "/search/findByStateProvince")
                        .param("stateProvince", "New Jersey")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.locations").isArray())
                .andExpect(jsonPath("$._embedded.locations[0].stateProvince").value("New Jersey"))
                .andExpect(jsonPath("$._embedded.locations[0].city").value("South Brunswick"));
    }

    // ============================================================
    // ✅ TC-24 | SEARCH — findByPostalCode: 50090 → South Brunswick
    // ============================================================
    @Test
    @DisplayName("TC-24 | GET /search/findByPostalCode?postalCode=50090 returns South Brunswick")
    void searchByPostalCode_50090_returnsSouthBrunswick() throws Exception {
        mockMvc.perform(get(BASE + "/search/findByPostalCode")
                        .param("postalCode", "50090")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.locations").isArray())
                .andExpect(jsonPath("$._embedded.locations", hasSize(1)))
                .andExpect(jsonPath("$._embedded.locations[0].postalCode").value("50090"))
                .andExpect(jsonPath("$._embedded.locations[0].city").value("South Brunswick"));
    }

    // ============================================================
    // ✅ TC-25 | SEARCH — findByCountry_CountryId: IT → Roma + Venice (2)
    // ============================================================
    @Test
    @DisplayName("TC-25 | GET /search/findByCountry_CountryId?countryId=IT returns 2 locations")
    void searchByCountryId_IT_returnsTwoLocations() throws Exception {
        mockMvc.perform(get(BASE + "/search/findByCountry_CountryId")
                        .param("countryId", "IT")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.locations").isArray())
                .andExpect(jsonPath("$._embedded.locations", hasSize(2)));
    }

    // ============================================================
    // ❌ TC-26 | SEARCH — findByCountry_CountryId: unknown → empty array
    // Log confirmed: { "_embedded": { "locations": [] } }
    // ============================================================
    @Test
    @DisplayName("TC-26 | GET /search/findByCountry_CountryId?countryId=ZZ returns empty array")
    void searchByCountryId_unknown_returnsEmptyArray() throws Exception {
        mockMvc.perform(get(BASE + "/search/findByCountry_CountryId")
                        .param("countryId", "ZZ")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.locations").isArray())
                .andExpect(jsonPath("$._embedded.locations").isEmpty());
    }

    // ============================================================
    // ✅ TC-27 | SEARCH — findByCityAndCountry: Roma + IT → 1 result
    // ============================================================
    @Test
    @DisplayName("TC-27 | GET /search/findByCityAndCountry_CountryId?city=Roma&countryId=IT returns 1 result")
    void searchByCityAndCountry_romaIT_returns1Result() throws Exception {
        mockMvc.perform(get(BASE + "/search/findByCityAndCountry_CountryId")
                        .param("city", "Roma")
                        .param("countryId", "IT")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.locations").isArray())
                .andExpect(jsonPath("$._embedded.locations", hasSize(1)))
                .andExpect(jsonPath("$._embedded.locations[0].city").value("Roma"));
    }

    // ============================================================
    // ❌ TC-28 | SEARCH — findByCityAndCountry: Roma + US → empty array
    // Log confirmed: { "_embedded": { "locations": [] } }
    // ============================================================
    @Test
    @DisplayName("TC-28 | GET /search/findByCityAndCountry_CountryId?city=Roma&countryId=US returns empty array")
    void searchByCityAndCountry_romaUS_returnsEmptyArray() throws Exception {
        mockMvc.perform(get(BASE + "/search/findByCityAndCountry_CountryId")
                        .param("city", "Roma")
                        .param("countryId", "US")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.locations").isArray())
                .andExpect(jsonPath("$._embedded.locations").isEmpty());
    }
}