package com.example.HR_Management.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.TestPropertySource;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.example.HR_Management.entity.Region;

import jakarta.validation.ConstraintViolation;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(locations = "classpath:application.properties")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Import(LocalValidatorFactoryBean.class)
class RegionRepositoryTest {

    @Autowired
    private RegionRepository regionRepository;

    private static final BigDecimal TEST_REGION_ID = BigDecimal.valueOf(99);
    private static final String TEST_REGION_NAME = "Test Region";

    @AfterEach
    void cleanUp() {
        if (regionRepository.existsById(TEST_REGION_ID)) {
            regionRepository.deleteById(TEST_REGION_ID);
        }
    }

    // R-REP-01
    @Test
    @Order(1)
    @DisplayName("findAll() returns non-empty list")
    void findAll_WhenDataExists_ReturnsNonEmptyList() {
        List<Region> regions = regionRepository.findAll();

        assertThat(regions).isNotNull();
        assertThat(regions).isNotEmpty();
    }

    // R-REP-02
    @Test
    @Order(2)
    @DisplayName("findAll() returns valid region IDs")
    void findAll_ContainsValidRegions() {
        List<Region> regions = regionRepository.findAll();

        assertThat(regions).isNotEmpty();
        assertThat(regions)
                .extracting(Region::getRegionId)
                .allMatch(id -> id != null);
    }

    // R-REP-03
    @Test
    @Order(3)
    @DisplayName("findById() returns region for existing ID")
    void findById_WhenValidId_ReturnsRegion() {
        BigDecimal id = regionRepository.findAll().get(0).getRegionId();

        Optional<Region> result = regionRepository.findById(id);

        assertThat(result).isPresent();
        assertThat(result.get().getRegionId()).isEqualTo(id);
    }

    // R-REP-04
    @Test
    @Order(4)
    @DisplayName("findById() returns empty for non-existing ID")
    void findById_WhenIdNotFound_ReturnsEmpty() {
        Optional<Region> result = regionRepository.findById(BigDecimal.valueOf(999));

        assertThat(result).isNotPresent();
    }

    // R-REP-05
    @Test
    @Order(5)
    @DisplayName("save() inserts new region")
    void save_WhenValidRegion_PersistsSuccessfully() {
        Region region = new Region();
        region.setRegionId(TEST_REGION_ID);
        region.setRegionName(TEST_REGION_NAME);

        regionRepository.save(region);

        assertThat(regionRepository.existsById(TEST_REGION_ID)).isTrue();
    }

    // R-REP-06
    @Test
    @Order(6)
    @DisplayName("save(null) throws exception")
    void save_WhenNullObject_ThrowsException() {
        assertThatThrownBy(() -> regionRepository.save(null))
                .isInstanceOfAny(IllegalArgumentException.class,
                        InvalidDataAccessApiUsageException.class);
    }

    // R-REP-07
    @Test
    @Order(7)
    @DisplayName("save() updates existing region")
    void save_WhenUpdatingExistingRegion_PersistsUpdatedName() {
        Region region = new Region();
        region.setRegionId(TEST_REGION_ID);
        region.setRegionName(TEST_REGION_NAME);

        regionRepository.save(region);

        region.setRegionName("Updated");
        regionRepository.save(region);

        Region updated = regionRepository.findById(TEST_REGION_ID).get();

        assertThat(updated.getRegionName()).isEqualTo("Updated");
    }

    // R-REP-08
    @Test
    @Order(8)
    @DisplayName("save() creates new entity if ID not exists")
    void save_WithNonExistingId_CreatesNewEntity() {
        assertThat(regionRepository.existsById(TEST_REGION_ID)).isFalse();

        Region region = new Region();
        region.setRegionId(TEST_REGION_ID);
        region.setRegionName("New");

        regionRepository.save(region);

        assertThat(regionRepository.existsById(TEST_REGION_ID)).isTrue();
    }

    @Autowired
    private jakarta.validation.Validator validator;

    @Test
    @Order(9)
    @DisplayName("save() with null regionName - check behavior")
    void save_WhenRegionNameIsNull_CheckBehavior() {
        Region region = new Region();
        region.setRegionId(TEST_REGION_ID);
        region.setRegionName(null);

        Set<ConstraintViolation<Region>> violations = validator.validate(region);

        assertThat(violations).isNotEmpty();
        assertThat(violations)
            .anyMatch(v -> v.getPropertyPath().toString().equals("regionName"));
    }
    // R-REP-10
    @Test
    @Order(10)
    @DisplayName("save() with same ID updates entity")
    void save_WhenDuplicateRegionId_UpdatesExisting() {
        Region region = new Region();
        region.setRegionId(TEST_REGION_ID);
        region.setRegionName("First");

        regionRepository.save(region);

        Region duplicate = new Region();
        duplicate.setRegionId(TEST_REGION_ID);
        duplicate.setRegionName("Updated");

        regionRepository.save(duplicate);

        Region result = regionRepository.findById(TEST_REGION_ID).get();

        assertThat(result.getRegionName()).isEqualTo("Updated");
    }

    // R-REP-11
    @Test
    @Order(11)
    @DisplayName("existsById() returns true for existing ID")
    void existsById_WhenIdExists_ReturnsTrue() {
        BigDecimal id = regionRepository.findAll().get(0).getRegionId();

        assertThat(regionRepository.existsById(id)).isTrue();
    }

    // R-REP-12
    @Test
    @Order(12)
    @DisplayName("existsById() returns false for non-existing ID")
    void existsById_WhenIdNotFound_ReturnsFalse() {
        assertThat(regionRepository.existsById(BigDecimal.valueOf(999))).isFalse();
    }

    // R-REP-13
    @Test
    @Order(13)
    @DisplayName("count() returns number of records")
    void count_ReturnsAtLeastOne() {
        long count = regionRepository.count();

        assertThat(count).isGreaterThan(0);
    }

    // R-REP-14
    @Test
    @Order(14)
    @DisplayName("findAll(Pageable) works correctly")
    void findAll_WithPageable_ReturnsPaginatedResults() {
        PageRequest pageable = PageRequest.of(0, 2, Sort.by("regionId").ascending());

        Page<Region> page = regionRepository.findAll(pageable);

        assertThat(page).isNotNull();
        assertThat(page.getContent()).hasSizeLessThanOrEqualTo(2);
        assertThat(page.getTotalElements()).isGreaterThan(0);
    }
}