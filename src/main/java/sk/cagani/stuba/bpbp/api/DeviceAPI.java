package sk.cagani.stuba.bpbp.api;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonWriter;
import javax.json.stream.JsonGenerator;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.slf4j.LoggerFactory;
import sk.cagani.stuba.bpbp.device.RouteData;
import sk.cagani.stuba.bpbp.serverApp.DatabaseConnector;
import sk.cagani.stuba.bpbp.utilities.Utils;
import stuba.bpbpdatabasemapper.GtfsRoutes;
import stuba.bpbpdatabasemapper.GtfsStopTimes;
import stuba.bpbpdatabasemapper.GtfsStops;
import stuba.bpbpdatabasemapper.GtfsTrips;
import stuba.bpbpdatabasemapper.TripPositions;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author martinhudec
 */
public class DeviceAPI extends HttpServlet {

    private final org.slf4j.Logger logger = LoggerFactory.getLogger(DeviceAPI.class);

    // private Session session;
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // System.out.println("[POST]");
        System.out.println("\n\n" + DatabaseConnector.getStatistics() + "\n\n");
        response.setContentType("text/json; charset=UTF-8");
        Map<String, Object> jwConfig = new HashMap<>();
        jwConfig.put(JsonGenerator.PRETTY_PRINTING, true);
        JsonWriter jw = Json.createWriterFactory(jwConfig).createWriter(response.getOutputStream(), Charset.forName("UTF-8"));
        System.out.println("[req URI]: " + request.getRequestURI());
        System.out.println(request.getParameterMap().toString());
        Boolean isBreak = false;
        if (!request.getParameterMap().isEmpty()) {
            switch (request.getParameter("requestContent")) {
                case "CurrentStop": {
                    if (request.getParameter("stopName") != null) {
                        String requestStopName = request.getParameter("stopName");

                        List<RouteData> routeList = new ArrayList<>();

                        Session session = DatabaseConnector.getSession();
                        Transaction tx = null;
                        tx = session.beginTransaction(); //open the transaction
                        int secondsSinceMidnight = Utils.getSecondsFromMidnight();
                        List<GtfsStops> stopList = session.createCriteria(GtfsStops.class).add(Restrictions.eq("name", requestStopName)).list();
                        for (GtfsStops stop : stopList) {
                            List<GtfsStopTimes> stopTimesList;
                            if (request.getParameter("count") != null) {
                                System.out.println("secondsSinceMidnight " + secsToHMS(secondsSinceMidnight) + " " + stop.getName());
                                stopTimesList = session.createCriteria(GtfsStopTimes.class).add(Restrictions.eq("gtfsStops", stop)).add(Restrictions.gt("arrivalTime", secondsSinceMidnight)).addOrder(Order.asc("arrivalTime")).setMaxResults(Integer.parseInt(request.getParameter("count"))).list();
                                    System.out.println("stopTimesList count " + stopTimesList.size());
                            } else {
                                stopTimesList = session.createCriteria(GtfsStopTimes.class).add(Restrictions.eq("gtfsStops", stop)).add(Restrictions.gt("arrivalTime", secondsSinceMidnight)).addOrder(Order.asc("arrivalTime")).setMaxResults(15).list();
                            }
                            for (GtfsStopTimes stopTimes : stopTimesList) {
                                if (stopTimes.getGtfsTrips().getServiceIdId().equals(Utils.getActualServiceId())) {
                                  //  System.out.println(stopTimes.getGtfsTrips().getId().getId());
                                    List<TripPositions> tripPositionList = session.createCriteria(TripPositions.class).add(Restrictions.eq("gtfsTrips", stopTimes.getGtfsTrips())).list();
                                    TripPositions lastPosition;
                                    if (!tripPositionList.isEmpty()) {
                                        lastPosition = tripPositionList.get(0);
                                        //System.out.println(lastPosition.getDelay());
                                    } else {
                                        System.out.println("empty trip positions");
                                        lastPosition = null;
                                    }
                                    Integer delay = 0;
                                    if (lastPosition != null) {
                                        delay = lastPosition.getDelay();
                                    }
                                    //                                  System.out.println(stopTimes.getGtfsTrips().getGtfsRoutes().getShortName() + " " + stop.getName() + " " + stopTimes.getGtfsTrips().getTripHeadsign() + " " + secsToHMS(stopTimes.getArrivalTime()) + " DELAY " + delay + " secs ");
                                    RouteData routeData = new RouteData(stopTimes.getGtfsTrips().getGtfsRoutes(), stopTimes, stopTimes.getGtfsTrips(), lastPosition);
                                    // if (!routeList.contains(routeData)) {
                                    routeList.add(routeData);
                                }
                                // }
                            }
//                            System.out.println(stop.getName());
                        }
                        Collections.sort(routeList, new CustomComparator());

                        JsonArrayBuilder routesJAB = Json.createArrayBuilder();
                        int routeIndex = 0;
                        for (RouteData route : routeList) {
                            if (routeIndex < 15) {
                                JsonObjectBuilder routeJOB = Json.createObjectBuilder();
                                routeJOB.add("routeId", route.getRoute().getShortName());
                                routeJOB.add("stopHeadSign", route.getGtfsTrip().getTripHeadsign());
                                routeJOB.add("routeType", route.getRoute().getType());
                                if (route.getTripPositions() != null) {
                                    routeJOB.add("delay", route.getTripPositions().getDelay());
                                } else {
                                    routeJOB.add("delay", "0");
                                }
                                routeJOB.add("arrivalTime", secsToHMS(route.getStopTime().getArrivalTime()));
                                routesJAB.add(routeJOB);
                            }
                            routeIndex++;
                        }
                        tx.commit();

                        session.close();

                        JsonArray routesJA = routesJAB.build();
                        System.out.println(routesJA.toString());
                        jw.writeArray(routesJA);
                    }
                    break;
                }

                case "vehiclesPositions": {
                    logger.debug("in api call vehiclePositions " + request.getRequestURI() + " " + request.getRequestURL());
                    Session session = DatabaseConnector.getSession();
                    Transaction tx = session.beginTransaction();
                    double northLon = Double.parseDouble(request.getParameter("northLon"));
                    double eastLat = Double.parseDouble(request.getParameter("eastLat"));
                    double westLat = Double.parseDouble(request.getParameter("westLat"));
                    double southLon = Double.parseDouble(request.getParameter("southLon"));

                    System.out.println("lat " + (westLat) + " " + (eastLat) + " lon " + (northLon) + " " + (southLon));
                    List<TripPositions> tripPositionsList = session.createCriteria(TripPositions.class).add(Restrictions.between("lat", westLat, eastLat)).add(Restrictions.between("lon", southLon, northLon)).add(Restrictions.eq("state", "a")).list();
                    JsonArrayBuilder vehicleJAB = Json.createArrayBuilder();
                    System.out.println(tripPositionsList.size());
                    for (TripPositions tripPosition : tripPositionsList) {
                        GtfsTrips trip = tripPosition.getGtfsTrips();
                        //   GtfsStopTimes stopTime = session.createCriteria(GtfsStopTimes.class).add(Restrictions.eq("gtfsTrips", trip)).list().get(0);
                        GtfsRoutes route = trip.getGtfsRoutes();
                        JsonObjectBuilder vehicleJOB = Json.createObjectBuilder();
                        vehicleJOB.add("id", trip.getId().getId());
                        vehicleJOB.add("shortName", route.getShortName());
                        vehicleJOB.add("vehicleType", route.getType());
                        vehicleJOB.add("lon", tripPosition.getLon());
                        vehicleJOB.add("lat", tripPosition.getLat());
                        vehicleJOB.add("headingTo", trip.getTripHeadsign());
                        vehicleJOB.add("delay", tripPosition.getDelay());
                        vehicleJOB.add("speed", tripPosition.getSpeed());
                        vehicleJOB.add("lastStop", "maybeOnce");
                        vehicleJOB.add("nextStop", "maybeOnce");
                        vehicleJOB.add("arrivalTime", "maybeOnce");
                        vehicleJAB.add(vehicleJOB);
                    }

                    tx.commit();
                    session.close();
                    JsonArray vehicleJA = vehicleJAB.build();
                    System.out.println(vehicleJA.toString());
                    jw.writeArray(vehicleJA);
                    break;
                }
                case "allStops": {
                    logger.debug("in api call allStops " + request.getRequestURI() + " " + request.getRequestURL());
                    Session session = DatabaseConnector.getSession();
                    List<GtfsStops> stopsList = session.createCriteria(GtfsStops.class).list();
                    session.getTransaction().commit(); //closes transaction
                    session.close();
                    JsonArrayBuilder stopsJAB = Json.createArrayBuilder();
                    for (GtfsStops stop : stopsList) {
                        if (stop.getId().getId().endsWith("1")) {
                            JsonObjectBuilder stopJOB = Json.createObjectBuilder();
                            stopJOB.add(stop.getId().getClass().getSimpleName(), stop.getId().getId());
                            stopJOB.add("name", stop.getName());
                            stopJOB.add("lat", stop.getLat());
                            stopJOB.add("lon", stop.getLon());
                            stopsJAB.add(stopJOB);
                        }
                    }
                    JsonObjectBuilder stopsJOB = Json.createObjectBuilder();
                    stopsJOB.add("stops", stopsJAB);
                    JsonObject stopsJO = stopsJOB.build();
                    System.out.println(stopsJO.toString());
                    jw.writeObject(stopsJO);

                    break;
                }

                default: {
                    response.getOutputStream().write(("invalid call " + request.getRequestURI()).getBytes());
                }
            }
        } else {
            response.getOutputStream().write(("invalid call EMPTY PARAM" + request.getRequestURI()).getBytes());
        }

        response.setStatus(HttpServletResponse.SC_OK);

    }

    public double round(Double value) {
        return (Math.round(value * 10000) / 10000);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        System.out.println("[GET]");
        response.setContentType("text/json");
        Map<String, Object> jwConfig = new HashMap<>();
        jwConfig.put(JsonGenerator.PRETTY_PRINTING, true);
        JsonWriter jw = Json.createWriterFactory(jwConfig).createWriter(response.getOutputStream());
        System.out.println("[req URI]: " + request.getRequestURI());
        Map<String, String[]> parameterMap = request.getParameterMap();
        System.out.println(parameterMap.toString());
        switch (request.getParameter("requestContent")) {
            case "CurrentStop": {
                System.out.println("MAM TO TU");
            }
        }

        switch (request.getRequestURI()) {
            case "/api/allStops": {

                logger.debug("in api call allStops " + request.getRequestURI() + " " + request.getRequestURL());
                Session session = DatabaseConnector.getSession();
                List<GtfsStops> stopsList = session.createCriteria(GtfsStops.class
                ).list();
                session.getTransaction()
                        .commit(); //closes transaction

                JsonArrayBuilder stopsJAB = Json.createArrayBuilder();
                for (GtfsStops stop : stopsList) {
                    if (stop.getId().getId().endsWith("1")) {
                        JsonObjectBuilder stopJOB = Json.createObjectBuilder();
                        stopJOB.add(stop.getId().getClass().getSimpleName(), stop.getId().getId());
                        stopJOB.add("name", stop.getName());
                        stopJOB.add("lat", stop.getLat());
                        stopJOB.add("lon", stop.getLon());
                        stopsJAB.add(stopJOB);
                    }
                }
                JsonObjectBuilder stopsJOB = Json.createObjectBuilder();

                stopsJOB.add(
                        "stops", stopsJAB);
                JsonObject stopsJO = stopsJOB.build();

                System.out.println(stopsJO.toString());
                jw.writeObject(stopsJO);
            }

            default: {
                response.getOutputStream().write(("invalid call " + request.getRequestURI()).getBytes());
            }
        }
        response.setStatus(HttpServletResponse.SC_OK);
    }

    public String secsToHMS(int totalSecs) {
        int hours = totalSecs / 3600;
        int minutes = (totalSecs % 3600) / 60;
        int seconds = totalSecs % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);

    }

    public class CustomComparator implements Comparator<RouteData> {

        @Override
        public int compare(RouteData o1, RouteData o2) {
            return o1.getStopTime().getArrivalTime().compareTo(o2.getStopTime().getArrivalTime());
        }
    }

}
