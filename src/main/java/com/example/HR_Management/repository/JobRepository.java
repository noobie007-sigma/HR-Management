package com.example.HR_Management.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import com.example.HR_Management.entity.Job;

@RepositoryRestResource(collectionResourceRel = "jobs", path = "jobs")
public interface JobRepository extends JpaRepository<Job, String> {
	Page<Job> findByJobTitleContainingIgnoreCase(String title, Pageable pageable);
}