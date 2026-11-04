package com.smartparking.model;
import java.math.BigDecimal;
public final class Bike extends Vehicle { public Bike(String r,String o){super(r,o);} public String getType(){return "BIKE";} public int requiredSize(){return 1;} public BigDecimal hourlyRate(){return new BigDecimal("20.00");} }
