package com.smartparking.interfaces;
import java.math.BigDecimal;
public interface PaymentStrategy { String method(); boolean processPayment(BigDecimal amount); }
