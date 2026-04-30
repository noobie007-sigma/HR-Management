package com.example.HR_Management.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.example.HR_Management.entity.Country;
import com.example.HR_Management.entity.Region;
import com.example.HR_Management.repository.CountryRepository;

import jakarta.persistence.EntityManager;

@DataJpaTest
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
	@Test
	void testFindCountryById() {
        Region region = createRegion();

        Country country = new Country("USA", "United States", region);
        countryRepository.save(country);

        Country found = countryRepository.findById("USA").orElse(null);

        assertThat(found).isNotNull();
        assertThat(found.getCountryName()).isEqualTo("United States");
    }
}
