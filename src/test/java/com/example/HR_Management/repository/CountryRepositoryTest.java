package com.example.HR_Management.repository;

import static org.assertj.core.api.Assertions.assertThat;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.mapping.AccessOptions.SetOptions.Propagation;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.example.HR_Management.entity.Country;
import com.example.HR_Management.entity.Region;
import com.example.HR_Management.repository.CountryRepository;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(locations = "classpath:application.properties")

class CountryRepositoryTest {
 
    @Autowired
    private CountryRepository countryRepository;
 
    @Autowired
    private RegionRepository regionRepository;   
 
    private static final BigDecimal EXISTING_REGION_ID = BigDecimal.valueOf(10);
 
    private Region testRegion;
    private Country testCountry;

    @BeforeEach
    void setUp() {
        testRegion = regionRepository.findById(EXISTING_REGION_ID)
                .orElseThrow(() -> new IllegalStateException(
                        "Region with id=" + EXISTING_REGION_ID + " not found in DB. " +
                        "Please adjust EXISTING_REGION_ID or run insert_data.sql first."));
 
        testCountry = new Country("ZZ", "TestLand", testRegion);
        countryRepository.save(testCountry);
    }
 
    @AfterEach
    void tearDown() {
        countryRepository.deleteById("ZZ");
    }
 
 
    @Test
    @Order(1)
    @DisplayName("save() – should persist a new country and assign the given id")
    void testSaveCountry() {
        Country saved = countryRepository.findById("ZZ").orElse(null);
 
        assertThat(saved).isNotNull();
        assertThat(saved.getCountryId()).isEqualTo("ZZ");
        assertThat(saved.getCountryName()).isEqualTo("TestLand");
        assertThat(saved.getRegion().getRegionId()).isEqualTo(EXISTING_REGION_ID);
    }
 
    @Test
    @Order(2)
    @DisplayName("findById() – should return the country when it exists")
    void testFindByIdExists() {
        Optional<Country> result = countryRepository.findById("ZZ");
 
        assertThat(result).isPresent();
        assertThat(result.get().getCountryName()).isEqualTo("TestLand");
    }
 
    @Test
    @Order(3)
    @DisplayName("findById() – should return empty Optional for unknown id")
    void testFindByIdNotFound() {
        Optional<Country> result = countryRepository.findById("XX");
 
        assertThat(result).isNotPresent();
    }
 
    @Test
    @Order(4)
    @DisplayName("findAll() – should return a non-empty list containing the test country")
    void testFindAll() {
        List<Country> all = countryRepository.findAll();
 
        assertThat(all).isNotEmpty();
        assertThat(all).extracting(Country::getCountryId).contains("ZZ");
    }
 
    @Test
    @Order(5)
    @DisplayName("findByCountryNameIgnoreCase() – should find country by name regardless of case")
    void testFindByCountryNameIgnoreCase() {
        Optional<Country> result = countryRepository.findByCountryNameIgnoreCase("testland");
 
        assertThat(result).isPresent();
        assertThat(result.get().getCountryId()).isEqualTo("ZZ");
    }
 
    @Test
    @Order(6)
    @DisplayName("findByCountryNameIgnoreCase() – should return empty when name does not match")
    void testFindByCountryNameNotFound() {
        Optional<Country> result = countryRepository.findByCountryNameIgnoreCase("NonExistentCountry");
 
        assertThat(result).isNotPresent();
    }
 
    @Test
    @Order(7)
    @DisplayName("findByRegion_RegionId() – should list all countries in a region")
    void testFindByRegionId() {
        List<Country> result = countryRepository.findByRegion_RegionId(EXISTING_REGION_ID);
 
        assertThat(result).isNotEmpty();
        assertThat(result).extracting(Country::getCountryId).contains("ZZ");
    }
 
    @Test
    @Order(8)
    @DisplayName("existsByCountryNameIgnoreCase() – should return true for an existing name")
    void testExistsByCountryName_True() {
        boolean exists = countryRepository.existsByCountryNameIgnoreCase("TestLand");
 
        assertThat(exists).isTrue();
    }
 
    @Test
    @Order(9)
    @DisplayName("existsByCountryNameIgnoreCase() – should return false for an absent name")
    void testExistsByCountryName_False() {
        boolean exists = countryRepository.existsByCountryNameIgnoreCase("GhostLand");
 
        assertThat(exists).isFalse();
    }
 
    @Test
    @Order(10)
    @DisplayName("save() (update) – should update an existing country's name")
    void testUpdateCountryName() {
        Country country = countryRepository.findById("ZZ")
                .orElseThrow(() -> new AssertionError("Country ZZ not found"));
 
        country.setCountryName("UpdatedLand");
        countryRepository.save(country);
 
        Country updated = countryRepository.findById("ZZ")
                .orElseThrow(() -> new AssertionError("Country ZZ not found after update"));
 
        assertThat(updated.getCountryName()).isEqualTo("UpdatedLand");
    }
 
    @Test
    @Order(11)
    @DisplayName("deleteById() – should remove the country and findById should return empty")
    void testDeleteById() {
        countryRepository.deleteById("ZZ");
 
        Optional<Country> deleted = countryRepository.findById("ZZ");
        assertThat(deleted).isNotPresent();
 
        // Re-save so @AfterEach tearDown doesn't fail trying to delete a gone row.
        countryRepository.save(testCountry);
    }
 
    @Test
    @Order(12)
    @DisplayName("count() – should be greater than zero after at least one save")
    void testCount() {
        long count = countryRepository.count();
 
        assertThat(count).isGreaterThan(0);
    }
}
