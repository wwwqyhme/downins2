package me.qyh.instd4j.download;

public interface DownloadListener {

    default void onStart(GroupItem item) {
    }

    default void onComplete(GroupItem item, boolean success, Exception ex) {
    }
}
