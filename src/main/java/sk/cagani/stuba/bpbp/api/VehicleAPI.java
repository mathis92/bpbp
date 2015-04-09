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
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.slf4j.LoggerFactory;
import sk.cagani.stuba.bpbp.serverApp.DatabaseConnector;
import sk.cagani.stuba.bpbp.utilities.Utils;
import stuba.bpbpdatabasemapper.GtfsAgencies;
import stuba.bpbpdatabasemapper.GtfsStopTimes;
import stuba.bpbpdatabasemapper.GtfsStops;
import stuba.bpbpdatabasemapper.GtfsTrips;
import stuba.bpbpdatabasemapper.GtfsTripsId;
import stuba.bpbpdatabasemapper.Poi;
import stuba.bpbpdatabasemapper.PoisInRoutes;
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
public class VehicleAPI extends HttpServlet {

    private final org.slf4j.Logger logger = LoggerFactory.getLogger(VehicleAPI.class);

    //private Session session;
    private String agencyId;

    public VehicleAPI() {
        Session sessionAgencyId = DatabaseConnector.getSession();
        Transaction sessionAgencyIdTransaction = sessionAgencyId.beginTransaction();
        agencyId = ((GtfsAgencies) sessionAgencyId.createCriteria(GtfsAgencies.class).list().get(0)).getId();
        sessionAgencyIdTransaction.commit();
        sessionAgencyId.close();
        System.out.println("\n\nagencyId: " + agencyId + "\n");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        System.out.println("\n\n" + DatabaseConnector.getStatistics() + "\n\n");
        //      System.out.print("[POST]  ");
        response.setContentType("text/json");
        Map<String, Boolean> jwConfig = new HashMap<>();
        jwConfig.put(JsonGenerator.PRETTY_PRINTING, Boolean.TRUE);
        JsonWriter jw = Json.createWriterFactory(jwConfig).createWriter(response.getOutputStream());

        switch (request.getRequestURI()) {
            case "/api/vehicle/init":
                Session sessionInit = DatabaseConnector.getSession();
                Transaction transactionInit = sessionInit.beginTransaction();

                Double lat = Double.parseDouble(request.getParameter("lat"));
                Double lon = Double.parseDouble(request.getParameter("lon"));

                List<GtfsStops> gtfsStops = sessionInit.createCriteria(GtfsStops.class)
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
                                    tripJOB.add("routeType", stopTime.getGtfsTrips().getGtfsRoutes().getType());
                                    tripJOB.add("tripId", stopTime.getGtfsTrips().getId().getId());
                                    tripJOB.add("departureTime", stopTime.getDepartureTime());
                                    tripJOB.add("headSign", stopTime.getGtfsTrips().getTripHeadsign());

                                    tripsJAB.add(tripJOB);
                                }
                            }
                        }
                    }
                }
                transactionInit.commit();
                sessionInit.close();

                JsonObjectBuilder possibleTripJOB = Json.createObjectBuilder();
                possibleTripJOB.add("possibleTripList", tripsJAB);
                JsonObject possibleTripJO = possibleTripJOB.build();
                System.out.println(possibleTripJO.toString());
                jw.writeObject(possibleTripJO);
                break;
            case "/api/vehicle/updateLocation":
             //   System.out.println("trip id:  " + (request.getParameter("trip_id") == null ? "empty" : request.getParameter("trip_id")));

                Session sessionUpdateLocation = DatabaseConnector.getSession();

                Transaction transaction = null;

                Transaction transactionUpdateLocation = null;
                try {

                    transactionUpdateLocation = sessionUpdateLocation.beginTransaction();
                    GtfsTrips trip = (GtfsTrips) sessionUpdateLocation.get(GtfsTrips.class, new GtfsTripsId(agencyId, request.getParameter("trip_id")));
             //   System.out.println("trip: " + trip.getId().getId());
/*
                     TripPositions tripPosition = null;
                     List<TripPositions> tripPositionList = sessionUpdateLocation.createCriteria(TripPositions.class).add(Restrictions.eq("gtfsTrips", trip)).list();
                     //new GtfsTrips(new GtfsTripsId(agencyId, request.getParameter("trip_id")))
                     if (!tripPositionList.isEmpty()) {
                     tripPosition = tripPositionList.get(0);
                     }
                     */
                    TripPositions tripPosition = (TripPositions) sessionUpdateLocation.createCriteria(TripPositions.class).add(Restrictions.eq("gtfsTrips", trip)).uniqueResult();
              //  System.out.println("tripPosition: " + tripPosition);

                    /*  System.out.println(Double.parseDouble(request.getParameter("lat")) + " "
                     + Double.parseDouble(request.getParameter("lon")) + " "
                     + Integer.parseInt(request.getParameter("delay")) + " "
                     + Double.parseDouble(request.getParameter("spd")) + " "
                     + Double.parseDouble(request.getParameter("acc")));
                     */
                    if (tripPosition == null) {
                        tripPosition = new TripPositions(
                                trip,
                                Double.parseDouble(request.getParameter("lat")),
                                Double.parseDouble(request.getParameter("lon")),
                                Double.parseDouble(request.getParameter("spd")),
                                Double.parseDouble(request.getParameter("acc")),
                                Integer.parseInt(request.getParameter("delay")),
                                "a");
                    } else {
                        tripPosition.setLat(Double.parseDouble(request.getParameter("lat")));
                        tripPosition.setLon(Double.parseDouble(request.getParameter("lon")));
                        tripPosition.setDelay(Integer.parseInt(request.getParameter("delay")));
                        tripPosition.setSpeed(Double.parseDouble(request.getParameter("spd")));
                        tripPosition.setAccuracy(Double.parseDouble(request.getParameter("acc")));
                        tripPosition.setModifiedAt(null);
                    }

                    sessionUpdateLocation.saveOrUpdate(tripPosition);

                    transactionUpdateLocation.commit();

                } catch (HibernateException | NumberFormatException e) {
                    if (transactionUpdateLocation != null) {
                        transactionUpdateLocation.rollback();
                        throw e;
                    }
                } finally {
                    sessionUpdateLocation.close();
                }
                //sessionUpdateLocation.close();

                //v buducnosti tu zrob to cekovanie pred zastavkou a potom ak hej, tak treba tie linky najblizsie poslat abo co
                break;
            case "/api/vehicle/getStopsAndPoi":
                System.out.println("tripId: " + request.getParameter("trip_id"));

                Session session1 = DatabaseConnector.getSession();
                Transaction tx1 = session1.beginTransaction();
                /*
                 get all POI
                 */
                GtfsTrips gtfsTrip = (GtfsTrips) session1.get(GtfsTrips.class, new GtfsTripsId(agencyId, request.getParameter("trip_id")));

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
