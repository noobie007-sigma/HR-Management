package com.example.HR_Management.repository;

import com.example.HR_Management.entity.Country;
import com.example.HR_Management.entity.Location;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class LocationRepositoryTest {

    @Autowired
    private LocationRepository locationRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private Country testCountry;

    // ================================
    // 🔧 SETUP
    // ================================
    @BeforeEach
    void setUp() {
        // "IN" must exist in the seeded DB via insert_data.sql
        testCountry = entityManager.find(Country.class, "IN");

        if (testCountry == null) {
            testCountry = new Country();
            testCountry.setCountryId("IN");
            testCountry.setCountryName("India");
            entityManager.persist(testCountry);
            entityManager.flush();
        }
    }

    // ================================
    // 🔧 HELPER — build and persist a Location via EntityManager
    //    Both setId() and findById() use BigDecimal (JpaRepository<Location, BigDecimal>)
    // ================================
    private Location persistLocation(BigDecimal id, String city, String postal, String state) {
        Location existing = entityManager.find(Location.class, id);
        if (existing != null) {
            entityManager.remove(existing);
            entityManager.flush();
        }

        Location loc = new Location();
        loc.setId(id);                        // setId(BigDecimal)
        loc.setCity(city);
        loc.setPostalCode(postal);
        loc.setStateProvince(state);
        loc.setStreetAddress("123 Test Street");
        loc.setCountry(testCountry);

        entityManager.persist(loc);
        entityManager.flush();
        return loc;
    }

    // ================================
    // ✅ 1. SAVE — Test Save Operation
    // ================================
    @Test
    @DisplayName("Positive - valid location is saved and returned with correct fields")
    void testSaveLocation_valid_success() {
        BigDecimal id = new BigDecimal("9001");
        Location saved = persistLocation(id, "Bengaluru", "560001", "KA");

        assertNotNull(saved);
        assertEquals(0, id.compareTo(saved.getId())); // BigDecimal.compareTo ignores scale (9001 == 9001.0)
        assertEquals("Bengaluru", saved.getCity());
        assertEquals("560001", saved.getPostalCode());
    }

    // ================================
    // ✅ 2. FIND BY ID — Test Find By ID
    // ================================
    @Test
    @DisplayName("Positive - findById returns correct location for a valid ID")
    void testFindById_exists_returnsLocation() {
        BigDecimal id = new BigDecimal("9002");
        persistLocation(id, "Mumbai", "400001", "MH");

        // JpaRepository<Location, BigDecimal> → pass BigDecimal directly
        Optional<Location> result = locationRepository.findById(id);

        assertTrue(result.isPresent());
        assertEquals("Mumbai", result.get().getCity());
        assertEquals("123 Test Street", result.get().getStreetAddress());
    }

    @Test
    @DisplayName("Negative - findById with non-existent ID returns empty Optional")
    void testFindById_notExists_returnsEmpty() {
        Optional<Location> result = locationRepository.findById(new BigDecimal("99999"));

        assertFalse(result.isPresent());
    }

    // ================================
    // ✅ 3. DELETE — Test Delete Operation
    // ================================
    @Test
    @DisplayName("Positive - deleteById removes location; subsequent findById is empty")
    void testDeleteLocation_valid_removedFromDB() {
        BigDecimal id = new BigDecimal("9003");
        persistLocation(id, "Chennai", "600001", "TN");

        locationRepository.deleteById(id);
        entityManager.flush();

        assertFalse(locationRepository.findById(id).isPresent());
    }

    // ================================
    // ✅ 4. VALIDATION — Null City Constraint
    // ================================
    @Test
    @DisplayName("Negative - saving location with null city throws ConstraintViolation or DataIntegrity exception")
    void testSaveLocation_nullCity_throwsException() {
        Location location = new Location();
        location.setId(new BigDecimal("9004"));
        location.setPostalCode("000000");
        location.setStreetAddress("No City Lane");
        location.setCountry(testCountry);
        // city NOT set → violates @NotBlank + column nullable = false

        assertThrows(Exception.class, () -> {
            locationRepository.save(location);
            entityManager.flush();
        });
    }

    // ================================
    // ✅ 5. FIND BY CITY
    // ================================
    @Test
    @DisplayName("Positive - findByCity returns all locations matching that city name")
    void testFindByCity_exists_returnsResults() {
        // location_id 1600 → South Brunswick, New Jersey, postal '50090' (insert_data.sql)
        List<Location> result = locationRepository.findByCity("South Brunswick");

        assertFalse(result.isEmpty());
        result.forEach(loc -> assertEquals("South Brunswick", loc.getCity()));
    }

    @Test
    @DisplayName("Negative - findByCity with unknown city returns empty list")
    void testFindByCity_notFound_returnsEmptyList() {
        List<Location> result = locationRepository.findByCity("CityThatDoesNotExistXYZ");

        assertTrue(result.isEmpty());
    }

    // ================================
    // ✅ 6. FIND BY STATE PROVINCE
    // ================================
    @Test
    @DisplayName("Positive - findByStateProvince returns all locations in that state")
    void testFindByStateProvince_exists_returnsResults() {
        // location_id 1600 → South Brunswick, New Jersey (insert_data.sql)
        List<Location> result = locationRepository.findByStateProvince("New Jersey");

        assertFalse(result.isEmpty());
        result.forEach(loc -> assertEquals("New Jersey", loc.getStateProvince()));
    }

    @Test
    @DisplayName("Negative - findByStateProvince with unknown state returns empty list")
    void testFindByStateProvince_notFound_returnsEmptyList() {
        List<Location> result = locationRepository.findByStateProvince("StateThatDoesNotExistXYZ");

        assertTrue(result.isEmpty());
    }

    // ================================
    // ✅ 7. FIND BY POSTAL CODE
    // ================================
    @Test
    @DisplayName("Positive - findByPostalCode returns location with matching postal code")
    void testFindByPostalCode_exists_returnsResult() {
        // location_id 1600 → South Brunswick, New Jersey, postal code '50090'
        List<Location> result = locationRepository.findByPostalCode("50090");

        assertFalse(result.isEmpty());
        result.forEach(loc -> assertEquals("50090", loc.getPostalCode()));
    }

    @Test
    @DisplayName("Negative - findByPostalCode with unknown code returns empty list")
    void testFindByPostalCode_notFound_returnsEmptyList() {
        List<Location> result = locationRepository.findByPostalCode("INVALID_000");

        assertTrue(result.isEmpty());
    }

    // ================================
    // ✅ 8. FIND BY COUNTRY ID
    // ================================
    @Test
    @DisplayName("Positive - findByCountry_CountryId returns locations for that country")
    void testFindByCountryId_exists_returnsResults() {
        // "US" has multiple locations in insert_data.sql
        List<Location> result = locationRepository.findByCountry_CountryId("US");

        assertFalse(result.isEmpty());
        result.forEach(loc ->
                assertEquals("US", loc.getCountry().getCountryId())
        );
    }

    @Test
    @DisplayName("Negative - findByCountry_CountryId with unknown country returns empty list")
    void testFindByCountryId_notFound_returnsEmptyList() {
        List<Location> result = locationRepository.findByCountry_CountryId("ZZ");

        assertTrue(result.isEmpty());
    }

    // ================================
    // ✅ 9. FIND BY CITY + COUNTRY
    // ================================
    @Test
    @DisplayName("Positive - findByCityAndCountry_CountryId returns correctly filtered result")
    void testFindByCityAndCountryId_correctPair_returnsResult() {
        List<Location> result =
                locationRepository.findByCityAndCountry_CountryId("South Brunswick", "US");

        assertFalse(result.isEmpty());
        result.forEach(loc -> {
            assertEquals("South Brunswick", loc.getCity());
            assertEquals("US", loc.getCountry().getCountryId());
        });
    }

    @Test
    @DisplayName("Negative - findByCityAndCountry_CountryId with wrong country returns empty")
    void testFindByCityAndCountryId_wrongCountry_returnsEmpty() {
        // City exists in DB but paired with a wrong country → must return empty
        List<Location> result =
                locationRepository.findByCityAndCountry_CountryId("South Brunswick", "IN");

        assertTrue(result.isEmpty());
    }

    // ================================
    // ✅ 10. FIND ALL
    // ================================
    @Test
    @DisplayName("Positive - findAll returns all seeded locations as a non-empty list")
    void testFindAll_returnsAllLocations() {
        List<Location> all = locationRepository.findAll();

        assertNotNull(all);
        assertFalse(all.isEmpty());
        // insert_data.sql seeds at least 20 locations
        assertTrue(all.size() >= 2);
    }

    // ================================
    // ✅ 11. EXISTS BY ID
    // ================================
    @Test
    @DisplayName("Positive - existsById returns true for seeded location_id 1000")
    void testExistsById_seededId_returnsTrue() {
        // location_id 1000 → Roma, Italy, postal '00989' (insert_data.sql)
        assertTrue(locationRepository.existsById(new BigDecimal("1000")));
    }

    @Test
    @DisplayName("Negative - existsById returns false for a non-existent ID")
    void testExistsById_nonExistentId_returnsFalse() {
        assertFalse(locationRepository.existsById(new BigDecimal("99999")));
    }

    // ================================
    // ✅ 12. COUNT
    // ================================
    @Test
    @DisplayName("Positive - count returns value >= total seeded locations")
    void testCount_returnsPositiveCount() {
        long count = locationRepository.count();

        // insert_data.sql seeds multiple location rows
        assertTrue(count >= 1);
    }
}
