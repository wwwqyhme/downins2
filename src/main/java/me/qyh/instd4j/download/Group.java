package me.qyh.instd4j.download;

import java.util.Collections;
import java.util.List;

public class Group {

    private final List<GroupItem> items;

    public Group(List<GroupItem> items) {
        this.items = items;
    }

    public Group(GroupItem... items) {
        this.items = List.of(items);
    }


    public List<GroupItem> getItems() {
        return Collections.unmodifiableList(items);
    }
}
