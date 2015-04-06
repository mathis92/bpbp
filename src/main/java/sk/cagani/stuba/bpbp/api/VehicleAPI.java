package sk.cagani.stuba.bpbp.api;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.slf4j.LoggerFactory;
import sk.cagani.stuba.bpbp.serverApp.DatabaseConnector;
import sk.cagani.stuba.bpbp.utilities.Utils;
import stuba.bpbphibernatemapper.GtfsAgencies;
import stuba.bpbphibernatemapper.GtfsStopTimes;
import stuba.bpbphibernatemapper.GtfsStops;
import stuba.bpbphibernatemapper.GtfsTrips;
import stuba.bpbphibernatemapper.GtfsTripsId;
import stuba.bpbphibernatemapper.Poi;
import stuba.bpbphibernatemapper.PoisInRoutes;
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

    //private Session session;
    private String agencyId;

    public VehicleAPI() {
        agencyId = ((GtfsAgencies) DatabaseConnector.getSession().createCriteria(GtfsAgencies.class).list().get(0)).getId();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Session session;
        Transaction transaction;
        System.out.println("[POST]");

        response.setContentType("text/json");
        Map<String, Boolean> jwConfig = new HashMap<>();
        jwConfig.put(JsonGenerator.PRETTY_PRINTING, Boolean.TRUE);
        JsonWriter jw = Json.createWriterFactory(jwConfig).createWriter(response.getOutputStream());

        switch (request.getRequestURI()) {
            case "/api/vehicle/init":

                Float lat = Float.parseFloat(request.getParameter("lat"));
                Float lon = Float.parseFloat(request.getParameter("lon"));

                session = DatabaseConnector.getSession();
                transaction = session.beginTransaction();

                List<GtfsStops> gtfsStops = session.createCriteria(GtfsStops.class)
                        .add(Restrictions.between("lat", lat - 0.0005, lat + 0.0005))
                        .add(Restrictions.between("lon", lon - 0.0005, lon + 0.0005)).list();

                JsonArrayBuilder tripsJAB = Json.createArrayBuilder();
                for (GtfsStops stop : gtfsStops) {
                    for (GtfsStopTimes stopTime : (Set<GtfsStopTimes>) stop.getGtfsStopTimeses()) {
                        int secsFromMidnight = 44400;//Utils.getSecondsFromMidnight();
                        if (stopTime.getDepartureTime() > secsFromMidnight - 300 && stopTime.getDepartureTime() < secsFromMidnight + 300) {
                            if (stopTime.getGtfsTrips().getServiceIdId().equals("Prac.dny_0"/*Utils.getActualServiceId()*/)) {
                                if (!stop.getName().equals(stopTime.getGtfsTrips().getTripHeadsign())) {
                                    JsonObjectBuilder tripJOB = Json.createObjectBuilder();
                                    tripJOB.add("routeName", stopTime.getGtfsTrips().getGtfsRoutes().getShortName());
                                    tripJOB.add("tripId", stopTime.getGtfsTrips().getId().getId());
                                    tripJOB.add("departureTime", stopTime.getDepartureTime());
                                    tripJOB.add("headSign", stopTime.getGtfsTrips().getTripHeadsign());

                                    tripsJAB.add(tripJOB);
                                }
                            }
                        }
                    }
                }
                transaction.commit();
                session.close();

                JsonObjectBuilder possibleTripJOB = Json.createObjectBuilder();
                possibleTripJOB.add("possibleTripList", tripsJAB);
                JsonObject possibleTripJO = possibleTripJOB.build();
                System.out.println(possibleTripJO.toString());
                jw.writeObject(possibleTripJO);
                break;
            case "/api/vehicle/updateLocation":
                session = DatabaseConnector.getSession();
                transaction = session.beginTransaction();
                //List<TripPositions> tripPositionList = session.createCriteria(TripPositions.class).add(Restrictions.eq("gtfsTrips", new GtfsTrips())).list();
                TripPositions tripPosition = (TripPositions) session.get(TripPositions.class, new GtfsTripsId(agencyId, request.getParameter("gtfs_trip_id")));

                if (tripPosition == null) {
                    tripPosition.setLat(Float.parseFloat(request.getParameter("lat")));
                    tripPosition.setLon(Float.parseFloat(request.getParameter("lon")));
                    tripPosition.setDelay(Integer.parseInt(request.getParameter("delay")));
                    tripPosition.setSpeed(Double.parseDouble(request.getParameter("spd")));
                    tripPosition.setAccuracy(Double.parseDouble(request.getParameter("acc")));
                } else {
                    tripPosition = new TripPositions(
                            new GtfsTrips(
                                    new GtfsTripsId(agencyId, request.getParameter("gtfs_trip_id"))),
                            Double.parseDouble(request.getParameter("lat")),
                            Double.parseDouble(request.getParameter("lon")),
                            Integer.parseInt(request.getParameter("delay")),
                            Double.parseDouble(request.getParameter("spd")),
                            Double.parseDouble(request.getParameter("acc")));

                }
                session.saveOrUpdate(tripPosition);
                transaction.commit();
                session.close();

                //v buducnosti tu zrob to cekovanie pred zastavkou a potom ak hej, tak treba tie linky najblizsie poslat abo co
                break;
            case "/api/vehicle/getStopsAndPoi":
                System.out.println("tripId: " + request.getParameter("gtfs_trip_id"));

                Session session1 = DatabaseConnector.getSession();
                Transaction tx1 = session1.beginTransaction();
                /*
                 get all POI
                 */
                GtfsTrips gtfsTrip = (GtfsTrips) session1.get(GtfsTrips.class, new GtfsTripsId(agencyId, request.getParameter("gtfs_trip_id")));

                JsonArrayBuilder poiJAB = Json.createArrayBuilder();
                for (PoisInRoutes pir : (Set<PoisInRoutes>) gtfsTrip.getGtfsRoutes().getPoisInRouteses()) {
                    JsonObjectBuilder poiJOB = Json.createObjectBuilder();
                    poiJOB.add("title", pir.getPoi().getTitle());
                    poiJOB.add("lat", pir.getPoi().getLat());
                    poiJOB.add("lon", pir.getPoi().getLon());
                    poiJOB.add("radius", pir.getPoi().getRadius());
                    poiJOB.add("filePath", pir.getPoi().getFilePath());

                    poiJAB.add(poiJOB);
                }
                /*
                 get all stops
                 */
                JsonArrayBuilder stopsJAB = Json.createArrayBuilder();
                for (GtfsStopTimes gst : (Set<GtfsStopTimes>) gtfsTrip.getGtfsStopTimeses()) {
                    JsonObjectBuilder stopsJOB = Json.createObjectBuilder();
                    stopsJOB.add("name", gst.getGtfsStops().getName());
                    stopsJOB.add("lat", gst.getGtfsStops().getLat());
                    stopsJOB.add("lon", gst.getGtfsStops().getLon());
                    stopsJOB.add("zoneId", gst.getGtfsStops().getZoneId());
                    stopsJOB.add("arrivalTime", gst.getArrivalTime());
                    stopsJOB.add("isOnRequest", gst.getPickupType().equals(3) ? "true" : "false");
                    stopsJOB.add("stopSequence", gst.getStopSequence());

                    stopsJAB.add(stopsJOB);
                }

                tx1.commit();
                session1.close();

                JsonObjectBuilder tripInfoJOB = Json.createObjectBuilder();
                tripInfoJOB.add("poiList", poiJAB);
                tripInfoJOB.add("stopsList", stopsJAB);

                JsonObject tripInfoJO = tripInfoJOB.build();

                System.out.println(tripInfoJO.toString());

                jw.writeObject(tripInfoJO);
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
                Session session = DatabaseConnector.getSession();
                List<Poi> poiList = session.createCriteria(Poi.class).list();
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
            default: {
                response.getOutputStream().write(("invalid call " + request.getRequestURI()).getBytes());
            }
        }
        response.setStatus(HttpServletResponse.SC_OK);
    }
}
