package net.devrand.kihon.kihonweather.data;

/**
 * Created by tstratto on 12/29/2015.
 */
public class WeatherStation {
    public String city;
    public String state;
    public String country;
    public String icao;
    public String neighborhood;
    public String id;
    //FIXME should be double, but some results give empty string ""
    // and GSON throws an exception when it tries to parse
    // (airport station KNTD has this error)
    public String lat;
    public String lon;
}
