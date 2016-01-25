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

    public boolean afterSunset(int hour, int minute) {
        if (hour > Integer.parseInt(sunset.hour, 10)) {
            return true;
        }
        if (hour < Integer.parseInt(sunset.hour, 10)) {
            return false;
        }
        return (Integer.parseInt(sunset.minute, 10) > minute);
    }
}
