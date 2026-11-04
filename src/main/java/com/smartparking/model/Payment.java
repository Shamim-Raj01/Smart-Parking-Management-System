package com.smartparking.model;
import java.math.BigDecimal;
import java.time.LocalDateTime;
public record Payment(long id,String ticketId,String registrationNumber,String slotId,LocalDateTime paidAt,String method,BigDecimal amount,String status) { }
