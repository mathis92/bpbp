/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sk.cagani.stuba.bpbp.device;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import sk.cagani.stuba.bpbp.serverApp.DatabaseConnector;
import stuba.bpbphibernatemapper.GtfsCalendarDates;

/**
 *
 * @author martinhudec
 */
public class CurrentDay implements Runnable{
public static String day = null;
    @Override
    public void run() {
        while(true){
            DateTime currentDate = new org.joda.time.DateTime();
        Session session = DatabaseConnector.getSession();
        Transaction tx = session.beginTransaction();
        List<GtfsCalendarDates> calendarDatesList = session.createCriteria(GtfsCalendarDates.class).addOrder(Order.asc("date")).list();
        String foundServiceId = null;
        for (GtfsCalendarDates date : calendarDatesList) {
         //   System.out.println(currentDate.getYear() + "" + ((currentDate.getMonthOfYear() < 10) ? "0" + currentDate.getMonthOfYear() : currentDate.getMonthOfYear()) + "" + ((currentDate.getDayOfMonth() < 10) ? "0" + currentDate.getDayOfMonth() : currentDate.getDayOfMonth()) + " " + date.getDate());

            if (date.getDate().equals(currentDate.getYear() + "" + ((currentDate.getMonthOfYear() < 10) ? "0" + currentDate.getMonthOfYear() : currentDate.getMonthOfYear()) + "" + ((currentDate.getDayOfMonth() < 10) ? "0" + currentDate.getDayOfMonth() : currentDate.getDayOfMonth()) + " " + date.getDate())) {
                foundServiceId = date.getServiceIdId();
                break;
            }
        }
        tx.commit();
        session.close();
        if (foundServiceId == null) {
            if (currentDate.getDayOfWeek() == DateTimeConstants.MONDAY
                    || currentDate.getDayOfWeek() == DateTimeConstants.TUESDAY
                    || currentDate.getDayOfWeek() == DateTimeConstants.WEDNESDAY
                    || currentDate.getDayOfWeek() == DateTimeConstants.THURSDAY
                    || currentDate.getDayOfWeek() == DateTimeConstants.FRIDAY) {
                foundServiceId = "Prac.dny_0";
            } else if (currentDate.getDayOfWeek() == DateTimeConstants.SATURDAY) {
                foundServiceId = "Soboty_1";
            } else {
                foundServiceId = "Neděle+Sv_2";
            }
            day = foundServiceId;
        }
     //   System.out.println(foundServiceId);
            try {
                Thread.sleep(60000);
            } catch (InterruptedException ex) {
                Logger.getLogger(CurrentDay.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    
    }

    public static String getDay() {
        return day;
    }
    
    
    
}
