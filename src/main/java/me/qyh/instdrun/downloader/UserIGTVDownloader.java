package me.qyh.instdrun.downloader;

import me.qyh.instd4j.parser.IGTV;
import me.qyh.instd4j.parser.Page;
import me.qyh.instd4j.parser.Post;
import me.qyh.instd4j.parser.exception.PostNotFoundException;
import me.qyh.instd4j.parser.job.JobConsumer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class UserIGTVDownloader extends UserPostsDownloader {

    public UserIGTVDownloader(String username) {
        super(username);
    }

    @Override
    public void run(String[] args) throws IOException {
        downloadErrorFiles();
        downloadUnCompletePosts();
        this.userParser.igtvs(50, "", new IGTVsJobConsumer());
    }

    private final class IGTVsJobConsumer extends DefaultJobConsumer<Page<IGTV>> {

        @Override
        public void accept(Page<IGTV> page) {

            ExecutorService executorService = Executors.newFixedThreadPool(5);
            CountDownLatch cdl = new CountDownLatch(page.getDatas().size());
            List<Post> posts = Collections.synchronizedList(new ArrayList<>());
            for (IGTV igtv : page.getDatas()) {
                executorService.execute(() -> {
                    try {
                        Post post = insParser.parsePost(igtv.getShortcode());
                        posts.add(post);
                    } catch (PostNotFoundException ignored) {
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    } finally {
                        cdl.countDown();
                    }
                });
            }
            try {
                cdl.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }

            if (!posts.isEmpty()) {
                downloadPosts(posts);
            }

            if (page.getPageInfo().isHasNextPage()) {
                userParser.igtvs(50, page.getPageInfo().getEndCursor(), new IGTVsJobConsumer());
            } else {
                exit();
            }
        }
    }

    @Override
    protected Path createDir(Path root, String username) {
        return super.createDir(root, username + "_igtv");
    }
}
