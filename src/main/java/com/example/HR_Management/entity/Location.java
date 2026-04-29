package com.example.HR_Management.entity;

import java.util.List;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "locations")
public class Location {

    @Id
    @Column(name = "location_id", precision = 4, scale = 0)
    @NotNull(message = "Location ID is required")
    @Digits(integer = 4, fraction = 0, message = "Location ID must be max 4 digits")
    private Long id;

    @Column(name = "street_address", length = 40)
    @Size(max = 40, message = "Street address cannot exceed 40 characters")
    private String streetAddress;

    @Column(name = "postal_code", length = 12)
    @Size(max = 12, message = "Postal code cannot exceed 12 characters")
    private String postalCode;

    @Column(name = "city", nullable = false, length = 30)
    @NotBlank(message = "City is mandatory")
    @Size(max = 30, message = "City cannot exceed 30 characters")
    private String city;

    @Column(name = "state_province", length = 25)
    @Size(max = 25, message = "State/Province cannot exceed 25 characters")
    private String stateProvince;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "country_id")
    private Country country;

    @OneToMany(mappedBy = "location")
    @JsonIgnore
    private List<Department> departments;

    // ===== Constructors =====

    public Location() {
    }

    public Location(Long id, String streetAddress, String postalCode,
                    String city, String stateProvince, Country country) {
        this.id = id;
        this.streetAddress = streetAddress;
        this.postalCode = postalCode;
        this.city = city;
        this.stateProvince = stateProvince;
        this.country = country;
    }

    // ===== Getters and Setters =====

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStreetAddress() {
        return streetAddress;
    }

    public void setStreetAddress(String streetAddress) {
        this.streetAddress = streetAddress;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getStateProvince() {
        return stateProvince;
    }

    public void setStateProvince(String stateProvince) {
        this.stateProvince = stateProvince;
    }

    public Country getCountry() {
        return country;
    }

    public void setCountry(Country country) {
        this.country = country;
    }

    public List<Department> getDepartments() {
        return departments;
    }

    public void setDepartments(List<Department> departments) {
        this.departments = departments;
    }
}