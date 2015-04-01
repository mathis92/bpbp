/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sk.cagani.stuba.bpbp.serverApp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import org.slf4j.LoggerFactory;
import stuba.bpbphibernatemapper.GtfsStopTimes;
import stuba.bpbphibernatemapper.GtfsStops;
import stuba.bpbphibernatemapper.GtfsStopsId;

/**
 *
 * @author martinhudec
 */
public class DatabaseConnector {

    private final org.slf4j.Logger logger = LoggerFactory.getLogger(DatabaseConnector.class);
    private static SessionFactory sessionFactory;

    public DatabaseConnector() {
        Configuration configuration = new Configuration();
        configuration.configure("hibernate.cfg.xml");
        configuration.addJar(new File("/home/debian/BPbp/target/lib/BpbpHibernateMapper-1.0.jar"));
        StandardServiceRegistryBuilder ssrb = new StandardServiceRegistryBuilder().applySettings(configuration.getProperties());
        sessionFactory = configuration.buildSessionFactory(ssrb.build());
        
        zrob();
        System.out.println("HOTOVO DO PICE");
    }

    public static Session getSession() {
        Session session = sessionFactory.openSession();
        session.beginTransaction(); //open the transaction
        return session;
    }

    private void zrob() {
        System.out.println("robim");
        String csvFile = "/home/debian/stopscustom.txt";
        BufferedReader br = null;
        String line = "";
        String cvsSplitBy = ",";

        Session session = getSession();
        try {
            FileInputStream fis = new FileInputStream(csvFile);
            System.out.println("otwaram");
            br = new BufferedReader(new InputStreamReader(fis, "UTF-8"));
            System.out.println("scitam");
            while ((line = br.readLine()) != null) {
                // use comma as separator
                String[] stop = line.split(cvsSplitBy);
                System.out.println(line + " " + stop.length);
                
                System.out.println(stop[0] + " - " + stop[1]);
                GtfsStops stp = (GtfsStops) session.get(GtfsStops.class, new GtfsStopsId("01", stop[0]));
                stp.setName(stop[1]);
                session.update(stp);
            }
            session.getTransaction().commit();
            session.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void testConnection() throws Exception {
        System.out.println("IDEM TESTUVAC");
        Session session = getSession();
        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);

        System.out.println(c.getTimeInMillis());
        Long timeSinceMidnight = new Date().getTime() - (c.getTimeInMillis());
        Long secondsSinceMidnight = timeSinceMidnight / 1000;
        System.out.println(secondsSinceMidnight.intValue() + " since midnight ");
        Date date1 = new Date();
        /*
         List<GtfsTrips> tripList = session.createCriteria(GtfsTrips.class).add(Restrictions.eq("serviceIdId", "Prac.dny_0")).list();
         System.out.println("pocet tripov pre prac dny " + tripList.size());

         for (GtfsTrips trip : tripList) {
         List<GtfsStopTimes> stopTimesList = (List<GtfsStopTimes>) session.createCriteria(GtfsStopTimes.class).add(Restrictions.eq("gtfsTrips", trip)).add(Restrictions.between("arrivalTime", secondsSinceMidnight, secondsSinceMidnight + 1200)).list();
         for (GtfsStopTimes stopTime : stopTimesList) {
         //for (GtfsStops stop : (List<GtfsStops>) session.createCriteria(GtfsStops.class).add(Restrictions.eq("name", "Zochova")).list()) {
         if (stopTime.getGtfsStops().getName().equalsIgnoreCase("Zochova")) {
         System.out.println("VOZIDLO ROZJEBANE cislo: " + trip.getGtfsRoutes().getShortName() + " sa prave dojebalo na zastafku -> " + stopTime.getGtfsStops().getName() + " KURVA KONECNE DOSLO O prichod " + secsToHMS(stopTime.getArrivalTime()));
         }
         }
         }
         */
        List<GtfsStops> stopList = session.createCriteria(GtfsStops.class).add(Restrictions.eq("name", "Zochova")).list();
        for (GtfsStops stop : stopList) {
            List<GtfsStopTimes> stopTimesList = session.createCriteria(GtfsStopTimes.class).add(Restrictions.eq("gtfsStops", stop)).add(Restrictions.between("arrivalTime", secondsSinceMidnight.intValue(), secondsSinceMidnight.intValue() + 1200)).addOrder(Order.asc("arrivalTime")).list();
            for (GtfsStopTimes stopTimes : stopTimesList) {
                if (stopTimes.getGtfsTrips().getServiceIdId().equals("Prac.dny_0")) {
                    System.out.println(stopTimes.getGtfsTrips().getGtfsRoutes().getShortName() + " " + stop.getName() + " " + stopTimes.getGtfsTrips().getTripHeadsign() + " " + secsToHMS(stopTimes.getArrivalTime()));
                }
            }
        }
        System.out.println((new Date().getTime() - date1.getTime()));

    }

    public String secsToHMS(int totalSecs) {
        int hours = totalSecs / 3600;
        int minutes = (totalSecs % 3600) / 60;
        int seconds = totalSecs % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}
