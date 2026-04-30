package com.example.HR_Management.api;

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
class DepartmentApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testPostNegativeNameWithNumbers() throws Exception {
        // Sl No 17: /api/v1/department should return 400 for names with numbers 
        String json = "{\"departmentId\": 60, \"departmentName\": \"HR 7\", \"location\": {\"id\": 1700}}";
        
        mockMvc.perform(post("/api/v1/department")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetMaxSalaryPositive() throws Exception {
        
        mockMvc.perform(get("/api/v1/department/search/findmaxsalary")
                .param("id", "10"))
                .andExpect(status().isOk());
    }
}