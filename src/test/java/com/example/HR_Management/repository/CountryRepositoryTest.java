package com.example.HR_Management.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import com.example.HR_Management.entity.Country;
import com.example.HR_Management.entity.Region;
import com.example.HR_Management.repository.CountryRepository;

import jakarta.persistence.EntityManager;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class CountryRepositoryTest {
	@Autowired
    CountryRepository countryRepository;
 
    @Autowired
    private EntityManager entityManager;
 
    private long regionCounter = 1;
 
    private Region createRegion() {
        Region region = new Region();
        region.setRegionId(regionCounter++);
        region.setRegionName("Region-" + region.getRegionId());
        entityManager.persist(region);
        
        return region;
    }
 
    @BeforeEach
    void setUp() {
        regionCounter = 1;
    }
    
    @Test
    void testFindByCountryNameContainingIgnoreCase_noMatch_returnsEmptyList() {
        Region region = createRegion();
        
 
        List<Country> results =
            countryRepository.findByCountryNameContainingIgnoreCase("neverland");
 
        assertThat(results).isEmpty();
    }
    
    @Test
    void testFindByRegionId_regionWithNoCountries_returnsEmptyList() {
        Region emptyRegion = createRegion();
        Region usedRegion = createRegion();
        countryRepository.save(new Country("US", "United States of America", usedRegion));
 
        List<Country> results = countryRepository.findByRegion_RegionId(emptyRegion.getRegionId());
 
        assertThat(results).isEmpty();
    }
    
    @Test
    void testFindAll_returnsAllSeededCountries() {
        Region region = createRegion();
        countryRepository.save(new Country("US", "United States of America", region));
        countryRepository.save(new Country("CA", "Canada", region));
        countryRepository.save(new Country("UK", "United Kingdom", region));
        countryRepository.save(new Country("DE", "Germany", region));
 
        List<Country> all = countryRepository.findAll();
 
        assertThat(all).hasSize(4);
        assertThat(all)
            .extracting(Country::getCountryId)
            .containsExactlyInAnyOrder("US", "CA", "UK", "DE");
    }
    @Test
    void testFindByRegionId_validRegion_returnsMatchingCountries() {
        Region northAmerica = createRegion();
        Region europe = createRegion();
 
        countryRepository.save(new Country("US", "United States of America", northAmerica));
        countryRepository.save(new Country("CA", "Canada", northAmerica));
        countryRepository.save(new Country("UK", "United Kingdom", europe));
 
        List<Country> results = countryRepository.findByRegion_RegionId(northAmerica.getRegionId());
 
        assertThat(results).hasSize(2);
        assertThat(results)
            .extracting(Country::getCountryId)
            .containsExactlyInAnyOrder("US", "CA");
    }
    @Test
    void testFindByCountryNameContainingIgnoreCase_partialMatch_returnsResults() {
        Region region = createRegion();
        countryRepository.save(new Country("US", "United States of America", region));
        countryRepository.save(new Country("UK", "United Kingdom", region));
        countryRepository.save(new Country("DE", "Germany", region));
 
        List<Country> results =
            countryRepository.findByCountryNameContainingIgnoreCase("united");
 
        assertThat(results).isNotEmpty();
        assertThat(results)
            .extracting(Country::getCountryName)
            .anySatisfy(name -> assertThat(name).containsIgnoringCase("united"));
    }
    
    @Test
    void testSaveCountry_duplicatePrimaryKey_throwsException() {
        Region region = createRegion();
        countryRepository.save(new Country("US", "United States of America", region));
        countryRepository.flush();
 
        Country duplicate = new Country("US", "United States Again", region);
 
        assertThatThrownBy(() -> {
            countryRepository.save(duplicate);
            countryRepository.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }
    @Test
    void testCount_returnsCorrectTotal() {
        Region region = createRegion();
        countryRepository.save(new Country("US", "United States of America", region));
        countryRepository.save(new Country("CA", "Canada", region));
        countryRepository.save(new Country("UK", "United Kingdom", region));
        countryRepository.save(new Country("DE", "Germany", region));
 
        long count = countryRepository.count();
 
        assertThat(count).isEqualTo(4L);
    }
    @Test
    void testExistsById_existingAndNonExisting_returnsCorrectBooleans() {
        Region region = createRegion();
        countryRepository.save(new Country("DE", "Germany", region));
 
        assertThat(countryRepository.existsById("DE")).isTrue();
        assertThat(countryRepository.existsById("XY")).isFalse();
    }
	
	@Test
	@Order(1)
    void testSaveCountry_returnsEntityWithCorrectFields() {
        Region region = createRegion();
        Country country = new Country("USA", "United States", region);
 
        Country saved = countryRepository.save(country);
 
        assertThat(saved).isNotNull();
        assertThat(saved.getCountryId()).isEqualTo("USA");
        assertThat(saved.getCountryName()).isEqualTo("United States");
    }
 
    @Test
    @Order(2)
    void testSaveCountry_idTooLong_throwsException() {
        Region region = createRegion();
        Country country = new Country("TOOLONG", "Some Country", region);
 
        assertThatThrownBy(() -> {
            countryRepository.save(country);
            countryRepository.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }
 
    @Test
    void testSaveCountry_nameTooLong_throwsException() {
        Region region = createRegion();
        String longName = "A".repeat(61);
        Country country = new Country("XT", longName, region);
 
        assertThatThrownBy(() -> {
            countryRepository.save(country);
            countryRepository.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }
 
    @Test
    void testFindCountryById_existingId_returnsCorrectCountry() {
        Region region = createRegion();
        countryRepository.save(new Country("UK", "United Kingdom", region));
 
        Country found = countryRepository.findById("UK").orElse(null);
 
        assertThat(found).isNotNull();
        assertThat(found.getCountryName()).isEqualTo("United Kingdom");
    }
 
    
    @Test
    void testUpdateCountryName_persistsNewName() {
        Region region = createRegion();
        countryRepository.save(new Country("DE", "Germany", region));
 
        Country germany = countryRepository.findById("DE").orElseThrow();
        germany.setCountryName("Federal Republic of Germany");
        countryRepository.save(germany);
        countryRepository.flush();
 
        Country updated = countryRepository.findById("DE").orElseThrow();
        assertThat(updated.getCountryName()).isEqualTo("Federal Republic of Germany");
    }
 
    @Test
    void testSaveCountry_nonExistentRegion_throwsForeignKeyViolation() {
        Region ghost = new Region();
        ghost.setRegionId(999L);
        ghost.setRegionName("Ghost Region");
        // intentionally NOT persisted
 
        Country country = new Country("GH", "Ghostland", ghost);
 
        assertThatThrownBy(() -> {
            countryRepository.save(country);
            countryRepository.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }
    @Test
    void testSaveCountry_nullName_throwsException() {
        Region region = createRegion();
        Country country = new Country("NU", null, region);
 
        assertThatThrownBy(() -> {
            countryRepository.save(country);
            countryRepository.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }
 
}
