package com.example.HR_Management.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name="countries")
public class Country {
	@Id
	@Column(name="country_id")
	private String country_id;
	
	@Column(name="country_name")
	private String country_name;
	
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="region_id",nullable=false)
	private Region region;

	public Country() {
		super();
	}

	public Country(String country_id, String country_name, Region region) {
		super();
		this.country_id = country_id;
		this.country_name = country_name;
		this.region = region;
	}

	public String getCountry_id() {
		return country_id;
	}

	public void setCountry_id(String country_id) {
		this.country_id = country_id;
	}

	public String getCountry_name() {
		return country_name;
	}

	public void setCountry_name(String country_name) {
		this.country_name = country_name;
	}

	public Region getRegion() {
		return region;
	}

	public void setRegion(Region region) {
		this.region = region;
	}
}
