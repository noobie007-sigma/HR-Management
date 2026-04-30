package com.example.HR_Management.projection;

import org.springframework.data.rest.core.config.Projection;
import com.example.HR_Management.entity.Location;

@Projection(name = "locationView", types = Location.class)
public interface LocationProjection {

    String getCity();
    String getPostalCode();
    String getStateProvince();
    String getStreetAddress();
    CountryProjection getCountry();
}