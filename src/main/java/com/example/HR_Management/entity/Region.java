package com.example.HR_Management.entity;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "regions")  // Fix 1: map to correct table name
public class Region {

    @Id
    // Fix 2: remove @GeneratedValue — your DB uses manual IDs (10, 20, 30...)
    @Column(name = "region_id")
    private Long regionId;

    @Column(name = "region_name")
    private String regionName;

    @OneToMany(mappedBy = "region")
    private List<Country> countries;

    public Long getRegionId() {
        return regionId;
    }

    public void setRegionId(Long regionId) {
        this.regionId = regionId;
    }

    public String getRegionName() {
        return regionName;
    }

    public void setRegionName(String regionName) {
        this.regionName = regionName;
    }

    public List<Country> getCountries() {
        return countries;
    }

    public void setCountries(List<Country> countries) {
        this.countries = countries;
    }
}