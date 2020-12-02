package me.qyh.instd4j.parser;

import java.io.File;

public class Link {
    private final String url;
    private final boolean video;
    private final String extension;

    public Link(String url, boolean video) {
        this.url = url;
        this.video = video;
        this.extension = getFileExtension(url);
    }

    public String getUrl() {
        return url;
    }

    public boolean isVideo() {
        return video;
    }

    public String getExtension() {
        return extension;
    }

    @Override
    public String toString() {
        return "Link{" +
                "url='" + url + '\'' +
                ", video=" + video +
                '}';
    }

    private static String getFileExtension(String path) {
        String name = new File(path).getName();
        String ext = name;
        int index = name.lastIndexOf('?');
        if (index > -1) {
            ext = ext.substring(0, index);
        }
        index = ext.lastIndexOf('.');
        if (index > -1) {
            ext = ext.substring(index + 1);
        }
        index = ext.indexOf('?');
        if (index > -1) {
            ext = ext.substring(0, index);
        }
        return ext;
    }
}
