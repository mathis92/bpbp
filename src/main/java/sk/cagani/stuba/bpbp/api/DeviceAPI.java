package sk.cagani.stuba.bpbp.api;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonWriter;
import javax.json.stream.JsonGenerator;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.hibernate.Session;
import org.slf4j.LoggerFactory;
import sk.cagani.stuba.bpbp.serverApp.DatabaseConnector;
import stuba.bpbphibernatemapper.GtfsStops;

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
        switch (request.getRequestURI()) {
            case "/api/allStops": {

                logger.debug("in api call allStops " + request.getRequestURI() + " " + request.getRequestURL());

            }

            default: {
                response.getOutputStream().write(("invalid call " + request.getRequestURI()).getBytes());
            }
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

            default: {
                response.getOutputStream().write(("invalid call " + request.getRequestURI()).getBytes());
            }
        }
        response.setStatus(HttpServletResponse.SC_OK);
    }
}
