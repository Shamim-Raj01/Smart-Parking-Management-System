package com.smartparking.model;
import java.math.BigDecimal;
public abstract class Vehicle {
    private final String registrationNumber; private String ownerName;
    protected Vehicle(String registrationNumber,String ownerName){if(registrationNumber==null||!registrationNumber.matches("[A-Za-z0-9-]{3,15}")||ownerName==null||ownerName.isBlank())throw new IllegalArgumentException("Valid registration and owner name required");this.registrationNumber=registrationNumber.trim().toUpperCase();this.ownerName=ownerName.trim();}
    public String getRegistrationNumber(){return registrationNumber;} public String getOwnerName(){return ownerName;} public void setOwnerName(String ownerName){if(ownerName==null||ownerName.isBlank())throw new IllegalArgumentException("Owner name is required");this.ownerName=ownerName.trim();}
    public abstract String getType(); public abstract int requiredSize(); public abstract BigDecimal hourlyRate();
}
