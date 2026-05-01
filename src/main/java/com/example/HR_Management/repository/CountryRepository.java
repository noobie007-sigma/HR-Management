package com.example.HR_Management.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import com.example.HR_Management.entity.Country;

@RepositoryRestResource(path = "countries")
public interface CountryRepository extends JpaRepository<Country, String> {
	/*List<Country> findByRegion_RegionId(Long regionId);

	List<Country> findByCountryNameContainingIgnoreCase(String name);*/
	
    //boolean existsByCountryNameIgnoreCase(String countryName);
	Optional<Country> findByCountryNameIgnoreCase(String countryName);
	 
    // Find all countries belonging to a specific region
    List<Country> findByRegion_RegionId(BigDecimal regionId);
 
    // Check whether a country name already exists
    boolean existsByCountryNameIgnoreCase(String countryName);
}
