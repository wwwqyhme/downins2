package me.qyh.instdrun.downloader;

import me.qyh.instd4j.parser.Link;
import me.qyh.instd4j.parser.UserParser;
import me.qyh.instdrun.config.Configure;
import me.qyh.instdrun.config.DowninsConfig;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

class LastStoryDownloader extends AbstractDownloadRunner {
    private final Path dir;
    private final UserParser userParser;
    private final String id;

    public LastStoryDownloader(String username, String id) {
        DowninsConfig config = Configure.get().getConfig();
        this.dir = config.getDownloadDir().resolve(username).resolve("_story");
        this.userParser = insParser.creatUserParser(username);
        this.id = id;
    }

    @Override
    public void run(String[] args) throws Exception {
        List<Link> links = userParser.lastStoryImmediately();
        int index = 0;
        for (Link link : links) {
            Path dest = dir.resolve(id + "_" + ++index + "." + link.getExtension());
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
