package me.qyh.instdrun.downloader;

import me.qyh.instd4j.download.Group;
import me.qyh.instd4j.download.GroupItem;
import me.qyh.instd4j.parser.HighlightStory;
import me.qyh.instd4j.parser.Link;
import me.qyh.instd4j.parser.UserParser;
import me.qyh.instdrun.config.Configure;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class HighlightStoriesDownloader extends AbstractDownloadRunner {

    private final UserParser userParser;
    private final Path dir;
    private final ErrorFileWriter errorFileWriter;

    public HighlightStoriesDownloader(String username) {
        super();
        this.userParser = insParser.creatUserParser(username);
        this.dir = Configure.get().getConfig().getDownloadDir().resolve(username + "_hs");
        Path error = dir.resolve("error.json");
        this.errorFileWriter = new ErrorFileWriter(error);
    }

    @Override
    public void run(String[] args) throws Exception {
        try (ErrorFileDownloader errorFileDownloader = new ErrorFileDownloader(errorFileWriter)) {
            errorFileDownloader.download();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
        this.userParser.highlightStories(new HighlightStoriesConsumer());
    }

    private final class HighlightStoriesConsumer extends DefaultJobConsumer<List<HighlightStory>> {

        @Override
        public void accept(List<HighlightStory> stories) {
            if (stories.isEmpty()) {
                exit();
                return;
            }
            insParser.parseHighlightStories(stories.stream().map(HighlightStory::getId).collect(Collectors.toList()), new ParseHighlightStoriesConsumer());
        }
    }

    private final class ParseHighlightStoriesConsumer extends DefaultJobConsumer<Map<String, List<Link>>> {

        @Override
        public void accept(Map<String, List<Link>> map) {
            for (Map.Entry<String, List<Link>> it : map.entrySet()) {
                final String id = it.getKey();
                List<Link> links = it.getValue();
                int index = 0;
                List<GroupItem> items = new ArrayList<>();
                for (Link link : links) {
                    Path dest = dir.resolve(id + "_" + ++index + "." + link.getExtension());
                    if (Files.exists(dest)) {
                        continue;
                    }
                    items.add(new GroupItem(link.getUrl(), dest, PRINT_LISTENER));
                }
                downloader.download(new Group(items), (successes, errors) -> {
                    if (!errors.isEmpty()) {
                        errorFileWriter.consume(files -> {
                            files.addAll(errors.stream().map(item -> new ErrorFile(item.getLink(), item.getDest().toString(), id)).collect(Collectors.toList()));
                            return true;
                        });
                    }
                });
            }
            exit();
        }
    }

    private void exit() {
        try {
            downloader.waitForDownloadComplete();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println("下载完成");
        errorFileWriter.consume(files->{
            if(files.size() > 0)
                System.out.println("存在下载失败的文件，请运行下载命令下载失败文件！");
            return false;
        });
        System.exit(0);
    }

}
