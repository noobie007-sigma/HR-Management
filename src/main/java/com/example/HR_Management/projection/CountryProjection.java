package com.example.HR_Management.projection;

import org.springframework.data.rest.core.config.Projection;
import com.example.HR_Management.entity.Country;

// The 'name' value becomes the query parameter:
//   GET /api/v1/countries?projection=countryWithRegion
@Projection(name = "countryWithRegion", types = { Country.class })
public interface CountryProjection {
    String getCountryId();
    String getCountryName();

    // Nested projection — Spring Data REST inlines this as a JSON object,
    // not as a link. Only regionId and regionName are included.
    RegionSummary getRegion();

    interface RegionSummary {
        java.math.BigDecimal getRegionId();
        String getRegionName();
    }
}