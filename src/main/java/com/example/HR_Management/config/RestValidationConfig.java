package com.example.HR_Management.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.event.ValidatingRepositoryEventListener;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

import com.example.HR_Management.entity.Country;

import com.example.HR_Management.entity.Department;
import com.example.HR_Management.entity.Employee;
import com.example.HR_Management.entity.Job;
import com.example.HR_Management.entity.Location;
import com.example.HR_Management.entity.Region;


@Configuration
public class RestValidationConfig implements RepositoryRestConfigurer {

    private final LocalValidatorFactoryBean validator;

    public RestValidationConfig(LocalValidatorFactoryBean validator) {
        this.validator = validator;
    }

    @Override
    public void configureValidatingRepositoryEventListener(
            ValidatingRepositoryEventListener validatingListener) {
    	
        validatingListener.addValidator("beforeCreate", validator);

        validatingListener.addValidator("beforeSave", validator);
    }

    
    @Override
    public void configureRepositoryRestConfiguration(
            RepositoryRestConfiguration config, CorsRegistry cors) {

    	config.exposeIdsFor(Country.class);
    	config.getProjectionConfiguration()
        .addProjection(com.example.HR_Management.projection.CountryProjection.class);


        // Expose entity IDs in all SDR responses
        config.exposeIdsFor(
            Employee.class, Department.class, Job.class,
            Location.class, Country.class, Region.class
        );

        // Allow the Thymeleaf frontend on port 8082
        cors.addMapping("/api/v1/**")
            .allowedOrigins("http://localhost:8082")
            .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            .allowedHeaders("*");

    }
}