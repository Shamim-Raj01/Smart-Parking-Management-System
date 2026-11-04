package com.smartparking.strategy;
import com.smartparking.interfaces.PaymentStrategy;import java.math.BigDecimal;
public class CashPayment implements PaymentStrategy { public String method(){return "CASH";} public boolean processPayment(BigDecimal amount){return amount!=null&&amount.signum()>=0;} }
