package net.devrand.kihon.kihonweather.data;

/**
 * Created by tstratto on 12/5/2015.
 */
public class Astronomy {
    public HourMin sunrise;
    public HourMin sunset;

    public class HourMin {
        public String hour;
        public String minute;
    }

    public String getSunrise() {
        return String.format("%s:%s", sunrise.hour, sunrise.minute);
    }

    public String getSunset() {
        return String.format("%s:%s", sunset.hour, sunset.minute);
    }
}
