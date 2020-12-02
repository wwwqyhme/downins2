package me.qyh.instd4j.parser;

import me.qyh.instd4j.config.Config;
import me.qyh.instd4j.http.HttpUtils;
import me.qyh.instd4j.http.InvalidStateCodeException;
import me.qyh.instd4j.http.RoutePlanner;
import me.qyh.instd4j.parser.auth.AuthenticationProvider;
import me.qyh.instd4j.parser.auth.CookieStorePersistence;
import me.qyh.instd4j.parser.exception.PostNotFoundException;
import me.qyh.instd4j.parser.job.JobConsumer;
import me.qyh.instd4j.parser.job.JobManager;
import me.qyh.instd4j.parser.job.ParseJob;
import me.qyh.instd4j.util.JsonExecutor;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.Closeable;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class InsParser implements Closeable {

    public static final String GRAPH_IMAGE = "GraphImage";
    public static final String GRAPH_VIDEO = "GraphVideo";
    public static final String GRAPH_SIDECAR = "GraphSidecar";

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/73.0.3683.103 Safari/537.36";
    private static final String HIGHLIGHT_STORIES_DETAIL_VARIABLES = "{\"reel_ids\":[],\"tag_names\":[],\"location_ids\":[],\"highlight_reel_ids\":[%s],\"precomposed_overlay\":false,\"show_story_viewer_list\":true,\"story_viewer_fetch_count\":50,\"story_viewer_cursor\":\"\",\"stories_video_dash_manifest\":false}";
    private static final int TIME_OUT = 30 * 1000;

    private static final String POST_URL = "https://www.instagram.com/p/%s/?__a=1";
    private final Config config;
    private final JobManager jobManager;
    private final AuthenticationManager authenticationManager;
    private final CloseableHttpClient client;


    public InsParser(Config config) {
        this.config = config;
        this.jobManager = new JobManager(config.getSleepMill());
        AuthenticationProvider provider = config.getAuthenticationProvider();
        BasicCookieStore cookieStore = provider.loadCookieStore().orElse(new BasicCookieStore());
        this.client = HttpClients.custom()
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setConnectionRequestTimeout(TIME_OUT)
                        .setConnectTimeout(TIME_OUT)
                        .setSocketTimeout(TIME_OUT)
                        .setCookieSpec(CookieSpecs.DEFAULT).build()).
                        addInterceptorLast((HttpRequestInterceptor) (httpRequest, httpContext) -> {
                            httpRequest.addHeader("user-agent", USER_AGENT);
                        })
                .setDefaultCookieStore(new CookieStoreDelegate(provider,cookieStore))
                .setMaxConnPerRoute(config.getHttpConfig().getMaxConnections())
                .setMaxConnTotal(config.getHttpConfig().getMaxConnections())
                .setRoutePlanner(new RoutePlanner(config.getHttpConfig().getProxyConfig())).build();
        this.authenticationManager = new AuthenticationManager(this.client, provider);
    }

    /**
     * 模拟登录
     *
     * @throws IOException 请求异常
     */
    public void authenticate() throws IOException {
        this.authenticationManager.authenticate();
    }

    /**
     * 创建一个用户解析器
     *
     * @param username 用户名
     * @return 用户解析器
     */
    public UserParser creatUserParser(String username) {
        return new UserParser(username, this.client, config.getQueryHashLoader(), this.jobManager, this.authenticationManager);
    }

    /**
     * 创建一个标签解析器
     *
     * @param tag 标签
     * @return 标签解析器
     */
    public TagParser creatTagParser(String tag) {
        return new TagParser(tag, this.client, config.getQueryHashLoader(), this, this.jobManager);
    }

    /**
     * 解析IGTV
     *
     * @param shortcode shortcode
     * @return IGTV
     */
    public Post parseIGTV(String shortcode) throws IOException {
        return parsePost(shortcode);
    }

    /**
     * 解析帖子
     *
     * @param shortcode shortcode
     * @return 帖子
     */
    public Post parsePost(String shortcode) throws IOException {
        String url = String.format(POST_URL, shortcode);
        String content;
        HttpClientContext context = HttpClientContext.create();
        try {
            content = HttpUtils.toString(client, url, context);
        } catch (InvalidStateCodeException ex) {
            if (ex.getCode() == 404) {
                throw new PostNotFoundException(shortcode);
            }
            throw ex;
        }
        if (authenticationManager.checkAuthentication(content, context)) {
            return parsePost(shortcode);
        }
        JsonExecutor executor = new JsonExecutor(content).execute("graphql->shortcode_media");
        return ParseUtils.parsePostNode(executor);
    }

    /**
     * 创建一个用于解析并获取query hash的辅助实例
     *
     * @return
     */
    public QueryHashTool createQueryHashTool() {
        return new QueryHashTool(this.authenticationManager, this.client);
    }

    public void parseHighlightStories(List<String> ids, JobConsumer<Map<String, List<Link>>> consumer) {
        this.jobManager.addJob(new ParseJob<>(() -> parseHighlightStoriesImmediately(ids), consumer));
    }

    public Map<String, List<Link>> parseHighlightStoriesImmediately(List<String> ids) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (String id : ids) {
            sb.append("\"").append(id).append("\"");
            sb.append(",");
        }
        sb.deleteCharAt(sb.length() - 1);
        String variables = String.format(HIGHLIGHT_STORIES_DETAIL_VARIABLES, sb.toString());
        JsonExecutor executors = GraphqlQuery.create(client)
                .queryHash(config.getQueryHashLoader().loadStoryQueryHash())
                .variables(variables)
                .execute()
                .execute("data->reels_media");
        Map<String, List<Link>> map = new HashMap<>();
        for (JsonExecutor executor : executors) {
            String id = executor.execute("id").getAsString();
            List<Link> links = new ArrayList<>();
            for (JsonExecutor item : executor.execute("items")) {
                links.add(ParseUtils.parseStoryItemNode(item));
            }
            map.put(id, links);
        }
        return map;
    }

    @Override
    public void close() throws IOException {
        this.jobManager.stopListen();
        this.client.close();
    }

    /**
     * 获取当前队列中的任务数量
     *
     * @return
     */
    public int getJobCount() {
        return this.jobManager.getJobCount();
    }


    /**
     * 等待队列中任务执行完毕，调用这个方法后仍然可以添加新任务，但不会等待新任务执行完毕
     *
     * @throws InterruptedException
     */
    public void waitJobsComplete() throws InterruptedException {
        this.jobManager.waitJobsComplete();
    }

    /**
     * @throws InterruptedException
     */
    public void waitJobsComplete(long timeout, TimeUnit unit) throws InterruptedException {
        this.jobManager.waitJobsComplete(timeout, unit);
    }

    /**
     * 清空队列中的任务，如果有线程等待任务执行完毕，那么将会立即停止等待，并且抛出 JobCanceledException
     */
    public void clearJobs() {
        this.jobManager.clearJobs();
    }

    private final class CookieStoreDelegate implements CookieStore {
        private final AuthenticationProvider authenticationProvider;
        private final BasicCookieStore basicCookieStore;

        private CookieStoreDelegate(AuthenticationProvider authenticationProvider, BasicCookieStore basicCookieStore) {
            this.authenticationProvider = authenticationProvider;
            this.basicCookieStore = basicCookieStore;
        }

        @Override
        public void addCookie(Cookie cookie) {
            basicCookieStore.addCookie(cookie);
            authenticationProvider.save(basicCookieStore);
        }

        @Override
        public List<Cookie> getCookies() {
            return basicCookieStore.getCookies();
        }

        @Override
        public boolean clearExpired(Date date) {
            if (basicCookieStore.clearExpired(date)) {
                authenticationProvider.save(basicCookieStore);
                return true;
            }
            return false;
        }

        @Override
        public void clear() {
            basicCookieStore.clear();
            authenticationProvider.save(basicCookieStore);
        }
    }
}
