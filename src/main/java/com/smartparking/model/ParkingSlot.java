package com.smartparking.model;
public class ParkingSlot { private final String id; private final int floor,size,distance; private boolean available=true;
 public ParkingSlot(String id,int floor,int size,int distance){this.id=id;this.floor=floor;this.size=size;this.distance=distance;}
 public String getId(){return id;} public int getFloor(){return floor;} public int getSize(){return size;} public int getDistance(){return distance;} public synchronized boolean isAvailable(){return available;} public synchronized void occupy(){if(!available)throw new IllegalStateException("Slot already occupied");available=false;} public synchronized void release(){if(available)throw new IllegalStateException("Slot is already available");available=true;} public boolean fits(Vehicle v){return size>=v.requiredSize();}
}
