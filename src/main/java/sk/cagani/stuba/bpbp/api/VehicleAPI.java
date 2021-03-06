package sk.cagani.stuba.bpbp.api;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonWriter;
import javax.json.stream.JsonGenerator;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
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
import stuba.bpbpdatabasemapper.StopRealTimesHistory;
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
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        System.out.println("[POST] request length: " + request.getContentLength() + " B   " + Long.parseLong(request.getHeader("Content-Length")) + " B\n" + request.toString());
        //   System.out.println("\n\n" + DatabaseConnector.getStatistics() + "\n\n");
        //      System.out.print("[POST]  ");
        response.setContentType("text/json");
        Map<String, Boolean> jwConfig = new HashMap<>();
        jwConfig.put(JsonGenerator.PRETTY_PRINTING, Boolean.TRUE);
        JsonWriter jw = Json.createWriterFactory(jwConfig).createWriter(response.getOutputStream());

        switch (request.getRequestURI()) {
            case "/api/vehicle/init":
                System.out.println("[Init] position: " + request.getParameter("lat") + "  " + request.getParameter("lon"));
                Session sessionInit = DatabaseConnector.getSession();
                Transaction transactionInit = null;
                try {
                    transactionInit = sessionInit.beginTransaction();

                    Double lat = Double.parseDouble(request.getParameter("lat"));
                    Double lon = Double.parseDouble(request.getParameter("lon"));
                    Integer time = Integer.parseInt(request.getParameter("time"));
                    String routeNumber = request.getParameter("routeNum");

                    List<GtfsStops> gtfsStops = sessionInit.createCriteria(GtfsStops.class)
                            .add(Restrictions.between("lat", lat - 0.0025, lat + 0.0025))
                            .add(Restrictions.between("lon", lon - 0.0025, lon + 0.0025))
                            .list();

                    JsonArrayBuilder tripsJAB = Json.createArrayBuilder();
                    for (GtfsStops stop : gtfsStops) {
                        for (GtfsStopTimes stopTime : (List<GtfsStopTimes>) sessionInit
                                .createCriteria(GtfsStopTimes.class)
                                .add(Restrictions.eq("gtfsStops", stop))
                                .addOrder(Order.asc("departureTime")).list()) {
                            if (stopTime.getGtfsTrips().getGtfsRoutes().getShortName().equals(routeNumber)) {
                                if (stopTime.getDepartureTime() > time - 1200 && stopTime.getDepartureTime() < time + 1200) {
                                    if (stopTime.getGtfsTrips().getServiceIdId().equals(Utils.getActualServiceId())) {
                                        if (!stop.getName().equals(stopTime.getGtfsTrips().getTripHeadsign())) {
                                            JsonObjectBuilder tripJOB = Json.createObjectBuilder();
                                            tripJOB.add("routeName", stopTime.getGtfsTrips().getGtfsRoutes().getShortName());
                                            tripJOB.add("routeType", stopTime.getGtfsTrips().getGtfsRoutes().getType());
                                            tripJOB.add("tripId", stopTime.getGtfsTrips().getId().getId());
                                            tripJOB.add("departureTime", stopTime.getDepartureTime());
                                            tripJOB.add("headSign", stopTime.getGtfsTrips().getTripHeadsign());
                                            tripJOB.add("foundStop", stop.getName());
                                            tripsJAB.add(tripJOB);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    transactionInit.commit();

                    JsonObjectBuilder possibleTripJOB = Json.createObjectBuilder();
                    possibleTripJOB.add("possibleTripList", tripsJAB);
                    JsonObject possibleTripJO = possibleTripJOB.build();
                    jw.writeObject(possibleTripJO);
                } catch (HibernateException | NumberFormatException e) {
                    if (transactionInit != null) {
                        transactionInit.rollback();
                        throw e;
                    }
                } finally {
                    sessionInit.close();
                }
                break;
            case "/api/vehicle/updateLocation":
                Session sessionUpdateLocation = DatabaseConnector.getSession();
                Transaction transactionUpdateLocation = null;
                try {
                    transactionUpdateLocation = sessionUpdateLocation.beginTransaction();

                    GtfsTrips trip = (GtfsTrips) sessionUpdateLocation
                            .get(GtfsTrips.class, new GtfsTripsId(agencyId, request.getParameter("tripId")));
                    TripPositions tripPosition = (TripPositions) sessionUpdateLocation
                            .createCriteria(TripPositions.class)
                            .add(Restrictions.eq("gtfsTrips", trip)).uniqueResult();

                    if (tripPosition == null) {
                        tripPosition = new TripPositions(
                                trip,
                                Double.parseDouble(request.getParameter("lat")),
                                Double.parseDouble(request.getParameter("lon")),
                                Double.parseDouble(request.getParameter("spd")),
                                Integer.parseInt(request.getParameter("nsn")),
                                Integer.parseInt(request.getParameter("delay")),
                                request.getParameter("state"));
                    } else {
                        tripPosition.setLat(Double.parseDouble(request.getParameter("lat")));
                        tripPosition.setLon(Double.parseDouble(request.getParameter("lon")));
                        tripPosition.setDelay(Integer.parseInt(request.getParameter("delay")));
                        tripPosition.setSpeed(Double.parseDouble(request.getParameter("spd")));
                        tripPosition.setNextStopNumber(Integer.parseInt(request.getParameter("nsn")));
                        tripPosition.setState(request.getParameter("state"));
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
                break;
            case "/api/vehicle/getStopsAndPoi":
                System.out.println("[Get stops and poi] tripId: " + request.getParameter("tripId"));
                Session sessionGetStopsAndPoi = DatabaseConnector.getSession();
                Transaction transactionGetStopsAndPoi = null;
                try {
                    transactionGetStopsAndPoi = sessionGetStopsAndPoi.beginTransaction();
                    /*
                     get all POI
                     */
                    GtfsTrips gtfsTrip = (GtfsTrips) sessionGetStopsAndPoi.get(GtfsTrips.class, new GtfsTripsId(agencyId, request.getParameter("tripId")));

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
                    List<GtfsStopTimes> stopTimesList = sessionGetStopsAndPoi.createCriteria(GtfsStopTimes.class).add(Restrictions.eq("gtfsTrips", gtfsTrip)).addOrder(Order.asc("stopSequence")).list();
                    JsonArrayBuilder stopsJAB = Json.createArrayBuilder();
                    for (GtfsStopTimes gst : stopTimesList) {
                        JsonObjectBuilder stopsJOB = Json.createObjectBuilder();
                        stopsJOB.add("name", gst.getGtfsStops().getName());
                        stopsJOB.add("lat", gst.getGtfsStops().getLat());
                        stopsJOB.add("lon", gst.getGtfsStops().getLon());
                        stopsJOB.add("zoneId", gst.getGtfsStops().getZoneId());
                        stopsJOB.add("arrivalTime", gst.getArrivalTime());
                        stopsJOB.add("stopTimeId", gst.getId());
                        stopsJOB.add("isOnRequest", gst.getPickupType().equals(3) ? "true" : "false");

                        stopsJAB.add(stopsJOB);
                    }

                    transactionGetStopsAndPoi.commit();

                    JsonObjectBuilder tripInfoJOB = Json.createObjectBuilder();
                    tripInfoJOB.add("poiList", poiJAB);
                    tripInfoJOB.add("stopsList", stopsJAB);
                    JsonObject tripInfoJO = tripInfoJOB.build();
                    jw.writeObject(tripInfoJO);
                } catch (HibernateException | NumberFormatException e) {
                    if (transactionGetStopsAndPoi != null) {
                        transactionGetStopsAndPoi.rollback();
                        throw e;
                    }
                } finally {
                    sessionGetStopsAndPoi.close();
                }
                break;
            case "/api/vehicle/realStopTime":
                Session sessionRealStopTime = DatabaseConnector.getSession();
                Transaction transactionRealStopTime = null;
                try {
                    transactionRealStopTime = sessionRealStopTime.beginTransaction();
                    
                    GtfsStopTimes stopTime = (GtfsStopTimes) sessionRealStopTime.get(GtfsStopTimes.class, Integer.parseInt(request.getParameter("stopTimeId")));                    
                    sessionRealStopTime.save(new StopRealTimesHistory(stopTime, Integer.parseInt(request.getParameter("realArrivalTime")), Integer.parseInt(request.getParameter("realDepartureTime"))));
                    transactionRealStopTime.commit();
                } catch (HibernateException | NumberFormatException e) {
                    if (transactionRealStopTime != null) {
                        transactionRealStopTime.rollback();
                        throw e;
                    }
                } finally {
                    sessionRealStopTime.close();
                }
                break;

            case "/api/vehicle/savePoi":
                System.out.println(request.getParameterMap().toString());
                System.out.println("poi " + request.getParameter("poiTitle"));
                System.out.println("\n" + request.toString());

                Session savePoi = DatabaseConnector.getSession();
                Transaction transactionSavePoi = null;
                try {
                    transactionSavePoi = savePoi.beginTransaction();
                    transactionSavePoi.commit();
                } catch (HibernateException | NumberFormatException e) {
                    if (transactionSavePoi != null) {
                        transactionSavePoi.rollback();
                        throw e;
                    }
                } finally {
                    savePoi.close();
                }

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
                List<GtfsStops> stopsList = session.createCriteria(GtfsStops.class
                ).list();
                session.getTransaction()
                        .commit(); //closes transaction
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

                stopsJOB.add(
                        "stops", stopsJAB);
                JsonObject stopsJO = stopsJOB.build();

                System.out.println(stopsJO.toString());
                jw.writeObject(stopsJO);
            }

            case "/api/vehicle/39":
                System.out.println("som v spoji 39 " + request.getRequestURI() + " " + request.getRequestURL());

                //ResultSet rs;
                List<TripPositions> positionList = DatabaseConnector.getSession().createCriteria(TripPositions.class
                ).list();

                JsonArrayBuilder positionJAB = Json.createArrayBuilder();

                for (TripPositions tripPositions : positionList) {
                    JsonObjectBuilder positionJOB = Json.createObjectBuilder();
                    positionJOB.add("lat", tripPositions.getLat());
                    positionJOB.add("lon", tripPositions.getLon());
                    positionJOB.add("spd", tripPositions.getSpeed());

                    positionJAB.add(positionJOB);
                }

                JsonObjectBuilder positionJOB = Json.createObjectBuilder();

                positionJOB.add(
                        "positionList", positionJAB);
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

            case "/api/vehicle/getPoi":

                Session session = DatabaseConnector.getSession();
                Transaction tx = session.beginTransaction();
                List<Poi> poiList = session.createCriteria(Poi.class).list();

                session.getTransaction()
                        .commit();
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

                poisJOB.add(
                        "poiList", poiJAB);
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
