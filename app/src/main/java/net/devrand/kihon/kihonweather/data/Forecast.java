package net.devrand.kihon.kihonweather.data;

/**
 * Created by tstratto on 12/5/2015.
 */
public class Forecast {
    TextForecast txt_forecast;
    String date;

    class TextForecast {
        TextForecastDay forecastday[];
    }

    class TextForecastDay {
        String title;
        String fcttext;
        String pop;
    }
}
