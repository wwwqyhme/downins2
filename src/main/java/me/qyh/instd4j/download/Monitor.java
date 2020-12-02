package me.qyh.instd4j.download;

public interface Monitor {

    /**
     * 获取当前下载的对象
     *
     * @return groupItem
     */
    GroupItem getGroupItem();

    /**
     * 获取当前已经下载的文件大小，如果获取失败，返回负数
     *
     * @return size
     */
    long getDownloadedBytes();

    /**
     * 获取需要下载的总字节大小，如果获取失败，返回负数
     *
     * @return size
     */
    long getTotalBytes();

    /**
     * 取消下载，如果下载已经完成，那么不起作用
     */
    void cancel();

    /**
     * 当前下载状态
     *
     * @return
     */
    State getState();

    enum State {
        WAIT,
        RUNNING,
        CANCEL,
        COMPLETE
    }
}
