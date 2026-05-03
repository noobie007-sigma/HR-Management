package com.example.HR_Management.projection;

import org.springframework.data.rest.core.config.Projection;
import com.example.HR_Management.entity.Country;

@Projection(name = "countryWithRegion", types = { Country.class })
public interface CountryProjection {
    String getCountryId();
    String getCountryName();

    
    RegionSummary getRegion();

    interface RegionSummary {
        java.math.BigDecimal getRegionId();
        String getRegionName();
    }
}