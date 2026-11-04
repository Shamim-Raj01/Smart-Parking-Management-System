package com.smartparking.model;
import java.math.BigDecimal;
public final class Truck extends Vehicle { public Truck(String r,String o){super(r,o);} public String getType(){return "TRUCK";} public int requiredSize(){return 3;} public BigDecimal hourlyRate(){return new BigDecimal("80.00");} }
