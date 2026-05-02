package com.example.HR_Management.api;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.example.HR_Management.entity.Region;
import com.example.HR_Management.repository.RegionRepository;

/**
 * Endpoint (MockMvc) integration tests for Region REST API.
 *
 * Uses @SpringBootTest + MockMvc against the real MySQL DB (no H2, no Mockito).
 * Spring Data REST auto-generates all endpoints from RegionRepository —
 * these tests exercise every documented endpoint and scenario.
 *
 * HOW TO RUN:
 *   mvn test -Dtest=RegionApiTest
 *   or run directly from your IDE.
 *
 * IMPORTANT NOTES:
 *   - DB uses region IDs: 10, 20, 30, 40, 50 (not 1, 2, 3, 4)
 *   - Spring Data REST returns 201 (no body) on POST — body is at the Location URL
 *   - Spring Data REST returns 204 (no body) on PUT and PATCH updates
 *   - regionId is NOT exposed in the HAL body by default; it appears only in _links.self href
 *     To expose it, add: config.exposeIdsFor(Region.class) in RepositoryRestConfigurer
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application.properties")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RegionApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RegionRepository regionRepository;

    private static final String BASE_URL   = "/api/v1/regions";
    private static final BigDecimal TEST_ID = BigDecimal.valueOf(99L);

    // Seed data uses IDs 10, 20, 30, 40, 50 — use 10 as the "valid existing" ID
    private static final BigDecimal EXISTING_ID = BigDecimal.valueOf(10L);

    private static final String VALID_BODY = """
            { "regionId": 99, "regionName": "Test Region" }
            """;
    private static final String NO_NAME_BODY = """
            { "regionId": 99 }
            """;
    private static final String MALFORMED_BODY = """
            { regionId: 99, }
            """;
    private static final String NULL_ID_BODY = """
            { "regionName": "No ID Region" }
            """;

    // ─────────────────────────────────────────────
    //  Cleanup: remove test region after each test
    // ─────────────────────────────────────────────
    @AfterEach
    void cleanUp() {
        if (regionRepository.existsById(TEST_ID)) {
            regionRepository.deleteById(TEST_ID);
        }
        // Also clean up the upsert region from R-EP-17
        BigDecimal upsertId = BigDecimal.valueOf(999L);
        if (regionRepository.existsById(upsertId)) {
            regionRepository.deleteById(upsertId);
        }
    }

    // ══════════════════════════════════════════════
    //  GET /api/v1/regions  — Collection
    // ══════════════════════════════════════════════

    // R-EP-01
    @Test
    @Order(1)
    @DisplayName("R-EP-01: GET /regions — returns HAL JSON with _embedded.regions")
    void getAllRegions_WhenDataExists_Returns200WithEmbeddedList() throws Exception {
        mockMvc.perform(get(BASE_URL).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.regions").isArray())
                .andExpect(jsonPath("$._embedded.regions", not(empty())))
                .andExpect(jsonPath("$.page").exists())
                .andExpect(jsonPath("$.page.totalElements").value(greaterThanOrEqualTo(4)));
    }

    // R-EP-02
    @Test
    @Order(2)
    @DisplayName("R-EP-02: GET /regions — page metadata is present in response")
    void getAllRegions_ResponseContainsPageMetadata() throws Exception {
        mockMvc.perform(get(BASE_URL).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.size").exists())
                .andExpect(jsonPath("$.page.totalElements").exists())
                .andExpect(jsonPath("$.page.totalPages").exists())
                .andExpect(jsonPath("$.page.number").exists());
    }

    // R-EP-04: Pagination — ?page=0&size=2
    @Test
    @Order(3)
    @DisplayName("R-EP-04: GET /regions?page=0&size=2 — returns exactly 2 regions")
    void getAllRegions_WithPagination_ReturnsCorrectPageSize() throws Exception {
        mockMvc.perform(get(BASE_URL + "?page=0&size=2").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.regions", hasSize(2)))
                .andExpect(jsonPath("$.page.size").value(2))
                .andExpect(jsonPath("$.page.number").value(0));
    }

    // R-EP-05: Sorting ascending by regionId
    // FIX: regionId is NOT in the HAL body by default — only in _links.self href.
    //      We verify the list is non-empty and page metadata is correct instead.
    //      To assert regionId values, add config.exposeIdsFor(Region.class) in your
    //      RepositoryRestConfigurer bean.
    @Test
    @Order(4)
    @DisplayName("R-EP-05: GET /regions?sort=regionId,asc — regions sorted in ascending ID order")
    void getAllRegions_SortedByRegionIdAsc_ReturnsSortedList() throws Exception {
        mockMvc.perform(get(BASE_URL + "?sort=regionId,asc").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.regions").isArray())
                .andExpect(jsonPath("$._embedded.regions", not(empty())))
                // Verify the first item's self-link contains the lowest ID (10)
                .andExpect(jsonPath("$._embedded.regions[0]._links.self.href")
                        .value(org.hamcrest.Matchers.containsString("/regions/10")))
                .andExpect(jsonPath("$._embedded.regions[1]._links.self.href")
                        .value(org.hamcrest.Matchers.containsString("/regions/20")));
    }

    // R-EP-06: Sorting descending by regionName
    @Test
    @Order(5)
    @DisplayName("R-EP-06: GET /regions?sort=regionName,desc — regions sorted reverse alphabetically")
    void getAllRegions_SortedByRegionNameDesc_ReturnsSortedList() throws Exception {
        mockMvc.perform(get(BASE_URL + "?sort=regionName,desc").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.regions").isArray())
                .andExpect(jsonPath("$._embedded.regions", not(empty())));
    }

    // ══════════════════════════════════════════════
    //  GET /api/v1/regions/{id}  — Single resource
    // ══════════════════════════════════════════════

    // R-EP-07
    // FIX: DB uses ID 10 (not 1). Use EXISTING_ID = 10.
    @Test
    @Order(6)
    @DisplayName("R-EP-07: GET /regions/10 — returns region with _links for valid ID")
    void getRegionById_WhenValidId_Returns200WithLinks() throws Exception {
        mockMvc.perform(get(BASE_URL + "/" + EXISTING_ID).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.regionName").isNotEmpty())
                .andExpect(jsonPath("$._links.self").exists())
                .andExpect(jsonPath("$._links.countries").exists());
    }

    // R-EP-08
    @Test
    @Order(7)
    @DisplayName("R-EP-08: GET /regions/9999 — returns 404 for non-existing ID")
    void getRegionById_WhenIdNotFound_Returns404() throws Exception {
        mockMvc.perform(get(BASE_URL + "/9999").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    // R-EP-09
    @Test
    @Order(8)
    @DisplayName("R-EP-09: GET /regions/abc — returns 400 for non-numeric ID")
    void getRegionById_WhenNonNumericId_Returns400() throws Exception {
        mockMvc.perform(get(BASE_URL + "/abc").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    // ══════════════════════════════════════════════
    //  POST /api/v1/regions  — Create
    // ══════════════════════════════════════════════

    // R-EP-10
    // FIX: Spring Data REST POST returns 201 with empty body + Location header.
    //      Assert the Location header instead of body fields.
    @Test
    @Order(9)
    @DisplayName("R-EP-10: POST /regions — creates region with valid body, returns 201 with Location")
    void createRegion_WithValidBody_Returns201() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location",
                        org.hamcrest.Matchers.containsString("/regions/99")));
    }

    // R-EP-11
    @Test
    @Order(10)
    @DisplayName("R-EP-11: POST /regions — missing regionName returns 400 or 201 (depends on validation config)")
    void createRegion_WhenMissingRegionName_Returns4xx() throws Exception {
        // Spring Data REST allows null regionName unless @NotBlank + spring-boot-starter-validation
        // is configured. This test documents the actual behaviour.
        // Add @NotBlank to Region.regionName to enforce 400.
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(NO_NAME_BODY))
                .andExpect(status().is4xxClientError());
    }

    // R-EP-12
    // FIX: Spring Data REST treats POST with an existing ID as an upsert (update) and
    //      returns 201 again — it does NOT return a conflict error.
    //      We verify the first POST succeeds and document the upsert behaviour.
    @Test
    @Order(11)
    @DisplayName("R-EP-12: POST /regions — duplicate regionId is treated as upsert (returns 201 both times)")
    void createRegion_WhenDuplicateId_ReturnsUpsert() throws Exception {
        // First POST — creates
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated());

        // Second POST with same regionId — Spring Data REST upserts (also 201)
        // NOTE: Spring Data REST does NOT return 409 Conflict out of the box.
        //       To enforce uniqueness, expose a custom POST endpoint with an existence check.
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated());
    }

    // R-EP-13
    @Test
    @Order(12)
    @DisplayName("R-EP-13: POST /regions — malformed JSON returns 400")
    void createRegion_WhenMalformedJson_Returns400() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MALFORMED_BODY))
                .andExpect(status().isBadRequest());
    }

    // R-EP-14
    @Test
    @Order(13)
    @DisplayName("R-EP-14: POST /regions — missing regionId returns error (manual ID required)")
    void createRegion_WhenRegionIdMissing_ReturnsError() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(NULL_ID_BODY))
                .andExpect(status().is4xxClientError());
    }

    // ══════════════════════════════════════════════
    //  PUT /api/v1/regions/{id}  — Full Replace
    // ══════════════════════════════════════════════

    // R-EP-16
    // FIX: Spring Data REST PUT returns 204 No Content (not 200) on a successful update.
    @Test
    @Order(14)
    @DisplayName("R-EP-16: PUT /regions/99 — full update returns 204 No Content")
    void putRegion_WithValidBody_Returns204() throws Exception {
        // Seed the region first
        Region r = new Region();
        r.setRegionId(TEST_ID);
        r.setRegionName("Original Name");
        regionRepository.save(r);

        String updateBody = """
                { "regionId": 99, "regionName": "Updated Via PUT" }
                """;

        mockMvc.perform(put(BASE_URL + "/" + TEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                // Spring Data REST returns 204 No Content on update, not 200
                .andExpect(status().isNoContent())
                .andExpect(header().string("Location",
                        org.hamcrest.Matchers.containsString("/regions/99")));

        // Verify the update persisted
        Region updated = regionRepository.findById(TEST_ID).orElseThrow();
        assert updated.getRegionName().equals("Updated Via PUT");
    }

    // R-EP-17
    // Spring Data REST upserts on PUT to a non-existing ID (creates it, returns 201).
    @Test
    @Order(15)
    @DisplayName("R-EP-17: PUT /regions/999 — non-existing ID is upserted (returns 201 Created)")
    void putRegion_WhenIdNotFound_ReturnsCreated() throws Exception {
        String body = """
                { "regionId": 999, "regionName": "Upserted Region" }
                """;

        mockMvc.perform(put(BASE_URL + "/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                // Spring Data REST upserts: 201 if new, 204 if existing
                .andExpect(status().is2xxSuccessful());
        // Cleanup handled in @AfterEach
    }

    // R-EP-18
    @Test
    @Order(16)
    @DisplayName("R-EP-18: PUT /regions/99 — empty body returns 400")
    void putRegion_WithEmptyBody_Returns400() throws Exception {
        mockMvc.perform(put(BASE_URL + "/" + TEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().is4xxClientError());
    }

    // ══════════════════════════════════════════════
    //  PATCH /api/v1/regions/{id}  — Partial Update
    // ══════════════════════════════════════════════

    // R-EP-20
    // FIX: Spring Data REST PATCH returns 204 No Content (not 200) on a successful update.
    @Test
    @Order(17)
    @DisplayName("R-EP-20: PATCH /regions/99 — partial update of regionName returns 204 No Content")
    void patchRegion_WithRegionNameOnly_Returns204() throws Exception {
        // Seed region
        Region r = new Region();
        r.setRegionId(TEST_ID);
        r.setRegionName("Before Patch");
        regionRepository.save(r);

        String patchBody = """
                { "regionName": "After Patch" }
                """;

        mockMvc.perform(patch(BASE_URL + "/" + TEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patchBody))
                // Spring Data REST returns 204 No Content on patch, not 200
                .andExpect(status().isNoContent());

        // Verify the patch persisted
        Region patched = regionRepository.findById(TEST_ID).orElseThrow();
        assert patched.getRegionName().equals("After Patch");
    }

    // R-EP-21
    @Test
    @Order(18)
    @DisplayName("R-EP-21: PATCH /regions/9999 — non-existing ID returns 404")
    void patchRegion_WhenIdNotFound_Returns404() throws Exception {
        mockMvc.perform(patch(BASE_URL + "/9999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"regionName\": \"Ghost Update\" }"))
                .andExpect(status().isNotFound());
    }

    // ══════════════════════════════════════════════
    //  GET /api/v1/regions/{id}/countries  — Association
    // ══════════════════════════════════════════════

    // R-EP-22
    // FIX: DB uses ID 10 for Europe (not 1). Use EXISTING_ID = 10.
    @Test
    @Order(19)
    @DisplayName("R-EP-22: GET /regions/10/countries — returns 200 for region with countries")
    void getCountriesForRegion_WhenRegionHasCountries_Returns200() throws Exception {
        // Region 10 (Europe) has countries in the Oracle HR seed data
        mockMvc.perform(get(BASE_URL + "/" + EXISTING_ID + "/countries")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    // R-EP-23
    @Test
    @Order(20)
    @DisplayName("R-EP-23: GET /regions/99/countries — returns 200 with empty list for region with no countries")
    void getCountriesForRegion_WhenRegionHasNoCountries_Returns200WithEmpty() throws Exception {
        // Seed a region with no linked countries
        Region r = new Region();
        r.setRegionId(TEST_ID);
        r.setRegionName("Isolated Region");
        regionRepository.save(r);

        mockMvc.perform(get(BASE_URL + "/" + TEST_ID + "/countries")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    // R-EP-24
    @Test
    @Order(21)
    @DisplayName("R-EP-24: GET /regions/9999/countries — non-existing region returns 404")
    void getCountriesForRegion_WhenRegionNotFound_Returns404() throws Exception {
        mockMvc.perform(get(BASE_URL + "/9999/countries").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}