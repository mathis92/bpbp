package sk.cagani.stuba.bpbp.api;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonWriter;
import javax.json.stream.JsonGenerator;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.slf4j.LoggerFactory;
import sk.cagani.stuba.bpbp.serverApp.DatabaseConnector;
import stuba.bpbphibernatemapper.GtfsAgencies;
import stuba.bpbphibernatemapper.GtfsStopTimes;
import stuba.bpbphibernatemapper.GtfsStops;
import stuba.bpbphibernatemapper.GtfsTrips;
import stuba.bpbphibernatemapper.GtfsTripsId;
import stuba.bpbphibernatemapper.Poi;
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
public class VehicleAPI extends HttpServlet {

    private final org.slf4j.Logger logger = LoggerFactory.getLogger(VehicleAPI.class);

    private Session session;

    private String agencyId = null;

    public VehicleAPI() {
        agencyId = DatabaseConnector.getSession().createCriteria(GtfsAgencies.class).list().get(0).toString();
        System.out.println("agency: " + agencyId);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        System.out.println("[POST]");

        switch (request.getRequestURI()) {
            case "/api/vehicle/updateLocation":
                TripPositions tripPosition = new TripPositions(new GtfsTrips(new GtfsTripsId(agencyId, request.getParameter("gtfs_trip_id"))), Float.parseFloat(request.getParameter("lat")), Float.parseFloat(request.getParameter("lon")), 0, Float.parseFloat(request.getParameter("spd")), Float.parseFloat(request.getParameter("acc")));

                session = DatabaseConnector.getSession();
                session.save(tripPosition);
                session.getTransaction().commit();
                session.close();
                //v buducnosti tu zrob to cekovanie pred zastavkou a potom ak hej, tak treba tie linky najblizsie poslat abo co
                break;
            case "/api/vehicle/getStopsAndPoi":
                response.setContentType("text/json;charset=utf-8");
                Map<String, Boolean> jwConfig = new HashMap<>();
                jwConfig.put(JsonGenerator.PRETTY_PRINTING, Boolean.TRUE);
                JsonWriter jw = Json.createWriterFactory(jwConfig).createWriter(response.getOutputStream());

                session = DatabaseConnector.getSession();
                List<Poi> poiList = session.createCriteria(Poi.class).list();                               
                List<GtfsStopTimes> stopTimeList = session.createCriteria(GtfsStopTimes.class).add(Restrictions.like("trip_id", request.getParameter("gtfs_trip_id"))).list();
                
                //JsonArrayBuilder stopsJAB = Json.createArrayBuilder();
                
                for (GtfsStopTimes stopTime : stopTimeList) {
                    //JsonObjectBuilder stopJOB = Json.createObjectBuilder();
                    System.out.println("zastafka: " + stopTime.getGtfsStops().getName() + "  cas: " + stopTime.getArrivalTime());                                  
                }               
                session.getTransaction().commit();
                session.close();

                JsonArrayBuilder poiJAB = Json.createArrayBuilder();

                for (Poi poi : poiList) {
                    JsonObjectBuilder poiJOB = Json.createObjectBuilder();
                    poiJOB.add("title", poi.getTitle());
                    poiJOB.add("lat", poi.getLat());
                    poiJOB.add("lon", poi.getLon());
                    poiJOB.add("radius", poi.getRadius());
                    poiJOB.add("filePath", poi.getFilePath());

                    poiJAB.add(poiJOB);
                }

                
                JsonObjectBuilder poisJOB = Json.createObjectBuilder();
                poisJOB.add("poiList", poiJAB);
                JsonObject poisJO = poisJOB.build();
                System.out.println(poisJO.toString());
                
                
                jw.writeObject(poisJO);
                break;
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

            case "/api/vehicle/39":
                System.out.println("som v spoji 39 " + request.getRequestURI() + " " + request.getRequestURL());

                //ResultSet rs;
                List<TripPositions> positionList = DatabaseConnector.getSession().createCriteria(TripPositions.class).list();

                JsonArrayBuilder positionJAB = Json.createArrayBuilder();

                for (TripPositions tripPositions : positionList) {
                    JsonObjectBuilder positionJOB = Json.createObjectBuilder();
                    positionJOB.add("lat", tripPositions.getLat());
                    positionJOB.add("lon", tripPositions.getLon());
                    positionJOB.add("spd", tripPositions.getSpeed());

                    positionJAB.add(positionJOB);
                }

                JsonObjectBuilder positionJOB = Json.createObjectBuilder();
                positionJOB.add("positionList", positionJAB);
                JsonObject positionJO = positionJOB.build();
                System.out.println(positionJO.toString());
                jw.writeObject(positionJO);

                /*
                 JsonObjectBuilder locationJOB = Json.createObjectBuilder();
                 ResultSetMetaData metadata = rs.getMetaData();
                
                 while (rs.next()) {
                 for (int i = 1; i < 8; i++) {
                 locationJOB.add(metadata.getColumnName(i), rs.getString(i));
                 }
                 }
                 JsonObjectBuilder coordinatesJOB = Json.createObjectBuilder().add("coordinates", locationJOB);
                
                 jw.writeObject(coordinatesJOB.build());*/
                break;

            case "/api/getPoi":
                session = DatabaseConnector.getSession();
                List<Poi> poiList = session.createCriteria(Poi.class).list();
                session.getTransaction().commit();

                JsonArrayBuilder poiJAB = Json.createArrayBuilder();

                for (Poi poi : poiList) {
                    JsonObjectBuilder poiJOB = Json.createObjectBuilder();
                    poiJOB.add("title", poi.getTitle());
                    poiJOB.add("lat", poi.getLat());
                    poiJOB.add("lon", poi.getLon());
                    poiJOB.add("radius", poi.getRadius());
                    poiJOB.add("filePath", poi.getFilePath());

                    poiJAB.add(poiJOB);
                }

                JsonObjectBuilder poisJOB = Json.createObjectBuilder();
                poisJOB.add("poiList", poiJAB);
                JsonObject poisJO = poisJOB.build();
                System.out.println(poisJO.toString());
                jw.writeObject(poisJO);
                break;
            default: {
                response.getOutputStream().write(("invalid call " + request.getRequestURI()).getBytes());
            }
        }
        response.setStatus(HttpServletResponse.SC_OK);
    }
}
