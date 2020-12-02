package me.qyh.instd4j.parser;

import me.qyh.instd4j.config.QueryHashLoader;
import me.qyh.instd4j.parser.job.JobConsumer;
import me.qyh.instd4j.parser.job.JobManager;
import me.qyh.instd4j.parser.job.ParseJob;
import me.qyh.instd4j.util.JsonExecutor;
import org.apache.http.impl.client.CloseableHttpClient;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

public class TagParser {
    private static final String TAG_VARIABLES = "{\"tag_name\":\"%s\",\"first\":%s,\"after\":\"%s\"}";

    private final String tag;
    private final CloseableHttpClient client;
    private final QueryHashLoader queryHashLoader;
    private final InsParser insParser;
    private final JobManager jobManager;

    TagParser(String tag, CloseableHttpClient client, QueryHashLoader queryHashLoader, InsParser insParser, JobManager jobManager) {
        this.tag = tag;
        this.client = client;
        this.queryHashLoader = queryHashLoader;
        this.insParser = insParser;
        this.jobManager = jobManager;
    }

    /**
     * 分页浏览标签下的帖子
     *
     * @param pageSize  每页数目
     * @param endCursor 游标
     * @return 帖子列表 <p><b>如果帖子的类型为 GRAPH_SIDECAR或者GRAPH_VIDEO，会额外查询一次帖子用以获取下载地址</b></p>
     */
    public Page<Post> postsImmediately(int pageSize, String endCursor) throws IOException {
        String variables = String.format(TAG_VARIABLES, tag, pageSize, endCursor == null ? "" : endCursor);
        JsonExecutor executor = GraphqlQuery.create(client).variables(variables)
                .queryHash(queryHashLoader.loadTagPostsQueryHash())
                .referer("https://www.instagram.com/explore/tags/" + tag + "/")
                .execute()
                .execute("data->hashtag->edge_hashtag_to_media");
        JsonExecutor edges = executor.execute("edges");
        List<Post> posts = new ArrayList<>(edges.getSize());
        PageInfo pageInfo = ParseUtils.parsePageInfoNode(executor.execute("page_info"));
        for (JsonExecutor edge : edges) {
            JsonExecutor node = edge.execute("node");
            Post post = ParseUtils.parsePostNode(node);
            posts.add(new PostWrapper(post));
        }
        return new Page<>(posts, pageInfo);
    }

    public void posts(int pageSize, String endCursor, JobConsumer<Page<Post>> consumer) {
        this.jobManager.addJob(new ParseJob<>(() -> postsImmediately(pageSize, endCursor), consumer));
    }

    private final class PostWrapper extends Post {

        public PostWrapper(Post source) {
            super(source);
        }

        @Override
        public List<Link> getLinks() {
            List<Link> links = super.getLinks();
            if (links.isEmpty()) {
                Post post;
                try {
                    post = insParser.parsePost(getShortcode());
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
                links.addAll(post.getLinks());
            }
            return links;
        }
    }
}
