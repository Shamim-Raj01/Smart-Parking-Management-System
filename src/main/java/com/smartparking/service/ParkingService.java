package com.smartparking.service;

import com.smartparking.dao.*;
import com.smartparking.database.DBConnection;
import com.smartparking.interfaces.AllocationStrategy;
import com.smartparking.interfaces.PaymentStrategy;
import com.smartparking.model.*;
import com.smartparking.strategy.CashPayment;
import com.smartparking.strategy.UpiPayment;
import com.smartparking.strategy.CardPayment;
import com.smartparking.strategy.NearestSlotStrategy;
import java.math.*;
import java.sql.*;
import java.time.*;
import java.util.*;

public class ParkingService {
 private final Map<String,Vehicle> vehicles=new HashMap<>();
 private final Map<String,ParkingTicket> active=new HashMap<>();
 private final List<ParkingTicket> history=new ArrayList<>();
 private final List<ParkingSlot> slots=new ArrayList<>();
 private final Queue<String> waiting=new ArrayDeque<>();
 private final AllocationStrategy allocator;
 private final boolean persistent;
 private final VehicleDAO vehicleDAO=new VehicleDAO();
 private final ParkingSlotDAO slotDAO=new ParkingSlotDAO();
 private final ParkingTicketDAO ticketDAO=new ParkingTicketDAO();
 private final PaymentDAO paymentDAO=new PaymentDAO();
 private final ParkingHistoryDAO historyDAO=new ParkingHistoryDAO();
 private final WaitingQueueDAO waitingDAO=new WaitingQueueDAO();

 public ParkingService(){this(new NearestSlotStrategy(),defaults());}
 public ParkingService(AllocationStrategy allocator,List<ParkingSlot> slots){this.allocator=allocator;this.slots.addAll(slots);persistent=false;}
 private ParkingService(List<Vehicle> vehicles,List<ParkingSlot> slots,List<ParkingTicket> active,List<ParkingTicket> history,List<String> waiting){allocator=new NearestSlotStrategy();persistent=true;for(Vehicle v:vehicles)this.vehicles.put(v.getRegistrationNumber(),v);this.slots.addAll(slots);for(ParkingTicket t:active){Vehicle vehicle=this.vehicles.get(t.vehicle().getRegistrationNumber());ParkingSlot slot=this.slots.stream().filter(s->s.getId().equals(t.slot().getId())).findFirst().orElse(t.slot());this.active.put(t.vehicle().getRegistrationNumber(),new ParkingTicket(t.id(),vehicle==null?t.vehicle():vehicle,slot,t.entryTime(),t.exitTime(),t.fee(),t.paymentMethod(),t.paymentStatus()));}this.history.addAll(history);this.waiting.addAll(waiting);}
 public static ParkingService persistent() throws SQLException {
  VehicleDAO vd=new VehicleDAO();ParkingSlotDAO sd=new ParkingSlotDAO();ParkingTicketDAO td=new ParkingTicketDAO();ParkingHistoryDAO hd=new ParkingHistoryDAO();WaitingQueueDAO wq=new WaitingQueueDAO();
  List<Vehicle> vehicles=vd.all();List<ParkingTicket> active=new ArrayList<>();for(Vehicle v:vehicles)td.active(v.getRegistrationNumber()).ifPresent(active::add);
  ParkingService service=new ParkingService(vehicles,sd.all(),active,hd.all(),wq.all());service.promoteWaiting();return service;
 }
 private static List<ParkingSlot> defaults(){List<ParkingSlot> out=new ArrayList<>();for(int i=1;i<=12;i++)out.add(new ParkingSlot("A"+i,1,i%4==0?3:(i%3==0?2:1),i*5));return out;}

 public synchronized void refreshFromDatabase(){if(!persistent)return;try{ParkingService fresh=persistent();vehicles.clear();vehicles.putAll(fresh.vehicles);slots.clear();slots.addAll(fresh.slots);active.clear();active.putAll(fresh.active);history.clear();history.addAll(fresh.history);waiting.clear();waiting.addAll(fresh.waiting);}catch(SQLException e){throw new IllegalStateException("Could not refresh dashboard from MySQL: "+e.getMessage(),e);}}
 public synchronized Vehicle register(Vehicle v){if(vehicles.containsKey(v.getRegistrationNumber()))throw new IllegalArgumentException("Vehicle already registered");if(persistent)try{vehicleDAO.save(v);}catch(SQLException e){throw new IllegalStateException("Could not save vehicle: "+e.getMessage(),e);}vehicles.put(v.getRegistrationNumber(),v);return v;}
 public synchronized Optional<Vehicle> findVehicle(String r){return Optional.ofNullable(vehicles.get(normalize(r)));}
 public synchronized List<Vehicle> vehicles(){return List.copyOf(vehicles.values());}

 public synchronized ParkingTicket enter(String registration){String key=normalize(registration);Vehicle v=vehicles.get(key);if(v==null)throw new IllegalArgumentException("Vehicle is not registered");if(active.containsKey(key))throw new IllegalStateException("Vehicle already parked");if(waiting.contains(key)){if(!Objects.equals(waiting.peek(),key))return null;}else if(!waiting.isEmpty()){enqueue(key);promoteWaiting();return active.get(key);}Optional<ParkingSlot> available=allocator.allocateSlot(v,slots);if(available.isEmpty()){enqueue(key);return null;}return allocate(v,available.get(),waiting.contains(key));}
 private void enqueue(String key){if(waiting.contains(key))return;if(persistent)try{waitingDAO.add(key);}catch(SQLException e){throw new IllegalStateException("Could not save waiting vehicle: "+e.getMessage(),e);}waiting.offer(key);}
 private ParkingTicket allocate(Vehicle vehicle,ParkingSlot slot,boolean queued){String key=vehicle.getRegistrationNumber();ParkingTicket ticket=new ParkingTicket(UUID.randomUUID().toString(),vehicle,slot,LocalDateTime.now(),null,null,null,null);if(persistent)try(Connection c=DBConnection.open()){c.setAutoCommit(false);try{slotDAO.setAvailable(c,slot.getId(),false);ticketDAO.insert(c,ticket);if(queued)waitingDAO.remove(c,key);c.commit();}catch(SQLException e){try{c.rollback();}catch(SQLException rollback){e.addSuppressed(rollback);}throw e;}}catch(SQLException e){throw new IllegalStateException("Could not persist vehicle entry: "+e.getMessage(),e);}slot.occupy();active.put(key,ticket);if(queued)waiting.remove(key);return ticket;}

 public synchronized ParkingTicket exit(String registration,String method){String key=normalize(registration);String payMethod=method==null?"":method.toUpperCase(Locale.ROOT);PaymentStrategy payment=switch(payMethod){case "CASH"->new CashPayment();case "UPI"->new UpiPayment();case "CARD"->new CardPayment();default->throw new IllegalArgumentException("Choose Cash, UPI, or Card");};ParkingTicket ticket=active.get(key);if(ticket==null&&persistent)try{ticket=ticketDAO.active(key).orElse(null);}catch(SQLException e){throw new IllegalStateException("Could not load active ticket: "+e.getMessage(),e);}if(ticket==null)throw new IllegalStateException("No active ticket");LocalDateTime now=LocalDateTime.now();long seconds=Duration.between(ticket.entryTime(),now).toSeconds();long hours=Math.max(1,(seconds+3599)/3600);BigDecimal amount=ticket.vehicle().hourlyRate().multiply(BigDecimal.valueOf(hours)).setScale(2,RoundingMode.HALF_UP);if(!payment.processPayment(amount))throw new IllegalStateException("Payment was declined");
  if(persistent)try(Connection c=DBConnection.open()){c.setAutoCommit(false);try{ticketDAO.close(c,ticket,now,amount);paymentDAO.save(c,ticket.id(),payMethod,amount);historyDAO.save(c,ticket.id(),key,ticket.slot().getId(),ticket.entryTime(),now,amount);slotDAO.setAvailable(c,ticket.slot().getId(),true);c.commit();}catch(SQLException e){try{c.rollback();}catch(SQLException rollback){e.addSuppressed(rollback);}throw e;}}catch(SQLException e){throw new IllegalStateException("Could not complete exit transaction: "+e.getMessage(),e);}
  ParkingTicket done=new ParkingTicket(ticket.id(),ticket.vehicle(),ticket.slot(),ticket.entryTime(),now,amount,payMethod,"PAID");active.remove(key);ticket.slot().release();history.add(done);promoteWaiting();return done;
 }
 private void promoteWaiting(){while(!waiting.isEmpty()){String candidate=null;Vehicle vehicle=null;ParkingSlot slot=null;for(String key:waiting){Vehicle v=vehicles.get(key);if(v==null){candidate=key;break;}Optional<ParkingSlot> fit=allocator.allocateSlot(v,slots);if(fit.isPresent()){candidate=key;vehicle=v;slot=fit.get();break;}}if(candidate==null)return;if(vehicle==null){removeWaiting(candidate);continue;}try{allocate(vehicle,slot,true);}catch(RuntimeException failure){return;}}}
 private void removeWaiting(String key){if(persistent)try{waitingDAO.remove(key);}catch(SQLException e){throw new IllegalStateException("Could not remove waiting vehicle: "+e.getMessage(),e);}waiting.remove(key);}
 private static String normalize(String r){if(r==null||r.isBlank())throw new IllegalArgumentException("Registration number is required");return r.trim().toUpperCase(Locale.ROOT);}
 public synchronized List<ParkingSlot> slots(){return List.copyOf(slots);} public synchronized List<ParkingTicket> activeTickets(){return List.copyOf(active.values());} public synchronized List<ParkingTicket> history(){return List.copyOf(history);} public synchronized List<Payment> payments(){if(persistent)try{return paymentDAO.all();}catch(SQLException e){throw new IllegalStateException("Could not load payment records: "+e.getMessage(),e);}List<Payment> out=new ArrayList<>();long id=1;for(ParkingTicket t:history)out.add(new Payment(id++,t.id(),t.vehicle().getRegistrationNumber(),t.slot().getId(),t.exitTime(),t.paymentMethod(),t.fee(),t.paymentStatus()));return List.copyOf(out);} public synchronized List<String> waitingVehicles(){return List.copyOf(waiting);} public synchronized int waitingCount(){return waiting.size();} public synchronized int availableCount(){return (int)slots.stream().filter(ParkingSlot::isAvailable).count();} public synchronized BigDecimal revenue(){return payments().stream().map(Payment::amount).filter(Objects::nonNull).reduce(BigDecimal.ZERO,BigDecimal::add);}
 public boolean isPersistent(){return persistent;}
}
