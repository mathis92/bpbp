/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sk.cagani.stuba.bpbp.device;

import java.util.Objects;
import stuba.bpbphibernatemapper.GtfsRoutes;
import stuba.bpbphibernatemapper.GtfsStopTimes;
import stuba.bpbphibernatemapper.GtfsTrips;

/**
 *
 * @author martinhudec
 */
public class RouteData {

    private final GtfsRoutes route;
    private final GtfsStopTimes stopTime;
    private final GtfsTrips gtfsTrip;
    

    public RouteData(GtfsRoutes route, GtfsStopTimes stopTime, GtfsTrips gtfsTrips) {
        this.route = route;
        this.stopTime = stopTime;
        this.gtfsTrip = gtfsTrips;
    }

    public GtfsRoutes getRoute() {
        return route;
    }

    public GtfsStopTimes getStopTime() {
        return stopTime;
    }

    public GtfsTrips getGtfsTrip() {
        return gtfsTrip;
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
