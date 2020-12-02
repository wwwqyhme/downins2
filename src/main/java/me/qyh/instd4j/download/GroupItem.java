package me.qyh.instd4j.download;

import java.nio.file.Path;

public class GroupItem {
    private final String link;
    private final Path dest;
    private final DownloadListener downloadListener;

    public GroupItem(String link, Path dest, DownloadListener downloadListener) {
        this.link = link;
        this.dest = dest;
        this.downloadListener = downloadListener;
    }

    public String getLink() {
        return link;
    }

    public Path getDest() {
        return dest;
    }

    public DownloadListener getDownloadListener() {
        return downloadListener;
    }
}
