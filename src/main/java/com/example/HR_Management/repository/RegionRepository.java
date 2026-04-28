package com.example.HR_Management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import com.example.HR_Management.entity.Region;

@RepositoryRestResource(collectionResourceRel = "regions", path = "regions")
public interface RegionRepository extends JpaRepository<Region, Long> {
}