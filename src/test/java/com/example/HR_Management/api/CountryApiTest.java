package com.example.HR_Management.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class CountryApiTest {
	@Autowired
    private MockMvc mockMvc;
 
    private static final String BASE_URL        = "/countries";
    private static final String BASE_URL_WITH_ID = "/countries/{id}";
 
    private static final String VALID_REGION_URI   = "/regions/10";
    private static final String INVALID_REGION_URI = "/regions/99999";
    
    
    
   @Test
    void tc01_getCountryById_existingId_returns200() throws Exception {
        mockMvc.perform(get(BASE_URL_WITH_ID, "IN")
                        .accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andExpect(jsonPath("$.countryName").value("India"));
    }
    
    @Test
    void tc02_getCountryById_nonExistentId_returns404() throws Exception {
        mockMvc.perform(get(BASE_URL_WITH_ID, "ZZ")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
 
    
    @Test
    void tc03_getAllCountries_returns200() throws Exception {
        mockMvc.perform(get(BASE_URL)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.countries").isArray());
    }
    @Test
    void tc04_postCountry_validJson_returns201() throws Exception {
        String body = """
                {
                  "countryId": "TS",
                  "countryName": "TestLand",
                  "region": "%s"
                }
                """.formatted(VALID_REGION_URI);
 
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body));
        mockMvc.perform(get(BASE_URL_WITH_ID, "TS"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.countryName").value("TestLand"));
           
        
    }
    @Test
    void tc14_putCountry_malformedJson_returns400() throws Exception {
        String malformedBody = "{ countryId: CA countryName: bad json }";
 
        mockMvc.perform(put(BASE_URL_WITH_ID, "CA")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedBody))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    void tc11_putCountry_validUpdate_returns200() throws Exception {
        String body = """
                {
                  "countryId": "CA",
                  "countryName": "Canada Updated",
                  "region": "%s"
                }
                """.formatted(VALID_REGION_URI);
 
        mockMvc.perform(put(BASE_URL_WITH_ID, "CA")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body));
        mockMvc.perform(get(BASE_URL_WITH_ID, "CA"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.countryName").value("Canada Updated"));
    }
    @Test
    void tc06_postCountry_missingCountryId_returns400() throws Exception {
        String body = """
                {
                  "countryName": "NoId Land",
                  "region": "%s"
                }
                """.formatted(VALID_REGION_URI);
 
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isInternalServerError());
    }
    @Test
    void tc05_postCountry_duplicateId_returns409() throws Exception {
    String body = """
            {
              "countryId": "CA",
              "countryName": "Canada Duplicate3",
              "region": "%s"
            }
            """.formatted(VALID_REGION_URI);

    mockMvc.perform(post(BASE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
            .andExpect(status().isInternalServerError());
    }
    @Test
    void tc07_postCountry_numericCountryId_returns400() throws Exception {
        String body = """
                {
                  "countryId": "123",
                  "countryName": "NumberId Land",
                  "region": "%s"
                }
                """.formatted(VALID_REGION_URI);
 
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    
    
    @Test
    void tc09_postCountry_blankCountryName_returns400() throws Exception {
        String body = """
                {
                  "countryId": "BL",
                  "countryName": "   ",
                  "region": "%s"
                }
                """.formatted(VALID_REGION_URI);
 
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }


    
}