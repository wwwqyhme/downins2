package me.qyh.instd4j.parser;

import java.util.List;

public class Page<T> {

    private final List<T> datas;
    private final PageInfo pageInfo;

    public Page(List<T> datas, PageInfo pageInfo) {
        this.datas = datas;
        this.pageInfo = pageInfo;
    }

    public List<T> getDatas() {
        return datas;
    }

    public PageInfo getPageInfo() {
        return pageInfo;
    }
}
