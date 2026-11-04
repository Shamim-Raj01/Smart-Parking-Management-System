package com.smartparking;
import com.smartparking.service.ParkingService;import com.smartparking.gui.DashboardFrame;import javax.swing.*;
public class Main { public static void main(String[] args){SwingUtilities.invokeLater(()->{try{new DashboardFrame(ParkingService.persistent()).setVisible(true);}catch(Exception e){JOptionPane.showMessageDialog(null,"Database connection failed: "+e.getMessage()+"\nSet application.properties and run schema.sql first.","Smart Parking",JOptionPane.ERROR_MESSAGE);}});} }
