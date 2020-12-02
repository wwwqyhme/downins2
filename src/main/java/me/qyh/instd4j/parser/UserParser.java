package me.qyh.instd4j.parser;

import me.qyh.instd4j.config.QueryHashLoader;
import me.qyh.instd4j.http.HttpUtils;
import me.qyh.instd4j.http.InvalidStateCodeException;
import me.qyh.instd4j.parser.exception.PrivateUserAccountException;
import me.qyh.instd4j.parser.exception.UserNotFoundException;
import me.qyh.instd4j.parser.job.JobConsumer;
import me.qyh.instd4j.parser.job.JobManager;
import me.qyh.instd4j.parser.job.ParseJob;
import me.qyh.instd4j.util.JsonExecutor;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class UserParser {

    private static final String USER_URL = "https://www.instagram.com/%s/?__a=1";
    private static final String PAGING_VARIABLES = "{\"id\":\"%s\",\"first\":%s,\"after\":\"%s\"}";
    private static final String LAST_STORY_VARIABLES = "{\"reel_ids\":[%s],\"tag_names\":[],\"location_ids\":[],\"highlight_reel_ids\":[],\"precomposed_overlay\":false,\"show_story_viewer_list\":true,\"story_viewer_fetch_count\":50,\"story_viewer_cursor\":\"\",\"stories_video_dash_manifest\":false}";
    private static final String HIGHLIGHT_STORIES_VARIABLES = "{\"user_id\":\"%s\",\"include_chaining\":true,\"include_reel\":true,\"include_suggested_users\":false,\"include_logged_out_extras\":false,\"include_highlight_reels\":true,\"include_live_status\":true}";

    private final String username;
    private String userId;
    private final CloseableHttpClient client;
    private final QueryHashLoader queryHashLoader;
    private final JobManager jobManager;
    private final AuthenticationManager authenticationManager;

    UserParser(String username, CloseableHttpClient client, QueryHashLoader queryHashLoader, JobManager jobManager, AuthenticationManager authenticationManager) {
        this.username = username;
        this.client = client;
        this.queryHashLoader = queryHashLoader;
        this.jobManager = jobManager;
        this.authenticationManager = authenticationManager;
    }

    public void lastStory(JobConsumer<List<Link>> consumer) {
        this.jobManager.addJob(new ParseJob<>(this::lastStoryImmediately, consumer));
    }

    public List<Link> lastStoryImmediately() throws IOException {
        String variables = String.format(LAST_STORY_VARIABLES, getUserId());
        JsonExecutor executor = GraphqlQuery.create(client).variables(variables)
                .queryHash(queryHashLoader.loadStoryQueryHash())
                .referer("https://www.instagram.com/" + username + "/")
                .execute()
                .execute("data->reels_media");
        List<Link> links = new ArrayList<>();
        for (JsonExecutor media : executor) {
            for (JsonExecutor item : media.execute("items")) {
                links.add(ParseUtils.parseStoryItemNode(item));
            }
        }
        return links;
    }

    public void highlightStories(JobConsumer<List<HighlightStory>> consumer) {
        this.jobManager.addJob(new ParseJob<>(this::highlightStoriesImmediately, consumer));
    }

    /**
     * 查询用户最近的快拍，如果需要获取文件下载地址，请调用InsParser#parseHighlightStories方法
     *
     * @return
     * @throws IOException
     * @see InsParser#parseHighlightStoriesImmediately(List)
     */
    public List<HighlightStory> highlightStoriesImmediately() throws IOException {
        String variables = String.format(HIGHLIGHT_STORIES_VARIABLES, getUserId());
        JsonExecutor edges = GraphqlQuery.create(client).variables(variables)
                .queryHash(queryHashLoader.loadHighlightStoriesQueryHash())
                .referer("https://www.instagram.com/" + this.username + "/")
                .execute()
                .execute("data->user->edge_highlight_reels->edges");
        if (!edges.isPresent()) {
            return new ArrayList<>();
        }
        List<HighlightStory> stories = new ArrayList<>(edges.getSize());
        for (JsonExecutor edge : edges) {
            stories.add(ParseUtils.parseHighlightStoryNode(edge.execute("node")));
        }
        return stories;
    }

    /**
     * 分页获取IGTV
     * <p>IGTV列表不包括可供下载的地址，通过InsParser#parseIGTV来获取下载链接</p>
     *
     * @param pageSize  每页数目
     * @param endCursor 游标
     * @return 分页
     * @see InsParser#parseIGTV(String)
     */
    public Page<IGTV> igtvsImmediately(int pageSize, String endCursor) throws IOException {
        String variables = String.format(PAGING_VARIABLES, getUserId(), pageSize, endCursor);
        JsonExecutor executor = GraphqlQuery.create(client).variables(variables)
                .queryHash(queryHashLoader.loadChannelQueryHash())
                .referer("https://www.instagram.com/" + username + "/channel/")
                .execute()
                .execute("data->user->edge_felix_video_timeline");
        JsonExecutor edges = executor.execute("edges");
        List<IGTV> igtvs = new ArrayList<>(edges.getSize());
        PageInfo pageInfo = ParseUtils.parsePageInfoNode(executor.execute("page_info"));
        for (JsonExecutor edge : edges) {
            JsonExecutor node = edge.execute("node");
            igtvs.add(ParseUtils.parseIGTVNode(node));
        }
        return new Page<>(igtvs, pageInfo);
    }

    public void igtvs(int pageSize, String endCursor, JobConsumer<Page<IGTV>> consumer) {
        this.jobManager.addJob(new ParseJob<>(() -> igtvsImmediately(pageSize, endCursor), consumer));
    }

    /**
     * 分页获取帖子
     *
     * @param pageSize  每页数目
     * @param endCursor 游标
     * @return 分页
     */
    public Page<Post> postsImmediately(int pageSize, String endCursor) throws IOException {
        String variables = String.format(PAGING_VARIABLES, getUserId(), pageSize, endCursor);
        JsonExecutor executor = GraphqlQuery.create(client).variables(variables)
                .queryHash(queryHashLoader.loadUserPostsQueryHash())
                .referer("https://www.instagram.com/" + username + "/")
                .execute()
                .execute("data->user->edge_owner_to_timeline_media");
        JsonExecutor edges = executor.execute("edges");
        List<Post> posts = new ArrayList<>(edges.getSize());
        PageInfo pageInfo = ParseUtils.parsePageInfoNode(executor.execute("page_info"));
        for (JsonExecutor edge : edges) {
            JsonExecutor node = edge.execute("node");
            posts.add(ParseUtils.parsePostNode(node));
        }
        return new Page<>(posts, pageInfo);
    }

    public void posts(int pageSize, String endCursor, JobConsumer<Page<Post>> consumer) {
        this.jobManager.addJob(new ParseJob<>(() -> postsImmediately(pageSize, endCursor), consumer));
    }

    /**
     * 获取用户信息
     *
     * @return 用户信息
     */
    public User getUser() throws IOException {
        HttpClientContext context = HttpClientContext.create();
        String content;
        try {
            content = HttpUtils.toString(client, String.format(USER_URL, username), context);
        } catch (InvalidStateCodeException ex) {
            if (ex.getCode() == 404) {
                throw new UserNotFoundException("用户不存在");
            }
            throw ex;
        }
        if (this.authenticationManager.checkAuthentication(content, context)) {
            return getUser();
        }
        JsonExecutor jsonExecutor = new JsonExecutor(content).execute("graphql->user");
        if (jsonExecutor.isPresent()) {
            //check private
            if (jsonExecutor.execute("is_private").getAsBoolean() && !jsonExecutor.execute("followed_by_viewer").getAsBoolean()) {
                throw new PrivateUserAccountException(username);
            }
            String id = jsonExecutor.execute("id").getAsString();
            String avatar = jsonExecutor.execute("profile_pic_url_hd").getAsString();
            return new User(username, id, new Link(avatar, false));
        }
        throw new IllegalArgumentException("fail to get user info :" + content);
    }

    private String getUserId() throws IOException {
        if (this.userId == null) {
            this.userId = getUser().getId();
        }
        return this.userId;
    }
}
