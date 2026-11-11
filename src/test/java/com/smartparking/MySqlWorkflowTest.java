package com.smartparking;

import com.smartparking.dao.*;
import com.smartparking.database.DBConnection;
import com.smartparking.model.*;
import com.smartparking.service.ParkingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

/** Destructive operations are scoped to these test-only registrations/slots and are cleaned in finally. */
@EnabledIfSystemProperty(named="parking.integration",matches="true")
class MySqlWorkflowTest {
 private static final String CAR1="TEST-CAR-101",CAR2="TEST-CAR-102",CAR3="TEST-CAR-103",BIKE="TEST-BIKE-101";
 private static final List<String> REGISTRATIONS=List.of(CAR1,CAR2,CAR3,BIKE);
 private static final List<String> TEST_SLOTS=List.of("P1","P2","P3","PX");
 private final VehicleDAO vehicles=new VehicleDAO();private final ParkingSlotDAO slots=new ParkingSlotDAO();private final ParkingTicketDAO tickets=new ParkingTicketDAO();private final PaymentDAO payments=new PaymentDAO();private final ParkingHistoryDAO history=new ParkingHistoryDAO();private final WaitingQueueDAO waiting=new WaitingQueueDAO();

 @Test void jdbcCrudParkingQueuePaymentsRecoveryAndRollback() throws Exception {
  Map<String,Boolean> sampleAvailability=new LinkedHashMap<>();boolean safeToCleanup=false;
  try {
   try(Connection c=DBConnection.open()){
    try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT DATABASE()")){assertTrue(r.next());assertEquals("smart_parking_test_db",r.getString(1));}
    for(String reg:REGISTRATIONS)assertTrue(vehicles.find(reg).isEmpty(),"Refusing to overwrite existing test vehicle "+reg);
    for(String id:TEST_SLOTS)assertTrue(slots.find(id).isEmpty(),"Refusing to overwrite existing test slot "+id);
    try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT COUNT(*) FROM waiting_queue")){assertTrue(r.next());assertEquals(0,r.getLong(1),"Refusing to run while existing vehicles are waiting");}
    try(PreparedStatement p=c.prepareStatement("SELECT slot_id,available FROM parking_slots" );ResultSet r=p.executeQuery()){while(r.next())sampleAvailability.put(r.getString(1),r.getBoolean(2));}
    safeToCleanup=true;
    c.setAutoCommit(false);
    try(PreparedStatement p=c.prepareStatement("UPDATE parking_slots SET available=FALSE")){p.executeUpdate();}
    try(PreparedStatement p=c.prepareStatement("INSERT INTO parking_slots(slot_id,floor_number,slot_size,distance_from_entrance,available) VALUES(?,1,?,?,TRUE)")){
     addSlot(p,"P1",2,20);addSlot(p,"P2",2,5);addSlot(p,"P3",1,3);addSlot(p,"PX",1,50);
    }
    c.commit();
   }

   ParkingService service=ParkingService.persistent();
   service.register(new Car(CAR1,"Test Car One"));service.register(new Car(CAR2,"Test Car Two"));service.register(new Car(CAR3,"Test Car Three"));service.register(new Bike(BIKE,"Test Bike"));
   assertEquals("CAR",vehicles.find(CAR1).orElseThrow().getType());assertEquals(4,vehicles.all().stream().filter(v->REGISTRATIONS.contains(v.getRegistrationNumber())).count());assertDbCount("SELECT COUNT(*) FROM vehicles WHERE registration_number=?",CAR1,1);assertThrows(SQLException.class,()->vehicles.save(new Car(CAR1,"Duplicate")));
   assertEquals("P2",service.enter(CAR1).slot().getId());assertDbCount("SELECT COUNT(*) FROM parking_tickets WHERE registration_number=? AND status='ACTIVE'",CAR1,1);assertDbCount("SELECT COUNT(*) FROM parking_slots WHERE slot_id='P2' AND available=FALSE",List.of(),1);
   assertEquals("P1",service.enter(CAR2).slot().getId());assertEquals("P3",service.enter(BIKE).slot().getId());assertNull(service.enter(CAR3));assertEquals(List.of(CAR3),waiting.all());assertDbCount("SELECT COUNT(*) FROM waiting_queue WHERE registration_number=?",CAR3,1);
   service=ParkingService.persistent();assertEquals(List.of(CAR3),service.waitingVehicles());assertDbCount("SELECT COUNT(*) FROM waiting_queue WHERE registration_number=?",CAR3,1);assertEquals(3,service.activeTickets().stream().filter(t->REGISTRATIONS.contains(t.vehicle().getRegistrationNumber())).count());assertEquals("P2",service.exit(CAR1,"CASH").slot().getId());assertEquals("P2",tickets.active(CAR3).orElseThrow().slot().getId());assertDbCount("SELECT COUNT(*) FROM parking_tickets WHERE registration_number=? AND status='ACTIVE' AND slot_id='P2'",CAR3,1);assertTrue(waiting.all().isEmpty());assertDbCount("SELECT COUNT(*) FROM waiting_queue WHERE registration_number=?",CAR3,0);

   service=ParkingService.persistent();ParkingTicket car2=service.activeTickets().stream().filter(t->t.vehicle().getRegistrationNumber().equals(CAR2)).findFirst().orElseThrow();
   try(Connection c=DBConnection.open()) {c.setAutoCommit(false);try{tickets.close(c,car2,LocalDateTime.now(),new BigDecimal("40.00"));payments.save(c,car2.id(),"UPI",new BigDecimal("40.00"));payments.save(c,car2.id(),"UPI",new BigDecimal("40.00"));fail("duplicate payment must abort the transaction");}catch(SQLException expected){c.rollback();}}
   assertTrue(tickets.active(CAR2).isPresent());assertTrue(payments.findByTicket(car2.id()).isEmpty());assertTrue(history.byRegistration(CAR2).isEmpty());assertDbCount("SELECT COUNT(*) FROM parking_tickets WHERE ticket_id=? AND status='ACTIVE'",car2.id(),1);assertDbCount("SELECT COUNT(*) FROM payments WHERE ticket_id=?",car2.id(),0);assertDbCount("SELECT COUNT(*) FROM parking_history WHERE registration_number=?",CAR2,0);assertFalse(slots.find("P1").orElseThrow().isAvailable());

   try(Connection c=DBConnection.open();PreparedStatement p=c.prepareStatement("UPDATE parking_tickets SET entry_time=? WHERE ticket_id=?")){p.setTimestamp(1,Timestamp.valueOf(LocalDateTime.now().minusMinutes(61)));p.setString(2,car2.id());assertEquals(1,p.executeUpdate());}
   service=ParkingService.persistent();ParkingTicket paidCar=service.exit(CAR2,"UPI");assertEquals(new BigDecimal("80.00"),paidCar.fee());assertEquals("UPI",payments.findByTicket(paidCar.id()).orElseThrow().method());assertDbCount("SELECT COUNT(*) FROM payments WHERE ticket_id=? AND method='UPI' AND amount=80.00 AND status='PAID'",paidCar.id(),1);assertDbCount("SELECT COUNT(*) FROM parking_history WHERE ticket_id=? AND fee=80.00",paidCar.id(),1);assertDbCount("SELECT COUNT(*) FROM parking_tickets WHERE ticket_id=? AND status='CLOSED' AND amount=80.00",paidCar.id(),1);
   ParkingTicket paidBike=service.exit(BIKE,"CARD");assertEquals("CARD",payments.findByTicket(paidBike.id()).orElseThrow().method());
   ParkingTicket paidQueued=service.exit(CAR3,"CASH");assertEquals("CASH",payments.findByTicket(paidQueued.id()).orElseThrow().method());
   assertTrue(history.byRegistration(CAR2).stream().anyMatch(t->"UPI".equals(t.paymentMethod())&&t.fee().compareTo(new BigDecimal("80.00"))==0));assertEquals(4,history.all().stream().filter(t->REGISTRATIONS.contains(t.vehicle().getRegistrationNumber())).count());assertDbCount("SELECT COUNT(*) FROM payments p JOIN parking_tickets t ON p.ticket_id=t.ticket_id WHERE t.registration_number IN (?,?,?,?)",REGISTRATIONS,4);assertDbCount("SELECT COUNT(*) FROM parking_history WHERE registration_number IN (?,?,?,?)",REGISTRATIONS,4);
   assertTrue(slots.find("P1").orElseThrow().isAvailable());assertTrue(slots.find("P2").orElseThrow().isAvailable());assertTrue(slots.find("P3").orElseThrow().isAvailable());assertTrue(service.activeTickets().stream().noneMatch(t->REGISTRATIONS.contains(t.vehicle().getRegistrationNumber())));
   service=ParkingService.persistent();assertEquals(4,service.vehicles().stream().filter(v->REGISTRATIONS.contains(v.getRegistrationNumber())).count());assertEquals(4,service.history().stream().filter(t->REGISTRATIONS.contains(t.vehicle().getRegistrationNumber())).count());assertEquals(4,service.payments().stream().filter(p->REGISTRATIONS.contains(p.registrationNumber())).count());assertTrue(service.waitingVehicles().isEmpty());assertDbCount("SELECT COUNT(*) FROM vehicles WHERE registration_number IN (?,?,?,?)",REGISTRATIONS,4);assertDbCount("SELECT COUNT(*) FROM parking_tickets WHERE registration_number IN (?,?,?,?) AND status='ACTIVE'",REGISTRATIONS,0);

   rollbackFailedEntry();concurrentSlotClaim();
  } finally {if(safeToCleanup){cleanup(sampleAvailability);assertDbCount("SELECT COUNT(*) FROM vehicles WHERE registration_number IN (?,?,?,?)",REGISTRATIONS,0);assertDbCount("SELECT COUNT(*) FROM parking_slots WHERE slot_id IN ('P1','P2','P3','PX')",List.of(),0);}}
 }
 private void rollbackFailedEntry()throws Exception{ParkingSlot slot=slots.find("PX").orElseThrow();Vehicle missing=new Car("NO-SUCH-VEHICLE","Missing");ParkingTicket invalid=new ParkingTicket(UUID.randomUUID().toString(),missing,slot,LocalDateTime.now(),null,null,null,null);try(Connection c=DBConnection.open()){c.setAutoCommit(false);try{slots.setAvailable(c,"PX",false);tickets.insert(c,invalid);fail("foreign key should reject unknown vehicle");}catch(SQLException expected){c.rollback();}}assertTrue(slots.find("PX").orElseThrow().isAvailable());assertTrue(tickets.active("NO-SUCH-VEHICLE").isEmpty());assertDbCount("SELECT COUNT(*) FROM parking_slots WHERE slot_id='PX' AND available=TRUE",List.of(),1);assertDbCount("SELECT COUNT(*) FROM parking_tickets WHERE ticket_id=?",invalid.id(),0);}
 private void concurrentSlotClaim()throws Exception{CountDownLatch ready=new CountDownLatch(2),go=new CountDownLatch(1);ExecutorService pool=Executors.newFixedThreadPool(2);try{List<Future<Boolean>> claims=new ArrayList<>();for(int i=0;i<2;i++)claims.add(pool.submit(()->{try(Connection c=DBConnection.open()){c.setAutoCommit(false);ready.countDown();go.await();try{slots.setAvailable(c,"PX",false);c.commit();return true;}catch(SQLException lost){c.rollback();return false;}}}));assertTrue(ready.await(5,TimeUnit.SECONDS));go.countDown();int winners=0;for(Future<Boolean> f:claims)if(f.get(10,TimeUnit.SECONDS))winners++;assertEquals(1,winners,"exactly one transaction may claim the same slot");}finally{pool.shutdownNow();}slots.release("PX");assertDbCount("SELECT COUNT(*) FROM parking_slots WHERE slot_id='PX' AND available=TRUE",List.of(),1);}
 private void cleanup(Map<String,Boolean> sampleAvailability)throws SQLException {
  try(Connection c=DBConnection.open()) {
   c.setAutoCommit(false);
   try {
    try(PreparedStatement p=c.prepareStatement("DELETE FROM waiting_queue WHERE registration_number=?")) {
     for(String r:REGISTRATIONS){p.setString(1,r);p.addBatch();}p.executeBatch();
    }
    try(PreparedStatement p=c.prepareStatement("DELETE FROM payments WHERE ticket_id IN (SELECT ticket_id FROM parking_tickets WHERE registration_number IN (?,?,?,?))")) {
     for(int i=0;i<REGISTRATIONS.size();i++)p.setString(i+1,REGISTRATIONS.get(i));p.executeUpdate();
    }
    try(PreparedStatement p=c.prepareStatement("DELETE FROM parking_history WHERE registration_number IN (?,?,?,?)")) {
     for(int i=0;i<REGISTRATIONS.size();i++)p.setString(i+1,REGISTRATIONS.get(i));p.executeUpdate();
    }
    for(String table:List.of("parking_tickets","vehicles")) {
     try(PreparedStatement p=c.prepareStatement("DELETE FROM "+table+" WHERE registration_number IN (?,?,?,?)")) {
      for(int i=0;i<REGISTRATIONS.size();i++)p.setString(i+1,REGISTRATIONS.get(i));p.executeUpdate();
     }
    }
    try(PreparedStatement p=c.prepareStatement("DELETE FROM parking_slots WHERE slot_id IN ('P1','P2','P3','PX')")){p.executeUpdate();}
    try(PreparedStatement p=c.prepareStatement("UPDATE parking_slots SET available=? WHERE slot_id=?")) {
     for(var e:sampleAvailability.entrySet()){p.setBoolean(1,e.getValue());p.setString(2,e.getKey());p.addBatch();}p.executeBatch();
    }
    c.commit();
   } catch(SQLException e) {c.rollback();throw e;}
  }
 }
 private void assertDbCount(String sql,String value,long expected)throws SQLException{assertDbCount(sql,List.of(value),expected);}
 private void assertDbCount(String sql,List<String> values,long expected)throws SQLException{try(Connection c=DBConnection.open();PreparedStatement p=c.prepareStatement(sql)){for(int i=0;i<values.size();i++)p.setString(i+1,values.get(i));try(ResultSet r=p.executeQuery()){assertTrue(r.next());assertEquals(expected,r.getLong(1),sql);}}}
 private static void addSlot(PreparedStatement p,String id,int size,int distance)throws SQLException{p.setString(1,id);p.setInt(2,size);p.setInt(3,distance);p.executeUpdate();}
}
