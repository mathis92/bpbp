package sk.mathis.stuba.bpbp;

import java.io.IOException;
import java.io.StringReader;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
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
import org.slf4j.LoggerFactory;
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
public class MainServletVajca extends HttpServlet {

    private final Mapper mapper;
    private final org.slf4j.Logger logger = LoggerFactory.getLogger(MainServletVajca.class);
    
    private Session session;
    
    private String agencyId = "01";

    public MainServletVajca(Mapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        System.out.println("[POST]");
        response.setContentType("text/html;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().println("<h1>ššaaaak to postuje</h1>");

        Map<String, Boolean> configMap = new HashMap<>();
        configMap.put(JsonGenerator.PRETTY_PRINTING, Boolean.TRUE);

        String vehicleParam = request.getParameter("vehicle");
        String coordinatesParam = request.getParameter("coordinates");
        System.out.println(coordinatesParam);
        /*
        if (coordinatesParam != null) {
            try (JsonReader jr = Json.createReader(new StringReader(coordinatesParam))) {
                coordinatesJO = jr.readObject();
                JsonObject latlon = coordinatesJO.getJsonObject("coordinates");

                longitude = Double.parseDouble(latlon.getString("longitude"));
                latitude = Double.parseDouble(latlon.getString("latitude"));
                System.out.println("lat = " + latitude + "\nlon = " + longitude);
            }
        }
        */
        if (vehicleParam != null && coordinatesParam != null) {
            JsonObject coordinates;
            System.out.println("line: " + vehicleParam);

            try (JsonReader jr = Json.createReader(new StringReader(coordinatesParam))) {
                //coordinatesJO = jr.readObject();
                coordinates = jr.readObject().getJsonObject("coordinates");
                System.out.println(coordinates);
            }
            
            TripPositions tripPosition = new TripPositions(new GtfsTrips(new GtfsTripsId(agencyId, coordinates.getString("gtfs_trip_id"))), Float.parseFloat(coordinates.getString("lat")), Float.parseFloat(coordinates.getString("lon")), 0, Float.parseFloat(coordinates.getString("spd")), Float.parseFloat(coordinates.getString("acc")));
            session = DatabaseConnector.getSession();
            session.save(tripPosition);
            session.getTransaction().commit();                                    
            
            /*
            switch (vehicleParam) {
                case "39": {
                    try {
                        locationData.add("1");
                        System.out.println("[39] idem zrobic query");
                        mapper.insertTableRow("location", locationData);
                        break;
                    } catch (SQLException ex) {
                        Logger.getLogger(MainServletVajca.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                case "31": {
                    try {
                        locationData.add("2");
                        System.out.println("[31] idem zrobic query");
                        mapper.insertTableRow("location", locationData);
                        break;
                    } catch (SQLException ex) {
                        Logger.getLogger(MainServletVajca.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }

            }
            */
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
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

                    ResultSet rs;

                    String query = "SELECT * FROM `location` order by `id` desc limit 1 ";
                    rs = mapper.executeQuery(query);

                    JsonObjectBuilder locationJOB = Json.createObjectBuilder();
                    ResultSetMetaData metadata = rs.getMetaData();

                    while (rs.next()) {
                        for (int i = 1; i < 8; i++) {
                            locationJOB.add(metadata.getColumnName(i), rs.getString(i));
                        }
                    }
                    JsonObjectBuilder coordinatesJOB = Json.createObjectBuilder().add("coordinates", locationJOB);

                    jw.writeObject(coordinatesJOB.build());
                    break;

                case "/api/getPoi":
                    session = DatabaseConnector.getSession();
                    List<Poi> poiList = session.createCriteria(Poi.class).list();
                    session.getTransaction().commit();
                    
                    JsonArrayBuilder poiJAB = Json.createArrayBuilder();
                    
                    for(Poi poi : poiList) {
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
        } catch (SQLException ex) {
            Logger.getLogger(MainServletVajca.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
