package com.example.HR_Management.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "regions")
public class Region {

    @Id
    @NotNull(message = "Region ID is required")
    @Positive(message = "Region ID must be a positive number")
    @Column(name = "region_id")
    private BigDecimal regionId;

    @NotBlank(message = "Region name is required")
    @Size(min = 2, max = 25, message = "Region name must be between 2 and 25 characters")
    @Column(name = "region_name", length = 25)
    private String regionName;

    @OneToMany(mappedBy = "region")
    private List<Country> countries;

    public BigDecimal getRegionId() {
        return regionId;
    }

    public void setRegionId(BigDecimal regionId) {
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