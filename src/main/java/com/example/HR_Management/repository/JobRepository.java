package com.example.HR_Management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import com.example.HR_Management.entity.Jobs;

@RepositoryRestResource(path = "jobs")
public interface JobRepository extends JpaRepository<Jobs, String> {

}