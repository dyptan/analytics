package com.dyptan.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Embeddable
@Table(name = "Filters")
public class Filter {

    private @Transient short limit = 100;
    private int periodLastDays;
    private String models;
    private String brands;
    private int yearFrom;
    private int yearTo;
    private int priceFrom;
    private int priceTo;
    private int mileageFrom;
    private int mileageTo;

    public Filter() {
    }

    @Override
    public String toString() {
        try {
            return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            return e.getMessage();
        }
    }

    public short getLimit() {
        return this.limit;
    }

    public String getModels() {
        return this.models;
    }

    public String getBrands() {
        return this.brands;
    }

    public int getPeriodRange() {
        return this.periodLastDays;
    }

    public int getYearFrom() {
        return this.yearFrom;
    }

    public int getYearTo() {
        return this.yearTo;
    }

    public int getPriceFrom() {
        return this.priceFrom;
    }

    public int getPriceTo() {
        return this.priceTo;
    }

    public int getMileageFrom() {
        return this.mileageFrom;
    }

    public int getMileageTo() {
        return this.mileageTo;
    }

    public void setLimit(short limit) {
        this.limit = limit;
    }

    public void setModels(String models) {
        this.models = models;
    }

    public void setBrands(String brands) {
        this.brands = brands;
    }

    public void setYearFrom(int yearFrom) {
        this.yearFrom = yearFrom;
    }

    public void setYearTo(int yearTo) {
        this.yearTo = yearTo;
    }

    public void setPriceFrom(int priceFrom) {
        this.priceFrom = priceFrom;
    }

    public void setPriceTo(int priceTo) {
        this.priceTo = priceTo;
    }

    public void setMileageFrom(int mileageFrom) {
        this.mileageFrom = mileageFrom;
    }

    public void setMileageTo(int mileageTo) {
        this.mileageTo = mileageTo;
    }

}

