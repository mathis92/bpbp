/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sk.mathis.stuba.bpbp;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

/**
 *
 * @author martinhudec
 */
public class Establishment{

    public void startServer() throws InterruptedException, Exception {
        Server webserver = new Server(80);
        ServletContextHandler sch = new ServletContextHandler();
        sch.addServlet(new ServletHolder(new MainServletVajca()), "/*");
        webserver.setHandler(sch);
        webserver.start();
        webserver.join();

    }

}
