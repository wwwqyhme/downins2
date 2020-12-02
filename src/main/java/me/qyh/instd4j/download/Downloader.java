package me.qyh.instd4j.download;

import me.qyh.instd4j.config.HttpConfig;
import me.qyh.instd4j.http.RoutePlanner;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Downloader implements Closeable {

    private final ThreadPoolExecutor executorService;
    private final CloseableHttpClient client;

    private final int threadNum;
    private static final DownloadListener EMPTY_DOWNLOAD_LISTENER = new DownloadListener() {
    };

    private final Object lock = new Object();
    private final Set<Monitor> runningMonitors = ConcurrentHashMap.newKeySet();

    public Downloader(int threadNum, HttpConfig config) {
        this.threadNum = threadNum;
        this.executorService = new ThreadPoolExecutor(threadNum, threadNum,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>()) {
            @Override
            protected void beforeExecute(Thread t, Runnable r) {
                if (r instanceof Monitor) {
                    runningMonitors.add((Monitor) r);
                }
            }

            @Override
            protected void afterExecute(Runnable r, Throwable t) {
                if (r instanceof Monitor) {
                    runningMonitors.remove(r);
                }
            }
        };
        this.client = HttpClients.custom()
                .setMaxConnPerRoute(Math.max(threadNum, config.getMaxConnections()))
                .setMaxConnTotal(Math.max(threadNum, config.getMaxConnections()))
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setConnectionRequestTimeout(config.getConnectionRequestTimeout())
                        .setConnectTimeout(config.getConnectTimeout())
                        .setSocketTimeout(config.getSocketTimeout()).build())
                .setRoutePlanner(new RoutePlanner(config.getProxyConfig()))
                .build();
    }

    /**
     * 下载一个文件
     *
     * @param link     文件下载地址
     * @param dest     文件存放位置
     * @param listener 下载监听器
     */
    public Monitor download(String link, Path dest, DownloadListener listener) {
        return this.download(new GroupItem(link, dest, listener));
    }

    /**
     * 下载一个文件
     *
     * @param item
     */
    public Monitor download(GroupItem item) {
        DownloadTask monitor = new DownloadTask(item);
        execute(monitor);
        return monitor;
    }

    /**
     * 下载一组文件
     *
     * @param group
     * @param listener
     */
    public List<Monitor> download(Group group, GroupDownloadListener listener) {
        int groupSize = group.getItems().size();
        if (groupSize == 0) return List.of();
        AtomicInteger counter = new AtomicInteger();
        final List<GroupItem> successList = Collections.synchronizedList(new ArrayList<>());
        final List<GroupItem> errorList = Collections.synchronizedList(new ArrayList<>());
        List<Monitor> monitors = new ArrayList<>();
        for (GroupItem item : group.getItems()) {
            DownloadListener downloadListener = item.getDownloadListener();
            if (downloadListener == null) {
                downloadListener = EMPTY_DOWNLOAD_LISTENER;
            }
            DelegateDownloadListener delegateDownloadListener = new DelegateDownloadListener(downloadListener, item, successList, errorList, counter, groupSize, listener);
            Monitor monitor = new DownloadTask(new GroupItem(item.getLink(), item.getDest(), delegateDownloadListener));
            monitors.add(monitor);
        }
        execute(monitors.stream().map(m -> (Runnable) m).toArray(Runnable[]::new));
        return monitors;
    }

    /**
     * 关闭下载
     * <ol>
     *  <li>关闭下载线程池</li>
     *  <li>等待下载立即结束</li>
     *  <li>取消正在运行的任务</li>
     *  <li>关闭连接池</li>
     * </ol>
     *
     * @throws IOException
     */
    @Override
    public void close() throws IOException {
        List<Runnable> tasks = executorService.shutdownNow();
        tasks.stream().filter(r -> r instanceof WaitTask).map(r -> (WaitTask) r).forEach(WaitTask::cancel);
        runningMonitors.forEach(Monitor::cancel);
        client.close();
    }

    /**
     * 等待当前的下载全部完成
     * <p>在等待期间，你仍然可以添加新的任务，但它们只有再等待结束之后才会被继续执行</p>
     *
     * @throws InterruptedException
     */
    public synchronized void waitForDownloadComplete() throws InterruptedException {
        CountDownLatch cdl = new CountDownLatch(this.threadNum);
        Runnable[] tasks = new Runnable[this.threadNum];
        Arrays.fill(tasks, new WaitTask(cdl));
        execute(tasks);
        cdl.await();
        synchronized (lock) {
            lock.notifyAll();
        }
    }

    /**
     * 获取正在运行的下载任务
     *
     * @return
     */
    public List<Monitor> getRunningMonitors() {
        return List.copyOf(this.runningMonitors);
    }

    private synchronized void execute(Runnable... tasks) {
        for (Runnable task : tasks) {
            this.executorService.execute(task);
        }
    }

    private final class DownloadTask implements Runnable, Monitor {

        private final GroupItem item;
        private final HttpGet request;

        private long totalBytes = -1;
        private volatile State state;
        private DownloadedBytesResolver downloadedBytesResolver;

        private DownloadTask(GroupItem item) {
            this.item = item;
            this.request = new HttpGet(item.getLink());
            this.state = State.WAIT;
        }

        @Override
        public void run() {
            this.state = State.RUNNING;
            DownloadListener downloadListener = item.getDownloadListener();
            if (downloadListener == null) {
                downloadListener = EMPTY_DOWNLOAD_LISTENER;
            }
            boolean success = false;
            Exception ex = null;
            try (CloseableHttpResponse response = client.execute(request)) {
                downloadListener.onStart(item);
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    totalBytes = entity.getContentLength();
                    Path parent = item.getDest().getParent();
                    if (!Files.exists(parent)) {
                        synchronized (client) {
                            if (!Files.exists(parent)) {
                                Files.createDirectories(parent);
                            }
                        }
                    }
                    final Path temp = Files.createTempFile(null, null);
                    this.downloadedBytesResolver = () -> {
                        try {
                            return Files.size(temp);
                        } catch (IOException e) {
                            return -1;
                        }
                    };
                    try (OutputStream os = Files.newOutputStream(temp)) {
                        entity.writeTo(os);
                        Files.move(temp, item.getDest(), StandardCopyOption.REPLACE_EXISTING);
                        success = true;
                        this.state = State.COMPLETE;
                    } finally {
                        try {
                            Files.deleteIfExists(temp);
                        } catch (Exception ignored) {
                        }
                    }
                } else {
                    throw new IOException("download file ,the response is empty : " + item.getLink());
                }
            } catch (Exception exception) {
                ex = exception;
            } catch (Throwable throwable) {
                ex = new RuntimeException(throwable);
            } finally {
                try {
                    request.releaseConnection();
                } finally {
                    if (State.RUNNING.equals(this.state)) {
                        this.state = State.COMPLETE;
                    }
                    downloadListener.onComplete(item, success, ex);
                }
            }
        }

        @Override
        public GroupItem getGroupItem() {
            return this.item;
        }

        @Override
        public long getDownloadedBytes() {
            return this.downloadedBytesResolver == null ? -1 : this.downloadedBytesResolver.getDownloadBytes();
        }

        @Override
        public long getTotalBytes() {
            return this.totalBytes;
        }

        @Override
        public void cancel() {
            synchronized (this) {
                if (State.COMPLETE.equals(this.state) || State.CANCEL.equals(this.state)) {
                    return;
                }
                request.abort();
                this.state = State.CANCEL;
            }
        }

        @Override
        public State getState() {
            return this.state;
        }
    }

    private static final class DelegateDownloadListener implements DownloadListener {
        private final DownloadListener delegate;
        private final GroupItem groupItem;
        private final List<GroupItem> successList;
        private final List<GroupItem> errorList;
        private final AtomicInteger counter;
        private final int groupSize;
        private final GroupDownloadListener groupDownloadListener;

        private DelegateDownloadListener(DownloadListener delegate, GroupItem groupItem, List<GroupItem> successList, List<GroupItem> errorList, AtomicInteger counter, int groupSize, GroupDownloadListener groupDownloadListener) {
            this.delegate = delegate;
            this.groupItem = groupItem;
            this.successList = successList;
            this.errorList = errorList;
            this.counter = counter;
            this.groupSize = groupSize;
            this.groupDownloadListener = groupDownloadListener;
        }

        @Override
        public void onStart(GroupItem item) {
            delegate.onStart(item);
        }

        @Override
        public void onComplete(GroupItem item, boolean success, Exception ex) {
            try {
                delegate.onComplete(item, success, ex);
            } finally {
                if (success) {
                    successList.add(groupItem);
                } else {
                    errorList.add(groupItem);
                }

                if (counter.incrementAndGet() == groupSize && groupDownloadListener != null) {
                    groupDownloadListener.onComplete(successList, errorList);
                }
            }
        }
    }

    private interface DownloadedBytesResolver {
        long getDownloadBytes();
    }

    private final class WaitTask implements Runnable {
        private final CountDownLatch countDownLatch;

        private WaitTask(CountDownLatch countDownLatch) {
            this.countDownLatch = countDownLatch;
        }

        @Override
        public void run() {
            synchronized (lock) {
                countDownLatch.countDown();
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        public void cancel() {
            countDownLatch.countDown();
        }
    }
}
