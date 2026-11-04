package com.smartparking.model;
import java.time.*;import java.math.*;
public record ParkingTicket(String id,Vehicle vehicle,ParkingSlot slot,LocalDateTime entryTime,LocalDateTime exitTime,BigDecimal fee,String paymentMethod,String paymentStatus) { public boolean active(){return exitTime==null;} }
