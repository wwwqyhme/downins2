package me.qyh.instd4j.parser;

import me.qyh.instd4j.config.QueryHashLoader;
import me.qyh.instd4j.http.HttpUtils;
import me.qyh.instd4j.http.InvalidStateCodeException;
import me.qyh.instd4j.util.JsonExecutor;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 这个类每次都会去扫描js文件，用于获取所有可用的query_hash(UUID匹配)，然后依次遍历，直到发现可用的query_hash为止
 * <p><b>如果不是极端情况，不推荐使用这个类，因为它太慢太慢，最好的选择是 DefaultQueryHashLoader(你可能得定期更新它的query_hash值)</b></p>
 *
 * @see me.qyh.instd4j.config.DefaultQueryHashLoader
 */
public class QueryHashTool extends QueryHashLoader {

    private static final String URL_PREFIX = "https://www.instagram.com";
    private static final String TAG_URL = "https://www.instagram.com/explore/tags/instagram/";
    private static final String TAG_VARIABLES = "{\"tag_name\":\"instagram\",\"first\":5,\"after\":\"\"}";
    private static final String USER_URL = "https://www.instagram.com/instagram/";
    private static final String USER_VARIABLES = "{\"id\":\"25025320\",\"first\":12,\"after\":\"\"}";
    private static final String CHANNEL_VARIABLES = "{\"id\":\"25025320\",\"first\":12,\"after\":\"\"}";
    private static final String LAST_STORY_VARIABLES = "{\"reel_ids\":[25025320],\"tag_names\":[],\"location_ids\":[],\"highlight_reel_ids\":[],\"precomposed_overlay\":false,\"show_story_viewer_list\":true,\"story_viewer_fetch_count\":50,\"story_viewer_cursor\":\"\",\"stories_video_dash_manifest\":false}";
    private static final String HIGHLIGHT_STORIES_VARIABLES = "{\"user_id\":\"25025320\",\"include_chaining\":true,\"include_reel\":true,\"include_suggested_users\":false,\"include_logged_out_extras\":false,\"include_highlight_reels\":true,\"include_live_status\":true}";

    private static final Pattern SCRIPT_SRC_PATTERN = Pattern.compile("<script[^<>]*?\\ssrc=['\"]?(.*?)['\"]?\\s.*?>");
    private static final Pattern UUID_PATTERN = Pattern.compile("([a-fA-F0-9]{8}[a-fA-F0-9]{4}[a-fA-F0-9]{4}[a-fA-F0-9]{4}[a-fA-F0-9]{12})");

    public QueryHashTool(AuthenticationManager authenticationManager, CloseableHttpClient client) {
        super(authenticationManager, client);
    }

    public String loadChannelQueryHash() throws IOException {
        return loadQueryHashes(USER_URL).parallelStream().filter(this::checkChannelQueryHash).findAny().orElseThrow(() -> new IllegalStateException("fail to get channel query hash "));
    }

    public String loadTagPostsQueryHash() throws IOException {
        return loadQueryHashes(TAG_URL).parallelStream().filter(this::checkTagPostsQueryHash).findAny().orElseThrow(() -> new IllegalStateException("fail to get tag posts query hash "));
    }

    public String loadStoryQueryHash() throws IOException {
        return loadQueryHashes(USER_URL).parallelStream().filter(this::checkStoryQueryHash).findAny().orElseThrow(() -> new IllegalStateException("fail to get story query hash "));
    }

    public String loadUserPostsQueryHash() throws IOException {
        return loadQueryHashes(USER_URL).parallelStream().filter(this::checkUserPostsQueryHash).findAny().orElseThrow(() -> new IllegalStateException("fail to get user posts query hash "));
    }

    public String loadHighlightStoriesQueryHash() throws IOException {
        return loadQueryHashes(USER_URL).parallelStream().filter(this::checkHighlightStoriesQueryHash).findAny().orElseThrow(() -> new IllegalStateException("fail to get highlight stories query hash "));
    }

    protected Set<String> loadQueryHashes(String url) throws IOException {
        String content = getPageContent(url);
        Set<String> hashes = Collections.synchronizedSet(new HashSet<>());

        Matcher scriptMatcher = SCRIPT_SRC_PATTERN.matcher(content);
        List<String> scripts = new ArrayList<>();
        while (scriptMatcher.find()) {
            String script = scriptMatcher.group(1);
            scripts.add(URL_PREFIX + script);
        }
        scripts.parallelStream().map(s -> HttpUtils.toString(client, s, null)).map(UUID_PATTERN::matcher).forEach(m -> {
            while (m.find()) {
                String key = m.group(1);
                hashes.add(key);
            }
        });

        return hashes;
    }

    protected boolean checkHighlightStoriesQueryHash(String hash) {
        return checkQueryHash(hash, graphqlQuery -> graphqlQuery.variables(HIGHLIGHT_STORIES_VARIABLES), jsonExecutor -> jsonExecutor.execute("data->user->edge_highlight_reels").isPresent());
    }

    protected boolean checkUserPostsQueryHash(String hash) {
        return checkQueryHash(hash, graphqlQuery -> graphqlQuery.variables(USER_VARIABLES), jsonExecutor -> jsonExecutor.execute("data->user->edge_owner_to_timeline_media").isPresent());
    }

    protected boolean checkStoryQueryHash(String hash) {
        return checkQueryHash(hash, graphqlQuery -> graphqlQuery.variables(LAST_STORY_VARIABLES), jsonExecutor -> jsonExecutor.execute("data->reels_media").isPresent());
    }

    protected boolean checkTagPostsQueryHash(String hash) {
        return checkQueryHash(hash, graphqlQuery -> graphqlQuery.variables(TAG_VARIABLES), jsonExecutor -> jsonExecutor.execute("data->hashtag->edge_hashtag_to_media").isPresent());
    }

    protected boolean checkChannelQueryHash(String hash) {
        return checkQueryHash(hash, graphqlQuery -> graphqlQuery.variables(CHANNEL_VARIABLES), jsonExecutor -> jsonExecutor.execute("data->user->edge_felix_video_timeline").isPresent());
    }

    protected boolean checkQueryHash(String hash, Consumer<GraphqlQuery> consumer, Function<JsonExecutor, Boolean> function) {
        JsonExecutor ee;
        try {
            GraphqlQuery graphqlQuery = GraphqlQuery.create(client);
            consumer.accept(graphqlQuery);
            ee = graphqlQuery.queryHash(hash).execute();
        } catch (InvalidStateCodeException ex) {
            if (ex.getCode() == 429) {
                sleep(10);
                return checkQueryHash(hash, consumer, function);
            }
            return false;
        } catch (Exception ex) {
            return false;
        }
        return function.apply(ee);
    }

    private void sleep(int sec) {
        try {
            Thread.sleep(sec * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    protected String getPageContent(String url) throws IOException {
        HttpClientContext context = HttpClientContext.create();
        String content = HttpUtils.toString(client, url, context);
        if (authenticationManager.checkAuthentication(content, context)) {
            return getPageContent(url);
        }
        return content;
    }

}
