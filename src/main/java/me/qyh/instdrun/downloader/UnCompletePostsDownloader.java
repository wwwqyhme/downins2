package me.qyh.instdrun.downloader;

import me.qyh.instd4j.download.Downloader;
import me.qyh.instd4j.download.Group;
import me.qyh.instd4j.download.GroupItem;
import me.qyh.instd4j.parser.InsParser;
import me.qyh.instd4j.parser.Link;
import me.qyh.instd4j.parser.Post;
import me.qyh.instd4j.parser.exception.PostNotFoundException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

class UnCompletePostsDownloader {

    private final ErrorFileWriter errorFileWriter;
    private final PostStateWriter postStateWriter;
    private final Path dir;
    private final InsParser insParser;
    private final Downloader downloader;

    public UnCompletePostsDownloader(ErrorFileWriter errorFileWriter, PostStateWriter postStateWriter, InsParser insParser, Path dir, Downloader downloader) {
        this.errorFileWriter = errorFileWriter;
        this.postStateWriter = postStateWriter;
        this.insParser = insParser;
        this.dir = dir;
        this.downloader = downloader;
    }

    public void download() throws IOException{
        List<PostState> uncompleted = new ArrayList<>();
        postStateWriter.consume(states -> {
            uncompleted.addAll(states.stream().filter(Predicate.not(PostState::isComplete)).collect(Collectors.toList()));
            return false;
        });
        if (uncompleted.isEmpty()) {
            return;
        }
        System.out.println("开始下载未完成的帖子");
        for (PostState postState : uncompleted) {
            String shortcode = getShortcode(postState.getPostId());
            Post post;
            try {
                post = insParser.parsePost(shortcode);
            } catch (PostNotFoundException ex) {
                this.postStateWriter.setComplete(postState.getPostId());
                continue;
            }

            List<GroupItem> groupItems = new ArrayList<>();
            int index = 0;
            for (Link link : post.getLinks()) {
                Path dest = dir.resolve(post.getId() + "_" + post.getShortcode() + "_" + ++index + "." + link.getExtension());
                if (Files.exists(dest)) {
                    System.out.println("文件：" + dest + "已经存在，跳过下载");
                    continue;
                }
                groupItems.add(new GroupItem(link.getUrl(), dest, AbstractDownloadRunner.PRINT_LISTENER));
            }

            if (groupItems.isEmpty()) {
                this.postStateWriter.setComplete(postState.getPostId());
                continue;
            }

            Group group = new Group(groupItems);

            this.downloader.download(group, (successes, errors) -> {
                System.out.println("帖子" + shortcode + "下载完毕");
                try {
                    if (!errors.isEmpty()) {
                        errorFileWriter.consume(states -> {
                            states.addAll(errors.stream().map(item -> new ErrorFile(item.getLink(), item.getDest().toString(), post.getId())).collect(Collectors.toList()));
                            return true;
                        });
                    }
                } finally {
                    postStateWriter.setComplete(postState.getPostId());
                }
            });

        }
        try {
            downloader.waitForDownloadComplete();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println("下载未完成的帖子完毕");
    }

    private static String getShortcode(String idStr) {
        String postId = "";
        long id = Long.parseLong(idStr);
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_";
        while (id > 0) {
            long remainder = (id % 64);
            id = (id - remainder) / 64;
            postId = alphabet.charAt((int) remainder) + postId;
        }
        return postId;
    }
}
