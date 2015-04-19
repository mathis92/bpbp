/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sk.cagani.stuba.bpbp.serverApp;

import com.sun.javafx.scene.control.skin.VirtualFlow;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import stuba.bpbpdatabasemapper.GtfsRoutes;
import stuba.bpbpdatabasemapper.GtfsStops;

/**
 *
 * @author martinhudec
 */
public class StopData {
    GtfsStops stop; 
    List<GtfsRoutes> routeList = new ArrayList<>();

    public List<GtfsRoutes> getRouteList() {
        return routeList;
    }

    public void setRouteList(List<GtfsRoutes> routeList) {
        this.routeList = routeList;
    }

    public GtfsStops getStop() {
        return stop;
    }
}
