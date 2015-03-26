/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sk.mathis.stuba.bpbp;

import java.io.File;
import java.util.Date;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.criterion.Restrictions;

import org.onebusaway.gtfs.impl.GtfsDaoImpl;
import org.onebusaway.gtfs.impl.StopTimeArray;
import org.onebusaway.gtfs.model.Agency;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.serialization.GtfsReader;
import org.slf4j.LoggerFactory;
import stuba.bpbphibernatemapper.GtfsRoutes;
import stuba.bpbphibernatemapper.GtfsStopTimes;
import stuba.bpbphibernatemapper.GtfsTrips;
import stuba.bpbphibernatemapper.Poi;

/**
 *
 * @author martinhudec
 */
public class DatabaseConnector {

    private final org.slf4j.Logger logger = LoggerFactory.getLogger(DatabaseConnector.class);
    private static Session session;
    private final SessionFactory sessionFactory;
    public DatabaseConnector() {
        Configuration configuration = new Configuration();
        configuration.configure("hibernate.cfg.xml");
        configuration.addJar(new File("/home/debian/BPbp/target/lib/BpbpHibernateMapper-1.0.jar"));
        StandardServiceRegistryBuilder ssrb = new StandardServiceRegistryBuilder().applySettings(configuration.getProperties());
        sessionFactory = configuration.buildSessionFactory(ssrb.build());
    }
    
    public Session getSession(){
        return sessionFactory.openSession();
    }
    
    
    
    public void testConnection() throws Exception {

        System.out.println("Trying to create a test connection with the database.");
        session = sessionFactory.openSession();
        logger.debug("Test connection with the database created successfuly.");
        Date date = new Date();
        for (GtfsRoutes routes : (List<GtfsRoutes>) session.createCriteria(GtfsRoutes.class).list()) {
            logger.debug(routes.getShortName());
        }
        logger.debug("MADARSKY CAS CIGANSKY DEVET " + (new Date().getTime() - date.getTime()));

        
        
        Date startTime = new Date();
        session.beginTransaction();

        List<Stop> stopList = session.createCriteria(Stop.class).list();
        //MdsDiagnostician testDiagnostician = ((MdsTesting) session.createCriteria(MdsTesting.class).add(Restrictions.eq("mdsDevice", device)).list().get(0)).getMdsDiagnostician();

        GtfsRoutes tricatdevina = (GtfsRoutes) session.createCriteria(GtfsRoutes.class).add(Restrictions.eq("shortName", "39")).list().get(0);

        List<GtfsTrips> trips = (List<GtfsTrips>) session.createCriteria(GtfsTrips.class).add(Restrictions.eq("gtfsRoutes", tricatdevina)).list();
        for (GtfsTrips trip : trips) {
            System.out.println("trip " + trip.getTripHeadsign());
            List<GtfsStopTimes> stopTimeList = session.createCriteria(GtfsStopTimes.class).add(Restrictions.eq("gtfsTrips", trip)).list();
            System.out.println(stopTimeList.size() + " velkost stoptime array list");
            for (GtfsStopTimes stopTime : stopTimeList) {
                System.out.println(stopTime.getGtfsStops().getName() + " " + stopTime.getGtfsStops().getLat() + " " + stopTime.getGtfsStops().getLon() + " " + funkcia(stopTime.getArrivalTime()));
            }
            break;
        }
        System.out.println("Cas zrobenia nie text : " + (new Date().getTime() - startTime.getTime()));

    }

    public String funkcia(int totalSecs) {
        int hours = totalSecs / 3600;
        int minutes = (totalSecs % 3600) / 60;
        int seconds = totalSecs % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}
