package sk.mathis.stuba.bpbp;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
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

        if (coordinatesParam != null) {
            try (JsonReader jr = Json.createReader(new StringReader(coordinatesParam))) {
                coordinatesJO = jr.readObject();
                JsonObject latlng = coordinatesJO.getJsonObject("coordinates");

                longitude = latlng.getJsonNumber("longitude").doubleValue();
                latitude = latlng.getJsonNumber("latitude").doubleValue();
                System.out.println("lat = " + latitude + "\nlon = " + longitude);
            }
        }
        if (vehicleParam != null && coordinatesParam != null) {
            JsonObject coordinates;
            System.out.println("line: " + vehicleParam);

            try (JsonReader jr = Json.createReader(new StringReader(coordinatesParam))) {
                coordinatesJO = jr.readObject();
                coordinates = coordinatesJO.getJsonObject("coordinates");
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

            switch (request.getRequestURI()) {
                case "/vehicle/39":
                    System.out.println("som v spoji 39 " + request.getRequestURI() + " " + request.getRequestURL());

                    jw.writeObject(coordinatesJO);
                    break;
                case "/updatePoi":
                    System.out.println("apdejtujem poika");
                    ResultSet rs;

                    String query = "SELECT * FROM `poi`";
                    rs = mapper.executeQuery(query);

                    JsonObjectBuilder poiJOB = Json.createObjectBuilder();
                    while (rs.next()) {
                        for (int i = 0; i < 5; i++) {
                            poiJOB.add(rs.getMetaData().getColumnName(i), rs.getString(i));
                        }
                    }
                    jw.writeObject(poiJOB.build());
                    break;
            }
            response.setStatus(HttpServletResponse.SC_OK);
        } catch (SQLException ex) {
            Logger.getLogger(MainServletVajca.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
