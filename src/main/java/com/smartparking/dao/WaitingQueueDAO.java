package com.smartparking.dao;
import com.smartparking.database.DBConnection;import java.sql.*;import java.util.*;
public class WaitingQueueDAO {
 public List<String> all() throws SQLException {try(Connection c=DBConnection.open();PreparedStatement p=c.prepareStatement("SELECT registration_number FROM waiting_queue ORDER BY queue_id");ResultSet r=p.executeQuery()){List<String> out=new ArrayList<>();while(r.next())out.add(r.getString(1));return out;}}
 public void add(Connection c,String registration) throws SQLException {try(PreparedStatement p=c.prepareStatement("INSERT IGNORE INTO waiting_queue(registration_number,enqueued_at) VALUES(?,CURRENT_TIMESTAMP)")){p.setString(1,registration);p.executeUpdate();}}
 public void remove(Connection c,String registration) throws SQLException {try(PreparedStatement p=c.prepareStatement("DELETE FROM waiting_queue WHERE registration_number=?")){p.setString(1,registration);p.executeUpdate();}}
 public void add(String registration)throws SQLException {try(Connection c=DBConnection.open()){add(c,registration);}}
 public void remove(String registration)throws SQLException {try(Connection c=DBConnection.open()){remove(c,registration);}}
}
