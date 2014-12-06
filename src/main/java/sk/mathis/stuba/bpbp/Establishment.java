/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sk.mathis.stuba.bpbp;

import java.sql.Connection;
import java.sql.DriverManager;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

/**
 *
 * @author martinhudec
 */
public class Establishment{
    private Connection connection;
    private Mapper mapper;
    
    public void startServer() throws InterruptedException, Exception {
        mapper = new Mapper();
        Server webserver = new Server(80);
        ServletContextHandler sch = new ServletContextHandler();
        sch.addServlet(new ServletHolder(new MainServletVajca(mapper)), "/*");
        webserver.setHandler(sch);
        webserver.start();
        webserver.join();        
    }

    public Connection getConnection() {
        return connection;
    }

    
    
    
}
