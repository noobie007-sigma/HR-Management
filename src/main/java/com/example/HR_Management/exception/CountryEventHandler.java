package com.example.HR_Management.exception;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.HandleBeforeSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.stereotype.Component;

import com.example.HR_Management.entity.Country;
import com.example.HR_Management.repository.CountryRepository;
import com.example.HR_Management.repository.RegionRepository;
@Component
@RepositoryEventHandler
public class CountryEventHandler {

    private final CountryRepository countryRepository;
    private final RegionRepository regionRepository;

    @Autowired
    public CountryEventHandler(CountryRepository countryRepository,
                               RegionRepository regionRepository) {
        this.countryRepository = countryRepository;
        this.regionRepository = regionRepository;
    }

    @HandleBeforeCreate
    public void handleBeforeCreate(Country country) {
        validateDuplicate(country);
        validateRegion(country);
    }

    @HandleBeforeSave
    public void handleBeforeSave(Country country) {
        validateRegion(country);
    }

    private void validateDuplicate(Country country) {
        if (countryRepository.existsById(country.getCountryId())) {
            throw new RuntimeException("Country already exists"); 
        }
    }

    private void validateRegion(Country country) {
        if (country.getRegion() == null ||
            country.getRegion().getRegionId() == null ||
            !regionRepository.existsById(country.getRegion().getRegionId())) {

            throw new IllegalArgumentException("Invalid region reference");
        }
    }
}