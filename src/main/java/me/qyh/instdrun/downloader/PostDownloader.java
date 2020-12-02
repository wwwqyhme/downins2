package me.qyh.instdrun.downloader;

import me.qyh.instd4j.parser.Link;
import me.qyh.instd4j.parser.Post;
import me.qyh.instd4j.parser.exception.PostNotFoundException;
import me.qyh.instdrun.config.Configure;
import me.qyh.instdrun.config.DowninsConfig;

import java.nio.file.Files;
import java.nio.file.Path;

class PostDownloader extends AbstractDownloadRunner {
    private final String shortcode;
    private final Path dir;

    public PostDownloader(String shortcode) {
        this.shortcode = shortcode;
        DowninsConfig config = Configure.get().getConfig();
        this.dir = config.getDownloadDir().resolve(shortcode);
    }

    @Override
    public void run(String[] args) throws Exception {
        Post post;
        try {
            post = this.insParser.parsePost(shortcode);
        } catch (PostNotFoundException ex) {
            System.out.println("帖子不存在");
            System.exit(0);
            return;
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(0);
            return;
        }

        int index = 0;
        for (Link link : post.getLinks()) {
            Path dest = dir.resolve(++index + "." + link.getExtension());
            if (Files.exists(dest)) {
                System.out.println("文件：" + dest + "已经存在");
                continue;
            }
            downloader.download(link.getUrl(), dest, PRINT_LISTENER);
        }

        downloader.waitForDownloadComplete();
        super.close();
    }
}
