package com.smartparking.strategy;
import com.smartparking.interfaces.PaymentStrategy;import java.math.BigDecimal;
public class CardPayment implements PaymentStrategy { public String method(){return "CARD";} public boolean processPayment(BigDecimal amount){return amount!=null&&amount.signum()>=0;} }
