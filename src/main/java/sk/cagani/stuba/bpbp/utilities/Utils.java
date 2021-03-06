package sk.cagani.stuba.bpbp.utilities;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.Duration;
import sk.cagani.stuba.bpbp.serverApp.DatabaseConnector;
import stuba.bpbpdatabasemapper.GtfsCalendarDates;

/**
 *
 * @author Martin Banas
 */
public class Utils implements Runnable {

    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
    private static String serviceId;
    private static String serviceIdTomorrow;
    private static List<GtfsCalendarDates> calendarDatesList = null;

    @Override
    public void run() {
        while (true) {
            serviceId = determineCurrentServiceId(new DateTime());
            try {
                Thread.sleep(60000);
            } catch (InterruptedException ex) {
                Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }

    public static String getActualServiceId() {
        return serviceId;
    }

    public static String getTomorrowServiceId() {
        DateTime dt = new DateTime();
        DateTime tomorrow = dt.plusDays(1).withTimeAtStartOfDay();
        return determineCurrentServiceId(tomorrow);
    }

    public static Integer getSecondsFromMidnight() {
        DateTime now = new DateTime();
        DateTime midnight = now.withTimeAtStartOfDay();
        Duration duration = new Duration(midnight, now);
        int secs = duration.toStandardSeconds().getSeconds();
        return secs < 12600 ? secs + 86400 : secs;
    }

    public static Integer getSecondsFromMidnight(Date date) {
        DateTime now = new DateTime(date);
        DateTime midnight = now.withTimeAtStartOfDay();
        Duration duration = new Duration(midnight, now);
        int secs = duration.toStandardSeconds().getSeconds();
        return secs < 12600 ? secs + 86400 : secs;
    }

    public static String secsToHMS(int totalSecs) {
        int hours = totalSecs / 3600;
        if (hours > 23) {
            hours = hours - 24;
        }
        int minutes = (totalSecs % 3600) / 60;
        int seconds = totalSecs % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    public static String determineCurrentServiceId(DateTime dateTime) {
        DateTime currentDate = dateTime;
        String currentServiceId = null;
        Session session = DatabaseConnector.getSession();
        Transaction tx = session.beginTransaction();
        if (calendarDatesList == null) {
            calendarDatesList = session.createCriteria(GtfsCalendarDates.class).addOrder(Order.asc("date")).list();
        }
        for (GtfsCalendarDates date : calendarDatesList) {
            //System.out.println("current Date " + date.getDate() + " SDF date " + sdf.format(new Date()));
            if (date.getDate().equals(sdf.format(currentDate.toDate()))) {
                currentServiceId = date.getServiceIdId();
                break;
            }

        }
        tx.commit();
        session.close();
        /*
         Toto je trosku napicu nie? sviatok moze byt aj v pondelok utorok sobotu hockedy.. a v tedy to vsetko 
         spada pod NEDELE + SV           
         */
        if (currentServiceId == null) {
            if (currentDate.getDayOfWeek() == DateTimeConstants.MONDAY
                    || currentDate.getDayOfWeek() == DateTimeConstants.TUESDAY
                    || currentDate.getDayOfWeek() == DateTimeConstants.WEDNESDAY
                    || currentDate.getDayOfWeek() == DateTimeConstants.THURSDAY
                    || currentDate.getDayOfWeek() == DateTimeConstants.FRIDAY) {
                currentServiceId = "Prac.dny_0";
            } else if (currentDate.getDayOfWeek() == DateTimeConstants.SATURDAY) {
                currentServiceId = "Soboty_1";
            } else {
                currentServiceId = "Neděle+Sv_2";
            }
        }
        return currentServiceId;
    }

}
