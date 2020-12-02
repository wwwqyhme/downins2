package me.qyh.instd4j.config;

import java.io.IOException;

public class DefaultQueryHashLoader extends QueryHashLoader {

    private final String channelQueryHash;
    private final String tagPostsQueryHash;
    private final String storyQueryHash;
    private final String userPostsQueryHash;
    private final String highlightStoriesQueryHash;

    public DefaultQueryHashLoader(
            String channelQueryHash, String tagPostsQueryHash, String storyQueryHash, String userPostsQueryHash, String highlightStoriesQueryHash) {
        super(null, null);
        this.channelQueryHash = channelQueryHash;
        this.tagPostsQueryHash = tagPostsQueryHash;
        this.storyQueryHash = storyQueryHash;
        this.userPostsQueryHash = userPostsQueryHash;
        this.highlightStoriesQueryHash = highlightStoriesQueryHash;
    }

    public DefaultQueryHashLoader() {
        this("bc78b344a68ed16dd5d7f264681c4c76",
                "c769cb6c71b24c8a86590b22402fda50",
                "90709b530ea0969f002c86a89b4f2b8d",
                "bfa387b2992c3a52dcbe447467b4b771",
                "d4d88dc1500312af6f937f7b804c68c3");
    }

    @Override
    public String loadChannelQueryHash() {
        return channelQueryHash;
    }

    @Override
    public String loadTagPostsQueryHash() {
        return tagPostsQueryHash;
    }

    @Override
    public String loadStoryQueryHash() {
        return storyQueryHash;
    }

    @Override
    public String loadUserPostsQueryHash() {
        return userPostsQueryHash;
    }

    @Override
    public String loadHighlightStoriesQueryHash() throws IOException {
        return highlightStoriesQueryHash;
    }
}
