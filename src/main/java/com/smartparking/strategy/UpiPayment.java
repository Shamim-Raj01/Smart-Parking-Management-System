package com.smartparking.strategy;
import com.smartparking.interfaces.PaymentStrategy;import java.math.BigDecimal;
public class UpiPayment implements PaymentStrategy { public String method(){return "UPI";} public boolean processPayment(BigDecimal amount){return amount!=null&&amount.signum()>=0;} }
