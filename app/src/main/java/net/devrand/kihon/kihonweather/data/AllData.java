package net.devrand.kihon.kihonweather.data;

/**
 * Created by tstratto on 12/5/2015.
 */
public class AllData {
    public Response response;
    public Current current_observation;
    public Forecast forecast;
    public Astronomy moon_phase;
    public Astronomy sun_phase;
    public Hourly hourly_forecast[];

    public boolean hasError() {
        return response == null || response.error != null;
    }

    public Error getError() {
        return response.error;
    }

    public class Response {
        public String version;
        public Error error;
    }

    public class Error {
        public String type;
        public String description;
    }

    public class Hourly {
        public FctTime FCTTIME;
        public int pop;
        public Measure temp;
        public String condition;
    }

}
