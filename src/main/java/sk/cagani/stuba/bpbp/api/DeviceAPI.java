package sk.cagani.stuba.bpbp.api;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.simple.JSONArray;
import org.slf4j.LoggerFactory;
import sk.cagani.stuba.bpbp.device.RouteData;
import sk.cagani.stuba.bpbp.serverApp.DatabaseConnector;
import stuba.bpbphibernatemapper.GtfsRoutes;
import stuba.bpbphibernatemapper.GtfsStopTimes;
import stuba.bpbphibernatemapper.GtfsStops;
import stuba.bpbphibernatemapper.GtfsTrips;
import stuba.bpbphibernatemapper.TripPositions;

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

    private Session session;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        System.out.println("[POST]");
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
                        /*
                         Charset.forName("UTF-8").encode(requestStopName);
                         byte[] data = org.apache.commons.codec.binary.Base64.decodeBase64(requestStopName);
                         String stopName = new String(data, "UTF-8");
                         */
                        List<RouteData> routeList = new ArrayList<>();
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

                        session = DatabaseConnector.getSession();
                        List<GtfsStops> stopList = session.createCriteria(GtfsStops.class).add(Restrictions.eq("name", requestStopName)).list();
                        for (GtfsStops stop : stopList) {
                            List<GtfsStopTimes> stopTimesList = session.createCriteria(GtfsStopTimes.class).add(Restrictions.eq("gtfsStops", stop)).add(Restrictions.between("arrivalTime", secondsSinceMidnight.intValue(), secondsSinceMidnight.intValue() + 1200)).addOrder(Order.asc("arrivalTime")).list();
                            for (GtfsStopTimes stopTimes : stopTimesList) {
                                if (stopTimes.getGtfsTrips().getServiceIdId().equals("Prac.dny_0")) {
                                    List<TripPositions> tripPositionList = session.createCriteria(TripPositions.class).add(Restrictions.eq("gtfsTrips", stopTimes.getGtfsTrips())).addOrder(Order.desc("id")).list();
                                    TripPositions lastPosition;
                                    if (!tripPositionList.isEmpty()) {
                                        lastPosition = tripPositionList.get(0);
                                    } else {
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
                        session.getTransaction().commit();
                        session.close();
                        JsonArray routesJA = routesJAB.build();
                        System.out.println(routesJA.toString());
                        jw.writeArray(routesJA);
                    }
                    break;
                }

                case "allStops": {
                    logger.debug("in api call allStops " + request.getRequestURI() + " " + request.getRequestURL());
                    session = DatabaseConnector.getSession();
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
                session = DatabaseConnector.getSession();
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
