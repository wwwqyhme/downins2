package me.qyh.instdrun.downloader;

import me.qyh.instd4j.parser.Page;
import me.qyh.instd4j.parser.Post;
import me.qyh.instd4j.parser.TagParser;

import java.io.IOException;
import java.nio.file.Path;

class TagPostsDownloader extends UserPostsDownloader {

    private final TagParser tagParser;

    public TagPostsDownloader(String tagName) {
        super(tagName);
        this.tagParser = insParser.creatTagParser(tagName);
    }

    @Override
    public void run(String[] args) throws IOException {
        downloadErrorFiles();
        downloadUnCompletePosts();
        this.tagParser.posts(50, "", new PostsJobConsumer());
    }

    protected final class PostsJobConsumer extends DefaultJobConsumer<Page<Post>> {
        @Override
        public void accept(Page<Post> postPage) {
            downloadPosts(postPage.getDatas());
            if (postPage.getPageInfo().isHasNextPage()) {
                tagParser.posts(50, postPage.getPageInfo().getEndCursor(), new PostsJobConsumer());
            } else {
                exit();
            }
        }
    }

    @Override
    protected Path createDir(Path root, String username) {
        return super.createDir(root, username + "_tag");
    }
}
