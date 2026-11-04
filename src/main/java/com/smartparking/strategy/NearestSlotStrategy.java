package com.smartparking.strategy;
import com.smartparking.interfaces.AllocationStrategy;import com.smartparking.model.*;import java.util.*;
public class NearestSlotStrategy implements AllocationStrategy { public Optional<ParkingSlot> allocateSlot(Vehicle v,Collection<ParkingSlot> slots){PriorityQueue<ParkingSlot> q=new PriorityQueue<>(Comparator.comparingInt(ParkingSlot::getDistance).thenComparingInt(ParkingSlot::getFloor).thenComparing(ParkingSlot::getId));for(ParkingSlot s:slots)if(s.isAvailable()&&s.fits(v))q.offer(s);return Optional.ofNullable(q.poll());} }
