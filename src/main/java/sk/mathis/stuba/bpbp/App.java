/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sk.mathis.stuba.bpbp;

/**
 *
 * @author martinhudec
 */

/* HttpsHello.java
 - Copyright (c) 2014, HerongYang.com, All Rights Reserved.
 */
public class App {

    public static void main(String[] args) throws Exception {
        DatabaseConnector dc = new DatabaseConnector();
        
        HttpServerEstablish establishment = new HttpServerEstablish();
        
        System.out.println("Hello World vajca nove!");
        establishment.startServer();

    }
}
