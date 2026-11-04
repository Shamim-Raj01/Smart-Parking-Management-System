package com.smartparking.dao;
import com.smartparking.database.DBConnection;
import com.smartparking.model.Payment;
import java.sql.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

public class PaymentDAO {
 public void save(Connection c,String ticket,String method,BigDecimal amount)throws SQLException{try(PreparedStatement p=c.prepareStatement("INSERT INTO payments(ticket_id,method,amount,status,paid_at) VALUES(?,?,?,'PAID',?)")){p.setString(1,ticket);p.setString(2,method.toUpperCase(Locale.ROOT));p.setBigDecimal(3,amount);p.setTimestamp(4,Timestamp.valueOf(LocalDateTime.now()));p.executeUpdate();}}
 public Optional<Payment> findByTicket(String ticketId)throws SQLException{try(Connection c=DBConnection.open();PreparedStatement p=c.prepareStatement(query()+" WHERE p.ticket_id=?")){p.setString(1,ticketId);try(ResultSet r=p.executeQuery()){return r.next()?Optional.of(map(r)):Optional.empty();}}}
 public List<Payment> all()throws SQLException{try(Connection c=DBConnection.open();PreparedStatement p=c.prepareStatement(query()+" ORDER BY p.paid_at DESC,p.payment_id DESC");ResultSet r=p.executeQuery()){List<Payment> out=new ArrayList<>();while(r.next())out.add(map(r));return out;}}
 private static String query(){return "SELECT p.payment_id,p.ticket_id,t.registration_number,t.slot_id,p.paid_at,p.method,p.amount,p.status FROM payments p JOIN parking_tickets t ON t.ticket_id=p.ticket_id";}
 private static Payment map(ResultSet r)throws SQLException{return new Payment(r.getLong("payment_id"),r.getString("ticket_id"),r.getString("registration_number"),r.getString("slot_id"),r.getTimestamp("paid_at").toLocalDateTime(),r.getString("method"),r.getBigDecimal("amount"),r.getString("status"));}
}
