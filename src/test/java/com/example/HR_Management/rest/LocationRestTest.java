package com.example.HR_Management.rest;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class LocationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // --- GET TESTS ---
    @Test
    void testGetAllLocations() throws Exception { // TC-LOC-05
        mockMvc.perform(get("/api/v1/location"))
                .andExpect(status().isOk()); // 200 OK
    }

    @Test
    void testGetLocationById_NotFound() throws Exception { // TC-LOC-08 (Edge Case)
        mockMvc.perform(get("/api/v1/location/9999"))
                .andExpect(status().isNotFound()); // 404 Not Found
    }

    // --- POST TESTS ---
    @Test
    void testCreateLocation_Success() throws Exception { // TC-LOC-09
        String json = "{\"city\": \"Delhi\", \"streetAddress\": \"123 St\", \"country\": {\"countryId\": \"IN\"}}";
        mockMvc.perform(post("/api/v1/location")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated()); // 201 Created
    }

    @Test
    void testCreateLocation_BadRequest() throws Exception { // TC-LOC-10 (Edge Case)
        String invalidJson = "{\"streetAddress\": \"Missing City\"}";
        mockMvc.perform(post("/api/v1/location")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest()); // 400 Bad Request
    }

    // --- PUT TESTS (Custom Implementation) ---
    @Test
    void testUpdateLocation_Success() throws Exception { // TC-LOC-11
        // Assumes ID 1 exists
        String json = "{\"location_id\": 1, \"city\": \"New Delhi\"}"; 
        mockMvc.perform(put("/api/v1/location")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk()); // 200 OK
    }

    @Test
    void testUpdateLocation_NoIdInBody() throws Exception { // TC-LOC-13 (Edge Case)
        String json = "{\"city\": \"No ID Provided\"}";
        mockMvc.perform(put("/api/v1/location")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest()); // 400 Bad Request
    }

    // --- DELETE TESTS ---
    @Test
    void testDeleteLocation_Success() throws Exception { // TC-LOC-14
        mockMvc.perform(delete("/api/v1/location/1"))
                .andExpect(status().isOk()); // 200 OK or 204
    }

    // --- GLOBAL EXCEPTION TEST ---
    @Test
    void testTypeMismatchException() throws Exception { // TC-LOC-16 (Edge Case)
        mockMvc.perform(get("/api/v1/location/abc")) // Passing string instead of ID
                .andExpect(status().isBadRequest()); // 400 Bad Request
    }
}