package com.example.HR_Management.api;

import com.example.HR_Management.entity.Country;
import com.example.HR_Management.entity.Location;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.data.rest.base-path=/api/v1"
})
@AutoConfigureMockMvc
@Transactional
class LocationApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager entityManager;

    private Country country;

    // ================================
    // 🔧 SETUP (SAFE FOR REAL DB)
    // ================================
    @BeforeEach
    void setup() {
        country = entityManager.find(Country.class, "IN");

        if (country == null) {
            country = new Country();
            country.setCountryId("IN");   // valid (<=4, letters only)
            country.setCountryName("India");
            entityManager.persist(country);
        }
    }

    private BigDecimal id(long val) {
        return BigDecimal.valueOf(val);
    }

    private Location createUniqueLocation() {
        Location loc = new Location();

        loc.setId(id(System.currentTimeMillis()));
        loc.setCity("City_" + UUID.randomUUID());
        loc.setStateProvince("State");
        loc.setPostalCode("99999");
        loc.setCountry(country);

        entityManager.persist(loc);
        return loc;
    }

    // ================================
    // ✅ GET ALL
    // ================================
    @Test
    void getAllLocations_shouldReturnData() throws Exception {
        mockMvc.perform(get("/api/v1/locations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.locations").isArray());
    }

    // ================================
    // ✅ GET BY ID
    // ================================
    @Test
    void getLocationById_shouldReturnLocation() throws Exception {
        Location loc = createUniqueLocation();

        mockMvc.perform(get("/api/v1/locations/" + loc.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.city").value(loc.getCity()));
    }

    // ================================
    // ❌ GET BY ID - NOT FOUND
    // ================================
    @Test
    void getLocationById_notFound() throws Exception {
        mockMvc.perform(get("/api/v1/locations/999999999"))
                .andExpect(status().isNotFound());
    }

    // ================================
    // ❌ INVALID ID FORMAT
    // ================================
    @Test
    void invalidId_shouldReturn400() throws Exception {
        mockMvc.perform(get("/api/v1/locations/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").exists());
    }

    // ================================
    // ✅ POST
    // ================================
    @Test
    void createLocation_shouldReturn201() throws Exception {
        long uniqueId = System.currentTimeMillis();

        String json = """
        {
          "id": %d,
          "city": "API_City",
          "stateProvince": "Test",
          "postalCode": "12345",
          "country": "/api/v1/countries/IN"
        }
        """.formatted(uniqueId);

        mockMvc.perform(post("/api/v1/locations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated());
    }

    // ================================
    // ❌ POST VALIDATION FAIL
    // ================================
    @Test
    void createLocation_missingCity_shouldFail() throws Exception {
        long uniqueId = System.currentTimeMillis();

        String json = """
        {
          "id": %d,
          "stateProvince": "Test",
          "postalCode": "12345",
          "country": "/api/v1/countries/IN"
        }
        """.formatted(uniqueId);

        mockMvc.perform(post("/api/v1/locations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest());
    }

    // ================================
    // ✅ PUT (UPDATE)
    // ================================
    @Test
    void updateLocation_shouldReturn204() throws Exception {
        Location loc = createUniqueLocation();

        String json = """
        {
          "city": "UpdatedCity",
          "stateProvince": "Updated",
          "postalCode": "00000",
          "country": "/api/v1/countries/IN"
        }
        """;

        mockMvc.perform(put("/api/v1/locations/" + loc.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isNoContent());
    }

    // ================================
    // ❌ PUT - NOT FOUND
    // ================================
    @Test
    void updateLocation_notFound_shouldCreateInstead() throws Exception {

        String json = """
        {
          "city": "CreatedCity",
          "stateProvince": "X",
          "postalCode": "000",
          "country": "/api/v1/countries/IN"
        }
        """;

        mockMvc.perform(put("/api/v1/locations/99999999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated()); // ✅ FIX
    }

    // ================================
    // ✅ DELETE
    // ================================
    @Test
    void deleteLocation_shouldReturn204() throws Exception {
        Location loc = createUniqueLocation();

        mockMvc.perform(delete("/api/v1/locations/" + loc.getId()))
                .andExpect(status().isNoContent());
    }

    // ================================
    // ❌ DELETE - NOT FOUND
    // ================================
    @Test
    void deleteLocation_notFound() throws Exception {
        mockMvc.perform(delete("/api/v1/locations/99999999"))
                .andExpect(status().isNotFound());
    }
}