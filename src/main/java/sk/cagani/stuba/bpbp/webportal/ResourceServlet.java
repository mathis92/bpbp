package sk.cagani.stuba.bpbp.webportal;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonWriter;
import javax.json.stream.JsonGenerator;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import sk.cagani.stuba.bpbp.serverApp.DatabaseConnector;
import stuba.bpbpdatabasemapper.GtfsRoutes;
import stuba.bpbpdatabasemapper.Poi;
import stuba.bpbpdatabasemapper.PoisInRoutes;

public class ResourceServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        System.out.println("[ResourceServlet POST]  url: " + request.getRequestURI());
        response.setContentType("text/json");
        Map<String, Object> jwConfig = new HashMap<>();
        jwConfig.put(JsonGenerator.PRETTY_PRINTING, true);
        JsonWriter jw = Json.createWriterFactory(jwConfig).createWriter(response.getOutputStream());

        switch (request.getRequestURI()) {
            case "/savePoi":
                String poiTitle = request.getParameter("poiTitle");
                String videoTitle = request.getParameter("videoTitle");
                String routeNum = request.getParameter("routeNumber");
                Double lat = Double.parseDouble(request.getParameter("lat"));
                Double lon = Double.parseDouble(request.getParameter("lon"));

                System.out.println(poiTitle + " " + videoTitle + " " + routeNum + " " + lat + " " + lon + " " + poiTitle.isEmpty() + " " + lat.isNaN());

                if (!(poiTitle.isEmpty() && videoTitle.isEmpty() && routeNum.isEmpty())) {
                    Session sessionSavePoi = DatabaseConnector.getSession();
                    Transaction transactionSavePoi = null;
                    try {
                        transactionSavePoi = sessionSavePoi.beginTransaction();

                        Poi poi = new Poi(lat, lon, 20, poiTitle, videoTitle);
                        sessionSavePoi.save(poi);
                        routeNum = routeNum.replace(" ", "");
                        String[] routes = routeNum.split(",");
                        for (String splitRoute : routes) {
                            GtfsRoutes route = (GtfsRoutes) sessionSavePoi.createCriteria(GtfsRoutes.class).add(Restrictions.eq("shortName", splitRoute)).uniqueResult();
                            if (route != null) {
                                PoisInRoutes poisInRoutes = new PoisInRoutes(route, poi);
                                sessionSavePoi.save(poisInRoutes);
                            }
                        }
                        transactionSavePoi.commit();
                    } catch (HibernateException e) {
                        if (transactionSavePoi != null) {
                            transactionSavePoi.rollback();
                            throw e;
                        }
                    } finally {
                        sessionSavePoi.close();
                    }
                }
                break;
            case "/getPoi":
                Session sessionGetPoi = DatabaseConnector.getSession();
                Transaction transactionGetPoi = null;
                try {
                    transactionGetPoi = sessionGetPoi.beginTransaction();
                    List<Poi> poiList = sessionGetPoi.createCriteria(Poi.class).list();

                    JsonArrayBuilder poiJAB = Json.createArrayBuilder();

                    for (Poi poi : poiList) {
                        List<PoisInRoutes> poisInRouteses = sessionGetPoi.createCriteria(PoisInRoutes.class)
                                .createAlias("gtfsRoutes", "routes")
                                .add(Restrictions.eq("poi", poi)).addOrder(Order.asc("routes.shortName"))
                                .list();

                        JsonObjectBuilder poiJOB = Json.createObjectBuilder();
                        poiJOB.add("id", poi.getId());
                        poiJOB.add("title", poi.getTitle());
                        poiJOB.add("lat", poi.getLat());
                        poiJOB.add("lon", poi.getLon());
                        poiJOB.add("radius", poi.getRadius());
                        poiJOB.add("filePath", poi.getFilePath());
                        String routes = null;
                        for (PoisInRoutes pir : poisInRouteses) {
                            if (routes == null) {
                                routes = pir.getGtfsRoutes().getShortName();
                            } else {
                                routes += ", " + pir.getGtfsRoutes().getShortName();
                            }
                        }
                        if (routes == null) {
                            routes = "";
                        }
                        poiJOB.add("routes", routes);
                        poiJAB.add(poiJOB);
                    }

                    JsonObjectBuilder poisJOB = Json.createObjectBuilder();

                    poisJOB.add("poiList", poiJAB);
                    JsonObject poisJO = poisJOB.build();

                    System.out.println(poisJO.toString());
                    jw.writeObject(poisJO);

                } catch (HibernateException e) {
                    if (transactionGetPoi != null) {
                        transactionGetPoi.rollback();
                        throw e;
                    }
                } finally {
                    sessionGetPoi.close();
                }
                break;
        }
        response.setStatus(HttpServletResponse.SC_OK);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String requestUrl = request.getRequestURI();
        if (requestUrl.equals("/")) {
            requestUrl = "/index.html";
        }
        String requestFileName = "/webportal" + requestUrl;
        InputStream is = getClass().getResourceAsStream(requestFileName);
        if (is == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().println("404 Not Found");
        } else {
            response.setStatus(HttpServletResponse.SC_OK);
            String fileNameLc = requestFileName.toLowerCase();
            if (fileNameLc.endsWith(".js")) {
                response.setContentType("text/javascript");
            } else if (fileNameLc.endsWith(".css")) {
                response.setContentType("text/css");
            } else if (fileNameLc.endsWith(".png")) {
                response.setContentType("image/png");
            } else if (fileNameLc.endsWith(".svg")) {
                response.setContentType("image/svg");
            } else if (fileNameLc.endsWith(".eot")) {
                response.setContentType("font/opentype");
            } else if (fileNameLc.endsWith(".ttf")) {
                response.setContentType("application/x-font-ttf");
            } else if (fileNameLc.endsWith(".woff")) {
                response.setContentType("application/x-font-woff");
            } else if (fileNameLc.endsWith(".html")) {
                response.setContentType("text/html");
            } else {
                throw new ServletException("Unknown file type");
            }
            byte[] buffer = new byte[1024];
            while (is.available() > 0) {
                int rd = is.read(buffer, 0, buffer.length);
                response.getOutputStream().write(buffer, 0, rd);
            }
        }
    }
}
