package com.example.HR_Management.repository;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.validation.annotation.Validated;

import com.example.HR_Management.entity.Region;

import java.util.List;
import java.util.Optional;

@Validated
@RepositoryRestResource(collectionResourceRel = "regions", path = "regions")
public interface RegionRepository extends JpaRepository<Region, Long> {

    
    Optional<Region> findByRegionNameIgnoreCase(
        @NotBlank(message = "Region name must not be blank")
        @Size(min = 2, max = 25, message = "Region name must be between 2 and 25 characters")
        String regionName
    );

    
    List<Region> findByRegionNameContainingIgnoreCase(
        @NotBlank(message = "Search keyword must not be blank")
        String keyword
    );
}