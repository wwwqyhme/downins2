package me.qyh.instd4j.parser;

public class PageInfo {
    private final boolean hasNextPage;
    private final String endCursor;

    public PageInfo(boolean hasNextPage, String endCursor) {
        this.hasNextPage = hasNextPage;
        this.endCursor = endCursor;
    }

    public boolean isHasNextPage() {
        return hasNextPage;
    }

    public String getEndCursor() {
        return endCursor;
    }
}
