package com.smartparking.dao;
import com.smartparking.database.DBConnection;
import com.smartparking.model.*;
import java.sql.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public class ParkingHistoryDAO {
 public void save(Connection c,String ticket,String registration,String slot,LocalDateTime entry,LocalDateTime exit,BigDecimal fee)throws SQLException{try(PreparedStatement p=c.prepareStatement("INSERT INTO parking_history(ticket_id,registration_number,slot_id,entry_time,exit_time,duration_minutes,fee) VALUES(?,?,?,?,?,?,?)")){p.setString(1,ticket);p.setString(2,registration);p.setString(3,slot);p.setTimestamp(4,Timestamp.valueOf(entry));p.setTimestamp(5,Timestamp.valueOf(exit));p.setLong(6,Duration.between(entry,exit).toMinutes());p.setBigDecimal(7,fee);p.executeUpdate();}}
 public List<ParkingTicket> all()throws SQLException{return query(null);}
 public List<ParkingTicket> byRegistration(String registration)throws SQLException{return query(registration);}
 private List<ParkingTicket> query(String registration)throws SQLException{String sql="SELECT h.*,s.floor_number,s.slot_size,s.distance_from_entrance,v.owner_name,v.vehicle_type,p.method,p.status AS payment_status FROM parking_history h JOIN parking_slots s ON s.slot_id=h.slot_id JOIN vehicles v ON v.registration_number=h.registration_number LEFT JOIN payments p ON p.ticket_id=h.ticket_id"+(registration==null?"":" WHERE h.registration_number=?")+" ORDER BY h.exit_time DESC";try(Connection c=DBConnection.open();PreparedStatement p=c.prepareStatement(sql)){if(registration!=null)p.setString(1,registration);try(ResultSet r=p.executeQuery()){List<ParkingTicket> out=new ArrayList<>();while(r.next()){Vehicle v=ParkingTicketDAO.vehicle(r);ParkingSlot s=new ParkingSlot(r.getString("slot_id"),r.getInt("floor_number"),r.getInt("slot_size"),r.getInt("distance_from_entrance"));out.add(new ParkingTicket(r.getString("ticket_id"),v,s,r.getTimestamp("entry_time").toLocalDateTime(),r.getTimestamp("exit_time").toLocalDateTime(),r.getBigDecimal("fee"),r.getString("method"),r.getString("payment_status")));}return out;}}}
}
