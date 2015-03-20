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
   
       HttpServerEstablish establishment = new HttpServerEstablish();

       establishment.startServer();
   
       System.out.println( "Hello World vajca nove!" );       
   }
}

