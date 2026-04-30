package com.example.HR_Management.repository;

import com.example.HR_Management.entity.Job;
import jakarta.persistence.EntityManager;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class JobRepositoryTest {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private EntityManager entityManager;

    
    private Job createValidJob(String id, String title) {
        Job job = new Job();
        job.setJobId(id);
        job.setJobTitle(title);
        job.setMinSalary(BigDecimal.valueOf(1000));
        job.setMaxSalary(BigDecimal.valueOf(5000));
        return job;
    }

    
    @Test
    void saveJob_Valid_ShouldPersist() {
        Job job = createValidJob("J101", "Developer");

        Job saved = jobRepository.saveAndFlush(job);

        assertNotNull(saved);
        assertEquals("J101", saved.getJobId());
    }

    
//    @Test
//    void saveJob_WithDuplicateId_ShouldThrowException() {
//        Job job1 = createValidJob("J102", "Tester");
//        Job job2 = createValidJob("J102", "Manager"); // same ID
//
//        jobRepository.saveAndFlush(job1);
//
//        entityManager.clear(); // 🔥 IMPORTANT
//
//        assertThrows(DataIntegrityViolationException.class, () -> {
//            jobRepository.saveAndFlush(job2);
//        });
//    }
    
//    @Test
//    void saveJob_WithDuplicateId_ShouldThrowException() {
//        // 1. Save the first job
//        Job job1 = new Job("DEV", "Developer", "5000", "10000");
//        jobRepository.saveAndFlush(job1); 
//
//        // 2. Try to save the duplicate
//        Job job2 = new Job("DEV", "Senior Developer", "6000", "12000");
//
//        assertThrows(DataIntegrityViolationException.class, () -> {
//            jobRepository.saveAndFlush(job2); // Flush forces the DB to throw the error NOW
//        });
//    }
  
    @Test
    void saveJob_WithNullTitle_ShouldThrowException() {
        Job job = new Job();
        job.setJobId("J103");
        job.setJobTitle(null); // invalid

        assertThrows(ConstraintViolationException.class, () -> {
            jobRepository.saveAndFlush(job);
        });
    }

    
    @Test
    void saveJob_InvalidSalaryRange_ShouldThrowException() {
        Job job = new Job();
        job.setJobId("J104");
        job.setJobTitle("Invalid Salary");

        job.setMinSalary(BigDecimal.valueOf(6000));
        job.setMaxSalary(BigDecimal.valueOf(3000)); // invalid

        assertThrows(ConstraintViolationException.class, () -> {
            jobRepository.saveAndFlush(job);
        });
    }

    
    @Test
    void findById_ShouldReturnJob() {
        Job job = createValidJob("J105", "Analyst");
        jobRepository.saveAndFlush(job);

        Job found = jobRepository.findById("J105").orElse(null);

        assertNotNull(found);
        assertEquals("Analyst", found.getJobTitle());
    }


}