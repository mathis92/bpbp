/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sk.cagani.stuba.bpbp.device;

import java.util.Objects;
import stuba.bpbphibernatemapper.GtfsRoutes;
import stuba.bpbphibernatemapper.GtfsStopTimes;

/**
 *
 * @author martinhudec
 */
public class RouteData {

    private final GtfsRoutes route;
    private final GtfsStopTimes stopTime;
    

    public RouteData(GtfsRoutes route, GtfsStopTimes stopTime) {
        this.route = route;
        this.stopTime = stopTime;
    }

    public GtfsRoutes getRoute() {
        return route;
    }

    public GtfsStopTimes getStopTime() {
        return stopTime;
    }

 
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final RouteData other = (RouteData) obj;
        if (!Objects.equals(this.route, other.route)) {
            return false;
        }
        if (!Objects.equals(this.stopTime, other.stopTime)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 89 * hash + Objects.hashCode(this.route);
        hash = 89 * hash + Objects.hashCode(this.stopTime);
        return hash;
    }

 
    
}
