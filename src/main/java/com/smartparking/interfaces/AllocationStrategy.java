package com.smartparking.interfaces;
import com.smartparking.model.*;import java.util.*;
public interface AllocationStrategy { Optional<ParkingSlot> allocateSlot(Vehicle vehicle,Collection<ParkingSlot> slots); }
