package com.example.HR_Management.repository;

import com.example.HR_Management.entity.Department;
import com.example.HR_Management.entity.Location;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class DepartmentRepositoryTest {

    @Autowired
    private DepartmentRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void testNegativeNameTooLong() {
        // Sl No 2: Name > 30 chars must throw violation [cite: 85]
        Location loc = new Location();
        loc.setId(new Long("1700"));
        entityManager.persist(loc);

        Department dept = new Department(new BigDecimal("100"), "Engineering Research and Development", loc);
        assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(dept));
    }

    @Test
    void testNegativeDuplicatePrimaryKey() {
        // Sl No 10: Throws exception on duplicate department_id [cite: 85]
        Location loc = new Location();
        loc.setId(new Long("1700"));
        entityManager.persist(loc);

        repository.saveAndFlush(new Department(new BigDecimal("10"), "HR", loc));
        entityManager.clear();

        Department dup = new Department(new BigDecimal("10"), "IT", loc);
        assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(dup));
    }
}