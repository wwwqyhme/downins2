package me.qyh.instdrun.downloader;

import me.qyh.instdrun.config.Configure;
import me.qyh.instd4j.download.DownloadListener;
import me.qyh.instd4j.download.Downloader;
import me.qyh.instd4j.download.GroupItem;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

class ErrorFileDownloader implements Closeable {

    private final Downloader downloader;
    private final ErrorFileWriter errorFileWriter;

    public ErrorFileDownloader(ErrorFileWriter errorFileWriter) {
        this.errorFileWriter = errorFileWriter;
        this.downloader = new Downloader(1, Configure.get().getConfig().getHttpConfig());
    }

    public void download() {
        List<ErrorFile> errorFiles = new ArrayList<>();
        errorFileWriter.consume(errorFiles::addAll);
        if (errorFiles.isEmpty()) {
            return;
        }
        System.out.println("开始下载失败文件");
        for (ErrorFile errorFile : errorFiles) {
            downloader.download(errorFile.getUrl(), Paths.get(errorFile.getDest()), new DownloadListener() {
                @Override
                public void onComplete(GroupItem item, boolean success, Exception ex) {
                    if (success) {
                        System.out.println("下载失败文件：" + item.getLink() + ",成功，文件位置：" + item.getDest());
                        errorFileWriter.delete(errorFile);
                    } else {
                        System.out.println("下载失败文件：" + item.getLink() + ",失败");
                        if (ex != null) {
                            ex.printStackTrace();
                        }
                    }
                }
            });
        }
        try {
            downloader.waitForDownloadComplete();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println("下载失败文件完毕");
    }

    @Override
    public void close() throws IOException {
        downloader.close();
    }
}
