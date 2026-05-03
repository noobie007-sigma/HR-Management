package com.example.HR_Management.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import com.example.HR_Management.entity.Country;
import com.example.HR_Management.projection.CountryProjection;

@RepositoryRestResource(path = "countries", excerptProjection = CountryProjection.class)
public interface CountryRepository extends JpaRepository<Country, String> {

	
    //boolean existsByCountryNameIgnoreCase(String countryName);
	Optional<Country> findByCountryNameIgnoreCase(String countryName);
	 
    // Find all countries belonging to a specific region
    List<Country> findByRegion_RegionId(BigDecimal regionId);
 
    // Check whether a country name already exists
    boolean existsByCountryNameIgnoreCase(String countryName);
}
