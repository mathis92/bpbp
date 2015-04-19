/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sk.cagani.stuba.bpbp.serverApp;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import sk.cagani.stuba.bpbp.api.DeviceAPI;
import sk.cagani.stuba.bpbp.api.VehicleAPI;
import sk.cagani.stuba.bpbp.utilities.Utils;
import sk.cagani.stuba.bpbp.webportal.ResourceServlet;

/**
 *
 * @author martinhudec
 */

/* HttpsHello.java
 - Copyright (c) 2014, HerongYang.com, All Rights Reserved.
 */
public class AppInit {

    public static void main(String[] args) throws Exception {       
        DatabaseConnector dc = new DatabaseConnector();
        
        new Thread(new Utils()).start();
       // dc.writeStopsToFile();
        new AppInit().startServlets();
        
       
    }
    
    private void startServlets() throws InterruptedException, Exception {
        Server webserver = new Server(80);
        ServletContextHandler sch = new ServletContextHandler();
        sch.addServlet(new ServletHolder(new VehicleAPI()), "/api/vehicle/*");
        sch.addServlet(new ServletHolder(new DeviceAPI()), "/api/device/*");
        sch.addServlet(new ServletHolder(new ResourceServlet()), "/*");
        webserver.setHandler(sch);
        webserver.start();
        webserver.join();
    }
}
