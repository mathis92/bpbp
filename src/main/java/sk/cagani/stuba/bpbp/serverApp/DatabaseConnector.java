/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sk.cagani.stuba.bpbp.serverApp;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.criterion.Restrictions;
import org.joda.time.DateTime;
import org.joda.time.JodaTimePermission;

import org.slf4j.LoggerFactory;
import stuba.bpbphibernatemapper.GtfsRoutes;
import stuba.bpbphibernatemapper.GtfsStopTimes;
import stuba.bpbphibernatemapper.GtfsStops;
import stuba.bpbphibernatemapper.GtfsTrips;

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
    }

    public static Session getSession() {
        Session session = sessionFactory.openSession();
        session.beginTransaction(); //open the transaction
        return session;
    }

    public void testConnection() throws Exception {
        System.out.println("IDEM TESTUVAC");
        Session session = getSession();
        List<GtfsTrips> tripList = session.createCriteria(GtfsTrips.class).add(Restrictions.eq("serviceIdId", "Prac.dny_0")).list();

        Date date1 = new Date();
        for (GtfsTrips trip : tripList) {
            // List<GtfsStopTimes> stopTimesList = (List<GtfsStopTimes>) session.createCriteria(GtfsStopTimes.class).add(Restrictions.eq("gtfsTrips", trip)).add(Restrictions.between("arrivalTime", 25000, 26200)).list();
            Set<GtfsStopTimes> stopTimeesList = trip.getGtfsStopTimeses();

            for (GtfsStopTimes stopTime : stopTimeesList) {
                if (stopTime.getArrivalTime() > 25000 && stopTime.getArrivalTime() < 26200) {
                    if (stopTime.getGtfsStops().getName().equalsIgnoreCase("Zochova")) {
                        System.out.println("VOZIDLO ROZJEBANE cislo: " + trip.getGtfsRoutes().getShortName() + " sa prave dojebalo na zastafku -> " + stopTime.getGtfsStops().getName() + " KURVA KONECNE DOSLO O prichod " + secsToHMS(stopTime.getArrivalTime()));
                    }
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
