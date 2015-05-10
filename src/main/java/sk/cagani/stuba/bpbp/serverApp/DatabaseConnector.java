/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sk.cagani.stuba.bpbp.serverApp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonWriter;
import javax.json.stream.JsonGenerator;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.stat.Statistics;
import org.hibernate.transform.Transformers;

import org.slf4j.LoggerFactory;
import sk.cagani.stuba.bpbp.utilities.Utils;
import stuba.bpbpdatabasemapper.GtfsRoutes;
import stuba.bpbpdatabasemapper.GtfsStopTimes;
import stuba.bpbpdatabasemapper.GtfsStops;
import stuba.bpbpdatabasemapper.GtfsTrips;

/**
 *
 * @author martinhudec
 */
public class DatabaseConnector {

    private final org.slf4j.Logger logger = LoggerFactory.getLogger(DatabaseConnector.class);
    private static SessionFactory sessionFactory;
    private static Statistics stats;

    public DatabaseConnector() {
        Configuration configuration = new Configuration();
        configuration.configure("hibernate.cfg.xml");
        configuration.addJar(new File("/home/debian/BPbp/target/lib/BPbpDatabaseMapper-1.0.jar"));
        StandardServiceRegistryBuilder ssrb = new StandardServiceRegistryBuilder().applySettings(configuration.getProperties());
        sessionFactory = configuration.buildSessionFactory(ssrb.build());
        stats = sessionFactory.getStatistics();
        stats.setStatisticsEnabled(true);
    }

    public static Statistics getStatistics() {
        return stats;
    }

    public static Session getSession() {
        return sessionFactory.openSession();
    }

    public void writeDetailedStopsToFile() {
        FileOutputStream fos = null;
        try {
            Map<String, Object> jwConfig = new HashMap<>();
            jwConfig.put(JsonGenerator.PRETTY_PRINTING, true);
            fos = new FileOutputStream("/home/debian/allDetailedStops.txt");
            JsonWriter jw = Json.createWriterFactory(jwConfig).createWriter(fos, Charset.forName("UTF-8"));
            Session session = getSession();
            Transaction tx = session.beginTransaction();

                        JsonArrayBuilder stopsJAB = Json.createArrayBuilder();
            List<StopData> stopDataList = new ArrayList<>();

            List<GtfsStops> stopsList = session.createCriteria(GtfsStops.class).addOrder(Order.asc("name")).list();
            for (GtfsStops stop : stopsList) {
                StopData data = null;
                data = new StopData();
                data.stop = stop;
                List<GtfsRoutes> routeList = session.createCriteria(GtfsStopTimes.class, "stopTime")
                        .createAlias("stopTime.gtfsTrips", "trip")
                        .createAlias("trip.gtfsRoutes", "route")
                        .add(Restrictions.eq("gtfsStops", stop))
                        .addOrder(Order.asc("route.shortName"))
                        .setProjection(Projections.distinct(Projections.projectionList()
                                .add(Projections.property("route.shortName"), "shortName")))
                        .setResultTransformer(Transformers.aliasToBean(GtfsRoutes.class))
                        .list();

                data.setRouteList(routeList);
                stopDataList.add(data);
            }

            for (StopData sD : stopDataList) {
                //System.out.println(sD.getStop().getName());
                StringBuilder vehicles = null;
                Collections.sort(sD.getRouteList(), new CustomComparator());
                for (GtfsRoutes route : sD.getRouteList()) {
                    if (vehicles == null) {
                        vehicles = new StringBuilder();
                        vehicles.append(route.getShortName());
                    } else {
                        vehicles.append(", " + route.getShortName());
                    }
                }
                if (vehicles == null) {
                    vehicles = new StringBuilder();
                    vehicles.append("neobsluhovaná");
                }
                JsonObjectBuilder stopJOB = Json.createObjectBuilder();
                stopJOB.add(sD.getStop().getId().getClass().getSimpleName(), sD.getStop().getId().getId());
                stopJOB.add("name", sD.getStop().getName());
                stopJOB.add("lat", sD.getStop().getLat());
                stopJOB.add("lon", sD.getStop().getLon());
                stopJOB.add("vehicles", vehicles.toString());

                stopsJAB.add(stopJOB);

            }

            JsonObjectBuilder stopsJOB = Json.createObjectBuilder();
            stopsJOB.add("stops", stopsJAB);
            JsonObject stopsJO = stopsJOB.build();
            System.out.println(stopsJO.toString());
            jw.writeObject(stopsJO);
            tx.commit(); //closes transaction
            session.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(DatabaseConnector.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                fos.close();
            } catch (IOException ex) {
                Logger.getLogger(DatabaseConnector.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }

    public void writeStopsToFile() {
        FileOutputStream fos = null;
        try {
            Map<String, Object> jwConfig = new HashMap<>();
            jwConfig.put(JsonGenerator.PRETTY_PRINTING, true);
            fos = new FileOutputStream("/home/debian/allStops.txt");
            JsonWriter jw = Json.createWriterFactory(jwConfig).createWriter(fos, Charset.forName("UTF-8"));
            Session session = getSession();
            Transaction tx = session.beginTransaction();
            List<GtfsStops> stopsList = session.createCriteria(GtfsStops.class).addOrder(Order.asc("name")).list();

            JsonArrayBuilder stopsJAB = Json.createArrayBuilder();
            List<StopData> stopDataList = new ArrayList<>();

            for (GtfsStops stop : stopsList) {
                boolean found = false;
                StopData data = null;
                for (StopData stopData : stopDataList) {
                    if (stopData.getStop().getName().equals(stop.getName())) {
                        data = stopData;
                        if (stop.getId().getId().endsWith("1")) {
                            data.stop = stop;
                        }
                        found = true;
                        break;
                    }
                }
                if (data == null) {
                    data = new StopData();
                    data.stop = stop;
                }

                List<GtfsRoutes> routeList = session.createCriteria(GtfsStopTimes.class, "stopTime")
                        .createAlias("stopTime.gtfsTrips", "trip")
                        .createAlias("trip.gtfsRoutes", "route")
                        .add(Restrictions.eq("gtfsStops", stop))
                        .addOrder(Order.asc("route.shortName"))
                        .setProjection(Projections.distinct(Projections.projectionList().add(Projections.property("route.shortName"), "shortName")))
                        .setResultTransformer(Transformers.aliasToBean(GtfsRoutes.class)).list();

                if (data.getRouteList().isEmpty()) {
                    data.setRouteList(routeList);
                } else {
                    for (GtfsRoutes route : routeList) {
                        boolean foundRoute = false;
                        for (GtfsRoutes route2 : data.getRouteList()) {
                            if (route2.getShortName().equals(route.getShortName())) {
                                foundRoute = true;
                            }
                        }
                        if (foundRoute == false) {
                            data.getRouteList().add(route);
                        }
                    }
                }
                if (found == false) {
                    stopDataList.add(data);
                }
            }

            for (StopData sD : stopDataList) {
                //System.out.println(sD.getStop().getName());
                StringBuilder vehicles = null;
                Collections.sort(sD.getRouteList(), new CustomComparator());
                for (GtfsRoutes route : sD.getRouteList()) {
                    //  logger.debug(route.getShortName());
                    if (vehicles == null) {
                        vehicles = new StringBuilder();
                        vehicles.append(route.getShortName());
                    } else {
                        vehicles.append(", " + route.getShortName());
                    }
                }
                if (vehicles == null) {
                    vehicles = new StringBuilder();
                    vehicles.append("neobsluhovaná");
                }
                JsonObjectBuilder stopJOB = Json.createObjectBuilder();
                stopJOB.add(sD.getStop().getId().getClass().getSimpleName(), sD.getStop().getId().getId());
                stopJOB.add("name", sD.getStop().getName());
                stopJOB.add("lat", sD.getStop().getLat());
                stopJOB.add("lon", sD.getStop().getLon());
                stopJOB.add("vehicles", vehicles.toString());

                stopsJAB.add(stopJOB);

            }

            JsonObjectBuilder stopsJOB = Json.createObjectBuilder();
            stopsJOB.add("stops", stopsJAB);
            JsonObject stopsJO = stopsJOB.build();
            System.out.println(stopsJO.toString());
            jw.writeObject(stopsJO);
            tx.commit(); //closes transaction
            session.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(DatabaseConnector.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                fos.close();
            } catch (IOException ex) {
                Logger.getLogger(DatabaseConnector.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }

    public class CustomComparator implements Comparator<GtfsRoutes> {

        @Override
        public int compare(GtfsRoutes o1, GtfsRoutes o2) {
            if (!((o1.getShortName().contains("N") || o2.getShortName().contains("N")) || (o1.getShortName().contains("X") || o2.getShortName().contains("X")))) {
                Integer object1 = Integer.parseInt(o1.getShortName());
                Integer object2 = Integer.parseInt(o2.getShortName());
                return object1.compareTo(object2);
            }
            return o1.getShortName().compareTo(o2.getShortName());
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
                    System.out.println(stopTimes.getGtfsTrips().getGtfsRoutes().getShortName() + " " + stop.getName() + " " + stopTimes.getGtfsTrips().getTripHeadsign() + " " + Utils.secsToHMS(stopTimes.getArrivalTime()));
                }
            }
        }
        System.out.println((new Date().getTime() - date1.getTime()));

    }

}
