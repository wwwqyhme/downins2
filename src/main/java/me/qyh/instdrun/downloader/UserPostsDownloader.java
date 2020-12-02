package me.qyh.instdrun.downloader;

import me.qyh.instd4j.download.Group;
import me.qyh.instd4j.download.GroupItem;
import me.qyh.instd4j.parser.Link;
import me.qyh.instd4j.parser.Page;
import me.qyh.instd4j.parser.Post;
import me.qyh.instd4j.parser.UserParser;
import me.qyh.instd4j.parser.exception.PrivateUserAccountException;
import me.qyh.instd4j.parser.exception.UserNotFoundException;
import me.qyh.instdrun.config.Configure;
import me.qyh.instdrun.config.DowninsConfig;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

class UserPostsDownloader extends AbstractDownloadRunner {
    protected final UserParser userParser;
    private final Path dir;
    private final ErrorFileWriter errorFileWriter;
    private final PostStateWriter postStateWriter;
    private final Path timestampFile;
    private final boolean increaseDownload;

    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public UserPostsDownloader(String username) {
        DowninsConfig config = Configure.get().getConfig();
        this.dir = createDir(config.getDownloadDir(), username);
        Path errorFile = dir.resolve("error.json");
        Path postsFile = dir.resolve("posts.txt");
        this.errorFileWriter = new ErrorFileWriter(errorFile);
        this.postStateWriter = new PostStateWriter(postsFile);
        this.userParser = insParser.creatUserParser(username);
        this.timestampFile = dir.resolve("timestamp");
        this.increaseDownload = Files.exists(timestampFile);
    }

    @Override
    public void run(String[] args) throws IOException {
        downloadErrorFiles();
        downloadUnCompletePosts();
        this.userParser.posts(50, "", new PostsJobConsumer());
    }

    protected final class PostsJobConsumer extends DefaultJobConsumer<Page<Post>> {

        @Override
        public void onError(Exception ex) {
            if (ex instanceof UserNotFoundException) {
                System.out.println("用户不存在");
                System.exit(0);
                return;
            }
            if (ex instanceof PrivateUserAccountException) {
                System.out.println("需要关注该用户才能下载");
                System.exit(0);
                return;
            }
            super.onError(ex);
        }

        @Override
        public void accept(Page<Post> postPage) {
            downloadPosts(postPage.getDatas());
            if (postPage.getPageInfo().isHasNextPage()) {
                userParser.posts(50, postPage.getPageInfo().getEndCursor(), new PostsJobConsumer());
            } else {
                exit();
            }
        }
    }

    protected void downloadPosts(List<Post> posts) {
        List<PostState> needToWriteList = new ArrayList<>();
        postStateWriter.consume(states -> {
            for (Post post : posts) {
                boolean contain = states.stream().anyMatch(state -> state.getPostId().equals(post.getId()));
                if (!contain) {
                    needToWriteList.add(new PostState(post.getId(), false));
                }
            }
            states.addAll(needToWriteList);
            return true;
        });
        for (Post post : posts) {
            postStateWriter.consume(states -> {
                if (needToWriteList.stream().anyMatch(state -> state.getPostId().equals(post.getId()))) {
                    return false;
                }
                if (increaseDownload && states.stream().anyMatch(state -> state.getPostId().equals(post.getId()))) {
                    exit();
                }
                return false;
            });
            int index = 0;
            List<GroupItem> items = new ArrayList<>();
            for (Link link : post.getLinks()) {
                Path dest = dir.resolve(post.getId() + "_" + post.getShortcode() + "_" + ++index + "." + link.getExtension());
                if (Files.exists(dest)) {
                    continue;
                }
                items.add(new GroupItem(link.getUrl(), dest, PRINT_LISTENER));
            }
            if (items.isEmpty()) {
                postStateWriter.setComplete(post.getId());
                continue;
            }
            downloader.download(new Group(items), (list, list1) -> {
                try {
                    if (!list1.isEmpty()) {
                        errorFileWriter.consume(files -> {
                            files.addAll(list1.stream().map(item -> new ErrorFile(item.getLink(), item.getDest().toString(), post.getId())).collect(Collectors.toList()));
                            return true;
                        });
                    }
                } finally {
                    postStateWriter.setComplete(post.getId());
                }
            });
        }
    }

    protected void downloadErrorFiles() {
        try (ErrorFileDownloader errorFileDownloader = new ErrorFileDownloader(errorFileWriter)) {
            errorFileDownloader.download();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    protected void downloadUnCompletePosts() throws IOException {
        UnCompletePostsDownloader unCompletePostsDownloader = new UnCompletePostsDownloader(errorFileWriter, postStateWriter, insParser, dir, downloader);
        unCompletePostsDownloader.download();
    }

    protected Path createDir(Path root, String username) {
        return root.resolve(username);
    }

    protected void exit() {
        try {
            downloader.waitForDownloadComplete();
            downloadErrorFiles();
            downloadUnCompletePosts();
            downloader.waitForDownloadComplete();
            //append timestamp to file
            Util.createFile(this.timestampFile);
            Files.write(this.timestampFile, List.of(LocalDateTime.now().format(dtf)), StandardOpenOption.APPEND);
            errorFileWriter.consume(files -> {
                if (files.size() > 0)
                    System.out.println("存在下载失败的文件，请运行下载命令下载失败文件！");
                return false;
            });
            System.out.println("下载完成");
            System.exit(0);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}
