package sk.mathis.stuba.bpbp;

import java.io.IOException;
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
        
        
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        System.out.println("[GET]");
        System.out.println("REQUEST -> " + request.toString());
        System.out.println("RESPONSE -> " + response.toString());
        response.setContentType("text/html;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().println("<h1>ššaaaak to klokočuje</h1>");

    }

}
