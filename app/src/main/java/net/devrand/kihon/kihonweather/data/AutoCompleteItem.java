package net.devrand.kihon.kihonweather.data;

/**
 * Created by tstratto on 1/22/2016.
 */
public class AutoCompleteItem {
    public String name;
    public String type;
    public String c;
    public String zmw;
    public String tz;
    public String tzs;
    public String l;
    public String lat;
    public String lon;

    public String toString() {
        return name + " : " + l;
    }
}
