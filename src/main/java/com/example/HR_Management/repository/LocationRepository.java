package com.example.HR_Management.repository;

import com.example.HR_Management.entity.Location;
import com.example.HR_Management.projection.LocationProjection;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.math.BigDecimal;
import java.util.List;

@RepositoryRestResource(path = "locations" , excerptProjection = LocationProjection.class)
public interface LocationRepository extends JpaRepository<Location, BigDecimal> {

    List<Location> findByCity(String city);

    List<Location> findByStateProvince(String stateProvince);

    List<Location> findByPostalCode(String postalCode);

    List<Location> findByCountry_CountryId(String countryId);

    List<Location> findByCityAndCountry_CountryId(String city, String countryId);
}