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
import org.hibernate.Hibernate;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.type.Type;
import org.slf4j.LoggerFactory;
import sk.cagani.stuba.bpbp.device.RouteData;
import sk.cagani.stuba.bpbp.serverApp.DatabaseConnector;
import sk.cagani.stuba.bpbp.utilities.Utils;
import static sk.cagani.stuba.bpbp.utilities.Utils.secsToHMS;
import stuba.bpbpdatabasemapper.GtfsRoutes;
import stuba.bpbpdatabasemapper.GtfsStopTimes;
import stuba.bpbpdatabasemapper.GtfsStops;
import stuba.bpbpdatabasemapper.GtfsTrips;
import stuba.bpbpdatabasemapper.GtfsTripsId;
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
        //   System.out.println("\n\n" + DatabaseConnector.getStatistics() + "\n\n");
        response.setContentType("text/json; charset=UTF-8");
        Map<String, Object> jwConfig = new HashMap<>();
        jwConfig.put(JsonGenerator.PRETTY_PRINTING, true);
        JsonWriter jw = Json.createWriterFactory(jwConfig).createWriter(response.getOutputStream(), Charset.forName("UTF-8"));
     //   logger.info("[req URI]: " + request.getRequestURI());
      //  logger.info(request.getParameterMap().toString());
        if (!request.getParameterMap().isEmpty()) {
            switch (request.getParameter("requestContent")) {
                case "CurrentStop": {
                    if (request.getParameter("stopName") != null) {
                        String requestStopName = request.getParameter("stopName");

                        List<RouteData> routeList;
                        List<RouteData> routeListTomorrow;

                        Session session = DatabaseConnector.getSession();
                        Transaction tx = null;
                        tx = session.beginTransaction(); //open the transaction
                        int routeCountRequest = 15;
                        if (request.getParameter("count") != null) {
                            routeCountRequest = Integer.parseInt(request.getParameter("count"));
                        }
                        int secondsSinceMidnight = Utils.getSecondsFromMidnight();
                        List<GtfsStops> stopList = session.createCriteria(GtfsStops.class).add(Restrictions.eq("name", requestStopName)).list();
                        routeList = getRouteData(Utils.getActualServiceId(), secondsSinceMidnight, stopList, session, request);
                //        logger.debug(routeList.size() + " velkost routeListu");
                        if (routeList.isEmpty() || (routeList.size() < routeCountRequest)) {
               //             logger.debug("route List je empty vchadzam do dalsieho citania");
                            routeListTomorrow = getRouteData(Utils.getTomorrowServiceId(), 0, stopList, session, request);
              //              logger.debug("naslo sa " + routeListTomorrow.size() + " zaznamov");
                            routeList.addAll(routeListTomorrow);
               //             logger.debug("spolu je to " + routeList.size());

                        }
                        Collections.sort(routeList, new CustomComparator());

                        JsonArrayBuilder routesJAB = Json.createArrayBuilder();
                        int routeIndex = 0;

                        for (RouteData route : routeList) {
                            if (routeIndex < routeCountRequest) {
                                JsonObjectBuilder routeJOB = Json.createObjectBuilder();
                                routeJOB.add("vehicleId", route.getGtfsTrip().getId().getId());
                                routeJOB.add("vehicleShortName", route.getRoute().getShortName());
                                routeJOB.add("stopHeadSign", route.getGtfsTrip().getTripHeadsign());
                                routeJOB.add("routeType", route.getRoute().getType());
                                if (route.getTripPositions() != null) {
                                    routeJOB.add("delay", route.getTripPositions().getDelay());
                                } else {
                                    routeJOB.add("delay", "notStarted");
                                }
                                routeJOB.add("arrivalTime", secsToHMS(route.getStopTime().getArrivalTime()));
                                routesJAB.add(routeJOB);
                            }
                            routeIndex++;
                        }
                        tx.commit();

                        session.close();

                        JsonArray routesJA = routesJAB.build();
             //           logger.info(routesJA.toString());
                        jw.writeArray(routesJA);
                    }
                    break;
                }
                case "currentVehicleDetail": {
                //    logger.debug("in api call currentVehicleDetail " + request.getRequestURI() + " " + request.getRequestURL());
                    Session session = DatabaseConnector.getSession();
                    Transaction tx = session.beginTransaction();
                    String vehicleId = request.getParameter("vehicleId");
                //    logger.debug("vehicleID" + vehicleId);
                    GtfsTripsId tripId = new GtfsTripsId("01", vehicleId);
                    GtfsTrips currentTrip = (GtfsTrips) session.get(GtfsTrips.class, tripId);
                    List<TripPositions> currentVehicleDetailList = session.createCriteria(TripPositions.class).add(Restrictions.eq("gtfsTrips", currentTrip)).add(Restrictions.eq("state", "a")).list();
                    JsonArrayBuilder vehicleJAB = Json.createArrayBuilder();
               //     System.out.println(currentVehicleDetailList.size());

                    if (!currentVehicleDetailList.isEmpty()) {
                        for (TripPositions currentVehicleDetail : currentVehicleDetailList) {
                 //           logger.debug((Utils.getSecondsFromMidnight(currentVehicleDetail.getModifiedAt()) - currentVehicleDetail.getDelay()) + " next stop arrival time " + currentVehicleDetail.getGtfsTrips().getId().getId());
                            List<GtfsStopTimes> nextStopTime = session.createCriteria(GtfsStopTimes.class, "stopTimes")
                                    .add(Restrictions.eq("stopTimes.gtfsTrips", currentVehicleDetail.getGtfsTrips()))
                                    .addOrder(Order.asc("arrivalTime"))
                                    .list();
                            GtfsTrips trip = currentVehicleDetail.getGtfsTrips();
                            GtfsRoutes route = trip.getGtfsRoutes();
                            JsonObjectBuilder vehicleJOB = Json.createObjectBuilder();
                            vehicleJOB.add("id", trip.getId().getId());
                            vehicleJOB.add("shortName", route.getShortName());
                            vehicleJOB.add("vehicleType", route.getType());
                            vehicleJOB.add("lon", currentVehicleDetail.getLon());
                            vehicleJOB.add("lat", currentVehicleDetail.getLat());
                            vehicleJOB.add("headingTo", trip.getTripHeadsign());
                            vehicleJOB.add("delay", currentVehicleDetail.getDelay());
                            vehicleJOB.add("speed", currentVehicleDetail.getSpeed());
                            if (nextStopTime.isEmpty()) {
                                vehicleJOB.add("lastStop", "Not supported");
                                vehicleJOB.add("nextStop", "Not supported");
                                vehicleJOB.add("arrivalTime", "Not supported");

                            } else {
                                if (currentVehicleDetail.getNextStopNumber() == 0) {
                                    vehicleJOB.add("lastStop", "Vehicle is at start");
                                    vehicleJOB.add("nextStop", nextStopTime.get(currentVehicleDetail.getNextStopNumber() + 1).getGtfsStops().getName());
                                    vehicleJOB.add("arrivalTime", Utils.secsToHMS(nextStopTime.get(currentVehicleDetail.getNextStopNumber() + 1).getArrivalTime()));
                                } else {
                                    vehicleJOB.add("lastStop", nextStopTime.get(currentVehicleDetail.getNextStopNumber() - 1).getGtfsStops().getName());
                                    vehicleJOB.add("nextStop", nextStopTime.get(currentVehicleDetail.getNextStopNumber()).getGtfsStops().getName());
                                    vehicleJOB.add("arrivalTime", Utils.secsToHMS(nextStopTime.get(currentVehicleDetail.getNextStopNumber()).getArrivalTime()));
                                }
                            }

                            vehicleJAB.add(vehicleJOB);
                        }
                    } else {
                        List<GtfsStopTimes> nextStopTime = session.createCriteria(GtfsStopTimes.class, "stopTimes")
                                .add(Restrictions.eq("stopTimes.gtfsTrips", currentTrip))
                                .addOrder(Order.asc("arrivalTime"))
                                .list();
                        JsonObjectBuilder vehicleJOB = Json.createObjectBuilder();
                        vehicleJOB.add("id", currentTrip.getId().getId());
                        vehicleJOB.add("shortName", currentTrip.getGtfsRoutes().getShortName());
                        vehicleJOB.add("vehicleType", currentTrip.getGtfsRoutes().getType());
                        vehicleJOB.add("lon", "0");
                        vehicleJOB.add("lat", "0");
                        vehicleJOB.add("headingTo", currentTrip.getTripHeadsign());
                        vehicleJOB.add("delay", "notStarted");
                        vehicleJOB.add("speed", "0");

                        vehicleJOB.add("lastStop", "not started Yet");
                        vehicleJOB.add("nextStop", nextStopTime.get(1).getGtfsStops().getName());
                        vehicleJOB.add("arrivalTime", Utils.secsToHMS(nextStopTime.get(1).getArrivalTime()));

                        vehicleJAB.add(vehicleJOB);
                    }

                    tx.commit();
                    session.close();
                    JsonArray vehicleJA = vehicleJAB.build();
               //     System.out.println(vehicleJA.toString());
                    jw.writeArray(vehicleJA);
                    break;
                }
                case "vehiclesPositions": {
               //     logger.debug("in api call vehiclePositions " + request.getRequestURI() + " " + request.getRequestURL());
                    Session session = DatabaseConnector.getSession();
                    Transaction tx = session.beginTransaction();
                    double northLon = Double.parseDouble(request.getParameter("northLon"));
                    double eastLat = Double.parseDouble(request.getParameter("eastLat"));
                    double westLat = Double.parseDouble(request.getParameter("westLat"));
                    double southLon = Double.parseDouble(request.getParameter("southLon"));

                 //   System.out.println("lat " + (westLat) + " " + (eastLat) + " lon " + (northLon) + " " + (southLon));
                    List<TripPositions> tripPositionsList = session.createCriteria(TripPositions.class).add(Restrictions.between("lat", westLat, eastLat)).add(Restrictions.between("lon", southLon, northLon)).add(Restrictions.eq("state", "a")).list();

                    JsonArrayBuilder vehicleJAB = Json.createArrayBuilder();
                 //   System.out.println(tripPositionsList.size());
                    for (TripPositions tripPosition : tripPositionsList) {
                //        logger.debug((Utils.getSecondsFromMidnight(tripPosition.getModifiedAt()) - tripPosition.getDelay()) + " next stop arrival time " + tripPosition.getGtfsTrips().getId().getId());
                        List<GtfsStopTimes> nextStopTime = session.createCriteria(GtfsStopTimes.class, "stopTimes")
                                .add(Restrictions.eq("stopTimes.gtfsTrips", tripPosition.getGtfsTrips()))
                                .addOrder(Order.asc("arrivalTime"))
                                .list();

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

                        if (nextStopTime.isEmpty()) {
                            vehicleJOB.add("lastStop", "Not supported");
                            vehicleJOB.add("nextStop", "Not supported");
                            vehicleJOB.add("arrivalTime", "Not supported");

                        } else {
                            if (tripPosition.getNextStopNumber() == 0) {
                                vehicleJOB.add("lastStop", "Vehicle is at start");
                                vehicleJOB.add("nextStop", nextStopTime.get(tripPosition.getNextStopNumber() + 1).getGtfsStops().getName());
                                vehicleJOB.add("arrivalTime", Utils.secsToHMS(nextStopTime.get(tripPosition.getNextStopNumber() + 1).getArrivalTime()));
                            } else {
                                vehicleJOB.add("lastStop", nextStopTime.get(tripPosition.getNextStopNumber() - 1).getGtfsStops().getName());
                                vehicleJOB.add("nextStop", nextStopTime.get(tripPosition.getNextStopNumber()).getGtfsStops().getName());
                                vehicleJOB.add("arrivalTime", Utils.secsToHMS(nextStopTime.get(tripPosition.getNextStopNumber()).getArrivalTime()));
                            }
                        }
                        vehicleJAB.add(vehicleJOB);
                    }

                    tx.commit();
                    session.close();
                    JsonArray vehicleJA = vehicleJAB.build();
                //    System.out.println(vehicleJA.toString());
                    jw.writeArray(vehicleJA);
                    break;
                }
                case "allStops": {
             //       logger.debug("in api call allStops " + request.getRequestURI() + " " + request.getRequestURL());
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
                //    System.out.println(stopsJO.toString());
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

            //    logger.debug("in api call allStops " + request.getRequestURI() + " " + request.getRequestURL());
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

            //    System.out.println(stopsJO.toString());
                jw.writeObject(stopsJO);
            }

            default: {
                response.getOutputStream().write(("invalid call " + request.getRequestURI()).getBytes());
            }
        }
        response.setStatus(HttpServletResponse.SC_OK);
    }

    public class CustomComparator implements Comparator<RouteData> {

        @Override
        public int compare(RouteData o1, RouteData o2) {
            return o1.getStopTime().getArrivalTime().compareTo(o2.getStopTime().getArrivalTime());
        }
    }

    public List<RouteData> getRouteData(String serviceId, Integer arrivalTime, List<GtfsStops> stopList, Session session, HttpServletRequest request) {
        List<RouteData> routeList = new ArrayList<>();
        for (GtfsStops stop : stopList) {
            List<GtfsStopTimes> stopTimesList;

            if (request.getParameter("count") != null) {
                stopTimesList = session.createCriteria(GtfsStopTimes.class, "stopTime")
                        .createAlias("stopTime.gtfsTrips", "trip")
                        //.createAlias("trip.tripPositionses", "position")
                        //.setProjection(Projections.sqlProjection("sum(cast(position.delay as signed)+ arrivalTime) as timewdelay", new String[] {"timewdelay"} , new Type[] {Hibernate.}))
                        .add(Restrictions.eq("gtfsStops", stop))
                        .add(Restrictions.ge("arrivalTime", arrivalTime))
                        .add(Restrictions.eq("trip.serviceIdId", serviceId))
                        .addOrder(Order.asc("arrivalTime"))
                        .setMaxResults(3).list();
            } else {
                stopTimesList = session.createCriteria(GtfsStopTimes.class, "stopTime")
                        .createAlias("stopTime.gtfsTrips", "trip")
                        .add(Restrictions.eq("gtfsStops", stop))
                        .add(Restrictions.ge("arrivalTime", arrivalTime))
                        .add(Restrictions.eq("trip.serviceIdId", serviceId))
                        .addOrder(Order.asc("arrivalTime"))
                        .setMaxResults(10).list();

            }
            for (GtfsStopTimes stopTimes : stopTimesList) {
                List<TripPositions> tripPositionList = session.createCriteria(TripPositions.class).add(Restrictions.eq("gtfsTrips", stopTimes.getGtfsTrips())).list();
                TripPositions lastPosition;
                if (!tripPositionList.isEmpty()) {
                    lastPosition = tripPositionList.get(0);
                } else {
                 //   logger.info("empty trip positions");
                    lastPosition = null;
                }
                Integer delay = 0;
                if (lastPosition != null) {
                    delay = lastPosition.getDelay();
                }
                RouteData routeData = new RouteData(stopTimes.getGtfsTrips().getGtfsRoutes(), stopTimes, stopTimes.getGtfsTrips(), lastPosition);
                routeList.add(routeData);
            }
        }
        return routeList;
    }

}
