package net.devrand.kihon.kihonweather.data;

import java.util.List;

/**
 * Created by tstratto on 12/29/2015.
 */
public class Location {
    public String type;
    public String country;
    public String city;
    public String state;
    public double lat;
    public double lon;
    public NearbyStations nearby_weather_stations;

    public class NearbyStations {
        public StationList airport;
        public StationList pws;
    }

    public class StationList {
        public List<WeatherStation> station;
    }
}


