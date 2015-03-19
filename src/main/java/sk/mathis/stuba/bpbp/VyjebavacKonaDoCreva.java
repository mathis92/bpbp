/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sk.mathis.stuba.bpbp;

import java.sql.Connection;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import sk.mathis.stuba.webportal.PortalServlet;
import sk.mathis.stuba.webportal.ResourceServlet;

/**
 *
 * @author martinhudec
 */
public class VyjebavacKonaDoCreva{
    private Connection connection;
    private Mapper mapper;
    
    public void startServer() throws InterruptedException, Exception {
        mapper = new Mapper();
        Server webserver = new Server(80);
        ServletContextHandler sch = new ServletContextHandler();
        sch.addServlet(new ServletHolder(new PortalServlet()), "/*");
        sch.addServlet(new ServletHolder(new MainServletVajca(mapper)), "/api/*");
        sch.addServlet(new ServletHolder(new ResourceServlet()), "/resource/*");
        webserver.setHandler(sch);
        webserver.start();
        webserver.join();        
    }

    public Connection getConnection() {
        return connection;
    }

    
    
    
}
