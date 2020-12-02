package me.qyh.instd4j.parser;

import java.util.List;

public class Post {
    private final String shortcode;
    private final String id;
    private final List<Link> thumbnails;
    private final List<Link> links;
    private final String type;

    public Post(String type, String shortcode, String id, List<Link> thumbnails, List<Link> links) {
        this.type = type;
        this.shortcode = shortcode;
        this.id = id;
        this.thumbnails = thumbnails;
        this.links = links;
    }

    public Post(Post source) {
        this.type = source.type;
        this.shortcode = source.shortcode;
        this.id = source.id;
        this.thumbnails = source.thumbnails;
        this.links = source.links;
    }

    public String getShortcode() {
        return shortcode;
    }

    public String getId() {
        return id;
    }

    public List<Link> getLinks() {
        return links;
    }

    public List<Link> getThumbnails() {
        return thumbnails;
    }

    public String getType() {
        return type;
    }

    public boolean isSidecar() {
        return InsParser.GRAPH_SIDECAR.equals(type);
    }

    public boolean isVideo() {
        return InsParser.GRAPH_VIDEO.equals(type);
    }

    public boolean isImage() {
        return InsParser.GRAPH_IMAGE.equals(type);
    }

    @Override
    public String toString() {
        return "Post{" +
                "shortcode='" + shortcode + '\'' +
                ", id='" + id + '\'' +
                ", thumbnails=" + thumbnails +
                ", links=" + links +
                ", type='" + type + '\'' +
                '}';
    }
}
