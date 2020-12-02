package me.qyh.instd4j.parser;

public class IGTV {

    private final String shortcode;
    private final String id;
    private final double duration;
    private final Link thumbnail;

    public IGTV(String shortcode, String id, double duration, Link thumbnail) {
        this.shortcode = shortcode;
        this.id = id;
        this.duration = duration;
        this.thumbnail = thumbnail;
    }

    public double getDuration() {
        return duration;
    }

    public String getShortcode() {
        return shortcode;
    }

    public String getId() {
        return id;
    }

    public Link getThumbnail() {
        return thumbnail;
    }

    @Override
    public String toString() {
        return "IGTV{" +
                "shortcode='" + shortcode + '\'' +
                ", id='" + id + '\'' +
                ", duration=" + duration +
                ", thumbnail=" + thumbnail +
                '}';
    }
}
