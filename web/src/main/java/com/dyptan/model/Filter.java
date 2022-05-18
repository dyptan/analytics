package com.dyptan.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.persistence.Embeddable;
import javax.persistence.Table;
import javax.persistence.Transient;

@Embeddable
@Table(name = "Filters")
public class Filter {

    private @Transient short limit = 100;
    private String models ;
    private String brands ;
    private @Transient Period periodMultiplier ;
    private @Transient int periodRange ;
    private int yearFrom ;
    private int yearTo ;
    private int priceFrom;
    private int priceTo ;

    public Filter() {
    }

    @Override
    public String toString(){
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
    public Period getPeriodMultiplier() {
        return this.periodMultiplier;
    }
    public int getPeriodRange() {
        return this.periodRange;
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
    public void setLimit(short limit) {
        this.limit = limit;
    }
    public void setModels(String models) {
        this.models = models;
    }
    public void setBrands(String brands) {
        this.brands = brands;
    }
    public void setPeriodMultiplier(Period periodMultiplier) {
        this.periodMultiplier = periodMultiplier;
    }
    public void setPeriodRange(int periodRange) {
        this.periodRange = periodRange;
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



    public enum Period{
        DAYS ("d"),
        WEEKS("w"),
        MONTHS("M");
        private final String abbreviation;
        Period(String abbreviation){
            this.abbreviation = abbreviation;
        }
        public String getAbbreviation() {return abbreviation;}
    }
}


