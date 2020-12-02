package me.qyh.instd4j.parser;

public class HighlightStory {
    private final String id;
    private final Link thumbnail;
    private final Link croppedThumbnail;

    public HighlightStory(String id, Link thumbnail, Link croppedThumbnail) {
        this.id = id;
        this.thumbnail = thumbnail;
        this.croppedThumbnail = croppedThumbnail;
    }

    public String getId() {
        return id;
    }

    public Link getThumbnail() {
        return thumbnail;
    }

    public Link getCroppedThumbnail() {
        return croppedThumbnail;
    }

    @Override
    public String toString() {
        return "Story{" +
                "id='" + id + '\'' +
                ", thumbnail=" + thumbnail +
                '}';
    }
}
