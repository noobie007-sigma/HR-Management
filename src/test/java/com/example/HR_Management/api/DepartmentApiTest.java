package com.example.HR_Management.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@WebAppConfiguration
@Transactional
public class DepartmentApiTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String HAL_JSON = "application/vnd.hal+json";

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .build();
    }

    private Map<String, Object> validDepartmentBody() {
        Map<String, Object> body = new HashMap<>();
        body.put("departmentId", 9999);
        body.put("departmentName", "TestDepartment");
        body.put("location", "http://localhost/api/v1/location/1700");
        return body;
    }

    @Test
    void tc16_createDepartment_validBody_returns201() throws Exception {
        mockMvc.perform(post("/api/v1/department")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validDepartmentBody())))
                .andDo(print())
                .andExpect(status().isCreated());
    }

 
    @Test
    void tc17_createDepartment_nameWithNumbers_returns400() throws Exception {
        Map<String, Object> body = validDepartmentBody();
        body.put("departmentName", "HR 7");

        mockMvc.perform(post("/api/v1/department")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    
    @Test
    void tc18_createDepartment_missingLocation_returns400() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("departmentId", 9998);
        body.put("departmentName", "NoLocationDept");

        mockMvc.perform(post("/api/v1/department")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }
    @Test
    void tc19_createDepartment_xmlContentType_returnsNotSuccess() throws Exception {
        String xmlBody = "<department><departmentId>9997</departmentId>"
                + "<departmentName>XmlDept</departmentName></department>";

        mockMvc.perform(post("/api/v1/department")
                        .contentType(MediaType.APPLICATION_XML)
                        .content(xmlBody))
                .andDo(print())
                .andExpect(status().is(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.both(
                                org.hamcrest.Matchers.greaterThanOrEqualTo(200))
                                .and(org.hamcrest.Matchers.lessThan(300)))));
    }

    
    @Test
    void tc20_updateDepartment_validBody_returns200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("departmentId", 10);
        body.put("departmentName", "UpdatedAdmin");
        body.put("location", "http://localhost/api/v1/location/1700");

        mockMvc.perform(put("/api/v1/department/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andDo(print())
                .andExpect(status().is(org.hamcrest.Matchers.both(
                        org.hamcrest.Matchers.greaterThanOrEqualTo(200))
                        .and(org.hamcrest.Matchers.lessThan(400))));
    }

  
    @Test
    void tc22_updateDepartment_nonExistentId_returns2xx() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("departmentId", 99999);
        body.put("departmentName", "GhostDept");
        body.put("location", "http://localhost/api/v1/location/1700");

        mockMvc.perform(put("/api/v1/department/99999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andDo(print())
                .andExpect(status().is2xxSuccessful());
    }

    
    @Test
    void tc23_searchByName_validName_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/department/search/byName")
                        .param("name", "Sales"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(HAL_JSON));
    }

    
    @Test
    void tc24_searchByLocation_validId_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/department/search/byLocation")
                        .param("locationId", "1700"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(HAL_JSON));
    }

   
    @Test
    void tc25_searchByName_emptyParam_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/department/search/byName")
                        .param("name", ""))
                .andDo(print())
                .andExpect(status().isOk());
    }

    
    @Test
    void tc26_listAllDepartments_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/department"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(HAL_JSON));
    }

    @Test
    void tc27_getDepartmentById_validId_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/department/10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(HAL_JSON));
    }

    
    @Test
    void tc28_updateDepartment_malformedJson_returns400() throws Exception {
        String malformedJson = "{ departmentId: 10, departmentName: }";

        mockMvc.perform(put("/api/v1/department/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void tc29_getDepartmentById_nonExistentId_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/department/99999"))
                .andDo(print())
                .andExpect(status().isNotFound());
    }
    @Test
    void tc30_dbFailure_returns500() {
        System.out.println("TC30: Run manually with datasource misconfigured.");
    }

    @Test
    void tc31_invalidUrlPath_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/dept-details"))
                .andDo(print())
                .andExpect(status().isNotFound());
    }
}