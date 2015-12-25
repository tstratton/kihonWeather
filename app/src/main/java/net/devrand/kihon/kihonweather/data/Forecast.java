package net.devrand.kihon.kihonweather.data;

import java.util.List;

/**
 * Created by tstratto on 12/5/2015.
 */
public class Forecast {
    public TextForecast txt_forecast;
    public String date;

    public class TextForecast {
        public List<TextForecastDay> forecastday;
    }

    public class TextForecastDay {
        public String title;
        public String fcttext;
        public String pop;
    }
}
