package me.qyh.instd4j.download;

import java.util.List;

public interface GroupDownloadListener {

    void onComplete(List<GroupItem> successes, List<GroupItem> errors);
}
