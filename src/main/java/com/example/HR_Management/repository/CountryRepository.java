package com.example.HR_Management.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import com.example.HR_Management.entity.Country;

@RepositoryRestResource(path = "countries")
public interface CountryRepository extends JpaRepository<Country, String> {
	List<Country> findByRegion_RegionId(Long regionId);

	List<Country> findByCountryNameContainingIgnoreCase(String name);
}
