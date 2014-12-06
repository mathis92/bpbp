package sk.mathis.stuba.bpbp;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
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

    public MainServletVajca() {

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

        String coordinates = request.getParameter("coordinates");
        System.out.println(coordinates);

        System.out.println(request.getRequestURI());

        if (coordinates != null) {
            try (JsonReader jr = Json.createReader(new StringReader(coordinates))) {
                coordinatesJO = jr.readObject();
                JsonObject latlng = coordinatesJO.getJsonObject("coordinates");

                longitude = latlng.getJsonNumber("longitude").doubleValue();
                latitude = latlng.getJsonNumber("latitude").doubleValue();
                System.out.println("lat = " + latitude + "\nlon = " + longitude);
            }
        }
        /*
         System.out.println("input stream -> " +is.toString());
         JsonReaderFactory jrf = Json.createReaderFactory(configMap);

         try (JsonReader jr = jrf.createReader(new InputStreamReader(is))) {
         System.out.println("idem vypisovat");

         JsonObject jo = jr.readObject();
         ArrayList<String> config = new ArrayList<>();
         config.add(jo.getString("latitude"));
         System.out.println(jo.getString("latitude"));
         config.add(jo.getString("longitude"));
         System.out.println(config.size() + " " + config);
         jr.close();

            
         }
         */
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        System.out.println("[GET]");
        System.out.println("REQUEST -> " + request.toString());
        System.out.println("RESPONSE -> " + response.toString());
        response.setContentType("text/json");
        switch (request.getRequestURI()) {
            case "/vehicle/39":
                System.out.println("som v spoji 39 " + request.getRequestURI() + " " + request.getRequestURL());
                Map<String, Object> jwConfig = new HashMap<>();

                jwConfig.put(JsonGenerator.PRETTY_PRINTING, true);
                JsonWriter jw = Json.createWriterFactory(jwConfig).createWriter(response.getOutputStream());
                jw.writeObject(coordinatesJO);
                break;

        }
        response.setStatus(HttpServletResponse.SC_OK);

    }
}
