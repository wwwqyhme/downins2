package me.qyh.instd4j.config;

import me.qyh.instd4j.parser.AuthenticationManager;
import org.apache.http.impl.client.CloseableHttpClient;

import java.io.IOException;

public abstract class QueryHashLoader {

    protected final AuthenticationManager authenticationManager;
    protected final CloseableHttpClient client;

    protected QueryHashLoader(AuthenticationManager authenticationManager, CloseableHttpClient client) {
        this.authenticationManager = authenticationManager;
        this.client = client;
    }

    public abstract String loadChannelQueryHash() throws IOException;

    public abstract String loadTagPostsQueryHash() throws IOException;

    public abstract String loadStoryQueryHash() throws IOException;

    public abstract String loadUserPostsQueryHash() throws IOException;

    public abstract String loadHighlightStoriesQueryHash() throws IOException;

}
