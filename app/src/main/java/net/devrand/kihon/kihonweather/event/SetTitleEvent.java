package net.devrand.kihon.kihonweather.event;

/**
 * Created by tstratto on 5/1/2017.
 */
public class SetTitleEvent {
    public String title;
    public String subTitle;

    public SetTitleEvent(String title, String subTitle) {
        this.title = title;
        this.subTitle = subTitle;
    }

    public static SetTitleEvent createSubTitleEvent(String subTitle) {
        return new SetTitleEvent(null, subTitle);
    }
}
