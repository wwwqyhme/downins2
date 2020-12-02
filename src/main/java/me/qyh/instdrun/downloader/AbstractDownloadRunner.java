package me.qyh.instdrun.downloader;

import me.qyh.instd4j.download.DownloadListener;
import me.qyh.instd4j.download.Downloader;
import me.qyh.instd4j.download.GroupItem;
import me.qyh.instd4j.parser.InsParser;
import me.qyh.instd4j.parser.job.JobConsumer;
import me.qyh.instdrun.config.Configure;
import me.qyh.instdrun.config.DowninsConfig;

import java.io.IOException;

abstract class AbstractDownloadRunner implements DownloadRunner {

    protected final InsParser insParser;
    protected final Downloader downloader;

    public static final DownloadListener PRINT_LISTENER = new DownloadListener() {
        @Override
        public void onStart(GroupItem item) {
            System.out.println("开始下载文件：" + item.getLink() + ",存放位置：" + item.getDest());
        }

        @Override
        public void onComplete(GroupItem item, boolean success, Exception ex) {
            String output = item.getLink() + "下载：" + (success ? "成功" : "失败");
            if (success) {
                output += "  文件位置：" + item.getDest();
            }
            System.out.println(output);
            if (ex != null) {
                ex.printStackTrace();
            }
        }
    };

    protected AbstractDownloadRunner() {
        DowninsConfig config = Configure.get().getConfig();
        this.insParser = new InsParser(config);
        this.downloader = new Downloader(config.getThreadNum(), config.getHttpConfig());
       /* new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(2000L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                List<Monitor> monitorList = downloader.getRunningMonitors();
                if (!monitorList.isEmpty()) {
                    for (Monitor monitor : monitorList) {
                        long db = monitor.getDownloadedBytes();
                        long tb = monitor.getTotalBytes();
                        if (db > 0 && db < tb) {
                            double percent = (double) 100 * db / (double) tb;
                            System.out.println(monitor.getGroupItem().getDest() + "已经下载:" + new DecimalFormat("#.##").format(percent));
                        }
                    }
                }
            }
        }).start();*/
    }

    protected void close() throws IOException {
        insParser.close();
        downloader.close();
    }

    protected abstract static class DefaultJobConsumer<T> implements JobConsumer<T> {

        @Override
        public void onError(Exception ex) {
            ex.printStackTrace();
            System.out.println("异常退出，请重新运行下载命令！");
            System.exit(-1);
        }
    }
}
