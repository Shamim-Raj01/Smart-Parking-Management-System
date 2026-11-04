package com.smartparking.model;
import java.math.BigDecimal;
public final class Car extends Vehicle { public Car(String r,String o){super(r,o);} public String getType(){return "CAR";} public int requiredSize(){return 2;} public BigDecimal hourlyRate(){return new BigDecimal("40.00");} }
