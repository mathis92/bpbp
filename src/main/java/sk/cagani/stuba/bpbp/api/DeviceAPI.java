package sk.cagani.stuba.bpbp.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
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
import org.hibernate.criterion.Restrictions;
import org.json.simple.JSONArray;
import org.slf4j.LoggerFactory;
import sk.cagani.stuba.bpbp.device.RouteData;
import sk.cagani.stuba.bpbp.serverApp.DatabaseConnector;
import stuba.bpbphibernatemapper.GtfsRoutes;
import stuba.bpbphibernatemapper.GtfsStopTimes;
import stuba.bpbphibernatemapper.GtfsStops;
import stuba.bpbphibernatemapper.GtfsTrips;

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
        response.setContentType("text/html;charset=utf-8");
        response.setContentType("text/json");
        Map<String, Object> jwConfig = new HashMap<>();
        jwConfig.put(JsonGenerator.PRETTY_PRINTING, true);
        JsonWriter jw = Json.createWriterFactory(jwConfig).createWriter(response.getOutputStream());
        System.out.println("[req URI]: " + request.getRequestURI());
        System.out.println(request.getParameterMap().toString());

        if (!request.getParameterMap().isEmpty()) {
            switch (request.getParameter("requestContent")) {
                case "CurrentStop": {
                    if (request.getParameter("stopName") != null) {
                        List<RouteData> routeList = new ArrayList<>();
                        Calendar c = Calendar.getInstance();
                        c.setTime(new Date());
                        c.set(Calendar.HOUR_OF_DAY, 0);
                        c.set(Calendar.MINUTE, 0);
                        c.set(Calendar.SECOND, 0);
                        c.set(Calendar.MILLISECOND, 0);
                        
                        System.out.println(c.getTimeInMillis());
                        Long timeSinceMidnight = new Date().getTime() - (c.getTimeInMillis() );
                        Long secondsSinceMidnight = timeSinceMidnight/1000;
                        System.out.println(secondsSinceMidnight.intValue() + " since midnight ");

                        session = DatabaseConnector.getSession();
                        List<GtfsStops> stopList = session.createCriteria(GtfsStops.class).add(Restrictions.eq("name", request.getParameter("stopName"))).list();
                        for (GtfsStops stop : stopList){
                            for (GtfsStopTimes stopTime : (Set<GtfsStopTimes>)stop.getGtfsStopTimeses()) {
                                GtfsRoutes route = stopTime.getGtfsTrips().getGtfsRoutes();
                                RouteData routeData = new RouteData(route, stopTime, stopTime.getGtfsTrips());
                                if (!routeList.contains(routeData)) {
                                    routeList.add(routeData);
                                }
                            }
                        }
                        List<GtfsTrips> tripList = session.createCriteria(GtfsTrips.class).add(Restrictions.eq("shapeIdId", "Prac.dny_0")).list();
                        for (GtfsTrips trip : tripList){
                            for(GtfsStopTimes stopTime : (Set<GtfsStopTimes>)trip.getGtfsStopTimeses()){
                                System.out.println(stopTime.getGtfsStops() + " zastafka - > prichod " + stopTime.getArrivalTime());
                            }
                        }                    
                        
                        JsonArrayBuilder routesJAB = Json.createArrayBuilder();
                        for (RouteData route : routeList) {
                            JsonObjectBuilder routeJOB = Json.createObjectBuilder();
                            routeJOB.add("name", route.getRoute().getShortName());
                            routeJOB.add("stopHeadSign", route.getGtfsTrip().getTripHeadsign());
                            routeJOB.add("arrivalTime", route.getStopTime().getArrivalTime());
                            routesJAB.add(routeJOB);
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
                List<GtfsStops> stopsList = session.createCriteria(GtfsStops.class).list();
                session.getTransaction().commit(); //closes transaction

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
            }

            default: {
                response.getOutputStream().write(("invalid call " + request.getRequestURI()).getBytes());
            }
        }
        response.setStatus(HttpServletResponse.SC_OK);
    }
}
