package net.devrand.kihon.kihonweather.data;

/**
 * Created by tstratto on 12/5/2015.
 */
public class Current {
    public LocationInfo display_location;

    public String weather;
    public String temperature_string;
    public long observation_epoch;
    public String icon_url;

    public class LocationInfo {
        public String full;
        public String city;
        public String state;
        public String state_name;
        public String zip;
        public String latitude;
        public String longitude;
    }
}
