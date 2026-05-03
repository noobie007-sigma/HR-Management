package com.example.HR_Management.api;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.example.HR_Management.entity.Job;
import com.example.HR_Management.repository.JobRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import jakarta.transaction.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class JobApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JobRepository jobRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void createJob_ValidData_ShouldReturnCreated() throws Exception {
        Job job = new Job();
        job.setJobId("DEV_01");
        job.setJobTitle("Software Engineer");
        job.setMinSalary(BigDecimal.valueOf(5000));
        job.setMaxSalary(BigDecimal.valueOf(10000));

        mockMvc.perform(post("/api/v1/jobs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(job)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"));
    }

    @Test
    void createJob_MissingTitle_ShouldReturnBadRequest() throws Exception {
        Job job = new Job();
        job.setJobId("ERR_01");
        job.setJobTitle("");

        mockMvc.perform(post("/api/v1/jobs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(job)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getJobById_ValidId_ShouldReturnJob() throws Exception {
        Job job = new Job();
        job.setJobId("FIND_ME");
        job.setJobTitle("Data Scientist");
        jobRepository.saveAndFlush(job);

        mockMvc.perform(get("/api/v1/jobs/FIND_ME"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobTitle").value("Data Scientist"));
    }

    @Test
    void patchJob_UpdateTitle_ShouldReturnNoContent() throws Exception {
        Job job = new Job();
        job.setJobId("PATCH_01");
        job.setJobTitle("Old Title");
        jobRepository.saveAndFlush(job);

        mockMvc.perform(patch("/api/v1/jobs/PATCH_01")
                .contentType("application/merge-patch+json")
                .content("{\"jobTitle\": \"New Updated Title\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/jobs/PATCH_01"))
                .andExpect(jsonPath("$.jobTitle").value("New Updated Title"));
    }
}
