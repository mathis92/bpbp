package sk.mathis.stuba.bpbp;

import java.io.IOException;
import java.io.StringReader;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
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

    private double longitude;
    private double latitude;
    private JsonObject coordinatesJO;
    private final Mapper mapper;

    public MainServletVajca(Mapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        System.out.println("[POST]");
        System.out.println("REQUEST -> " + request.toString());
        System.out.println("RESPONSE -> " + response.toString());
        response.setContentType("text/html;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().println("<h1>ššaaaak to postuje</h1>");

        Map<String, Boolean> configMap = new HashMap<>();
        configMap.put(JsonGenerator.PRETTY_PRINTING, Boolean.TRUE);

        String vehicleParam = request.getParameter("vehicle");
        String coordinatesParam = request.getParameter("coordinates");
        System.out.println(coordinatesParam);
        
        if (coordinatesParam != null) {
            try (JsonReader jr = Json.createReader(new StringReader(coordinatesParam))) {
                coordinatesJO = jr.readObject();
                JsonObject latlng = coordinatesJO.getJsonObject("coordinates");

                longitude = Double.parseDouble(latlng.getString("longitude"));
                latitude = Double.parseDouble(latlng.getString("latitude"));
                System.out.println("lat = " + latitude + "\nlon = " + longitude);
            }
        }
        
        System.out.println("vehParam " + vehicleParam +" -> "+ coordinatesParam);
        
        if (vehicleParam != null && coordinatesParam != null) {
            JsonObject coordinates;
            System.out.println("line: " + vehicleParam);

            try (JsonReader jr = Json.createReader(new StringReader(coordinatesParam))) {
                coordinatesJO = jr.readObject();
                coordinates = coordinatesJO.getJsonObject("coordinates");
                System.out.println(coordinates);
                System.out.println(coordinatesJO);
            }

            List<String> locationData = new ArrayList<>();
            locationData.add(coordinates.getString("latitude"));
            locationData.add(coordinates.getString("longitude"));
            locationData.add(coordinates.getString("accuracy"));
            locationData.add(coordinates.getString("speed"));

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

        }

    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            System.out.println("[GET]");
            System.out.println("REQUEST -> " + request.toString());
            System.out.println("RESPONSE -> " + response.toString());
            response.setContentType("text/json");

            Map<String, Object> jwConfig = new HashMap<>();
            jwConfig.put(JsonGenerator.PRETTY_PRINTING, true);
            JsonWriter jw = Json.createWriterFactory(jwConfig).createWriter(response.getOutputStream());
            System.out.println("req URI: " + request.getRequestURI());
            switch (request.getRequestURI()) {
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
                    
                case "/api/updatePoi":                 
                    query = "SELECT * FROM `poi`";
                    rs = mapper.executeQuery(query);

                    JsonArrayBuilder poisJAB = Json.createArrayBuilder();
                    while (rs.next()) {
                        JsonObjectBuilder poiJOB = Json.createObjectBuilder();
                        for (int i = 1; i < 7; i++) {
                            poiJOB.add(rs.getMetaData().getColumnName(i), rs.getString(i));
                        }
                        poisJAB.add(poiJOB);
                    }                    
                    JsonObjectBuilder poisJOB = Json.createObjectBuilder();
                    poisJOB.add("pois", poisJAB);
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
