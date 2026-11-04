package com.smartparking.dao;
import com.smartparking.database.DBConnection;
import com.smartparking.model.ParkingSlot;
import java.sql.*;
import java.util.*;

public class ParkingSlotDAO {
 public List<ParkingSlot> all()throws SQLException{try(Connection c=DBConnection.open();PreparedStatement p=c.prepareStatement("SELECT * FROM parking_slots ORDER BY distance_from_entrance");ResultSet r=p.executeQuery()){List<ParkingSlot> out=new ArrayList<>();while(r.next())out.add(map(r));return out;}}
 public Optional<ParkingSlot> find(String id)throws SQLException{try(Connection c=DBConnection.open();PreparedStatement p=c.prepareStatement("SELECT * FROM parking_slots WHERE slot_id=?")){p.setString(1,id);try(ResultSet r=p.executeQuery()){return r.next()?Optional.of(map(r)):Optional.empty();}}}
 public List<ParkingSlot> available()throws SQLException{try(Connection c=DBConnection.open();PreparedStatement p=c.prepareStatement("SELECT * FROM parking_slots WHERE available=TRUE ORDER BY distance_from_entrance");ResultSet r=p.executeQuery()){List<ParkingSlot> out=new ArrayList<>();while(r.next())out.add(map(r));return out;}}
 public void setAvailable(Connection c,String id,boolean available)throws SQLException{try(PreparedStatement p=c.prepareStatement("UPDATE parking_slots SET available=? WHERE slot_id=? AND available=?")){p.setBoolean(1,available);p.setString(2,id);p.setBoolean(3,!available);if(p.executeUpdate()!=1)throw new SQLException("Slot is missing or its availability changed concurrently: "+id);}}
 public void allocate(String id)throws SQLException{setAvailabilityInTransaction(id,false);}
 public void release(String id)throws SQLException{setAvailabilityInTransaction(id,true);}
 private void setAvailabilityInTransaction(String id,boolean available)throws SQLException{try(Connection c=DBConnection.open()){c.setAutoCommit(false);try{setAvailable(c,id,available);c.commit();}catch(SQLException e){try{c.rollback();}catch(SQLException rollback){e.addSuppressed(rollback);}throw e;}}}
 private static ParkingSlot map(ResultSet r)throws SQLException{ParkingSlot s=new ParkingSlot(r.getString("slot_id"),r.getInt("floor_number"),r.getInt("slot_size"),r.getInt("distance_from_entrance"));if(!r.getBoolean("available"))s.occupy();return s;}
}
