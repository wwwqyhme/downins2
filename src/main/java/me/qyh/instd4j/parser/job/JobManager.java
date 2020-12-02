package me.qyh.instd4j.parser.job;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class JobManager {

    private final LinkedBlockingQueue<Job> queue = new LinkedBlockingQueue<>();

    private volatile boolean stopped;
    private volatile boolean stopListen;

    private final long sleepMill;
    private final Thread listener;

    public JobManager(int sleepMill) {
        super();
        this.sleepMill = sleepMill;
        this.listener = new Thread(this::startListen);
        this.listener.start();
    }

    /**
     * 停止监听任务队列<br>
     * 如果此时有正在执行中的任务，那么依旧会执行<br>
     * 同时清空队列中的任务，如果有线程等待任务执行完毕，那么将会立即停止等待，并且抛出 JobCanceledException
     * <p><b>停止之后JobManager不再工作</b></p>
     */
    public void stopListen() {
        checkCurrentThread();
        this.stopped = true;
        this.stopListen = true;
        clearJobs();
        queue.offer(() -> {
        });//add an empty job because queue take method maybe blocking
    }

    /**
     * 添加一个任务
     *
     * @param job
     */
    public void addJob(Job job) {
        checkStop();
        this.queue.offer(job);
    }

    /**
     * 添加一批任务
     *
     * @param jobs 任务列表
     * @see #clearJobs()
     * @see #stopListen()
     */
    public void addJobs(Job... jobs) {
        checkStop();
        queue.addAll(Arrays.asList(jobs));
    }

    /**
     * 添加一批任务，并且等待这批任务完成
     *
     * @param jobs 任务列表
     * @throws InterruptedException current thread interrupted
     * @throws JobCanceledException 当等待任务期间，调用 clearJobs方法或者stopListen()方法，但此时这批任务还未执行完毕
     */
    public void waitJobs(Job... jobs) throws InterruptedException {
        checkStop();
        checkCurrentThread();
        List<Job> jobList = new ArrayList<>(Arrays.asList(jobs));
        WaitNode cdl = new WaitNode();
        jobList.add(cdl);
        queue.addAll(jobList);
        cdl.await();
    }

    /**
     * 等待当前队列中的任务执行完毕
     * <P><b>调用这个方法之后，依然可以向队列中添加其他任务，但是不会等待后面添加的任务执行完毕</b></P>
     *
     * @throws InterruptedException
     * @throws JobCanceledException 等待过程中调用了 clearJob或者stopListen方法
     */
    public void waitJobsComplete() throws InterruptedException {
        WaitNode cdl = new WaitNode();
        queue.offer(cdl);
        cdl.await();
    }

    public void waitJobsComplete(long timeout, TimeUnit unit) throws InterruptedException {
        WaitNode cdl = new WaitNode();
        queue.offer(cdl);
        cdl.await(timeout, unit);
    }

    /**
     * 获取当前队列中的任务数
     *
     * @return
     */
    public int getJobCount() {
        AtomicInteger counter = new AtomicInteger();
        this.queue.forEach(job -> {
            if (!(job instanceof WaitNode)) {
                counter.incrementAndGet();
            }
        });
        return counter.get();
    }

    /**
     * 清空队列中的任务，如果有线程等待任务执行完毕，那么将会立即停止等待，并且抛出 JobCanceledException
     */
    public void clearJobs() {
        queue.removeIf(job -> {
            if (job instanceof WaitNode) {
                ((WaitNode) job).cancel();
            }
            return true;
        });
    }

    private void startListen() {
        while (!stopListen) {
            Job job;
            try {
                job = queue.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            try {
                job.execute();
            } catch (Throwable ignored) {
            }

            if (job instanceof ParseJob) {
                try {
                    Thread.sleep(sleepMill);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    private void checkStop() {
        if (stopped)
            throw new IllegalStateException("job manager has been stopped , can not add jobs any more");
    }

    private void checkCurrentThread() {
        if (Thread.currentThread() == listener) {
            throw new IllegalStateException("can no call this method in Job execute !");
        }
    }

    /**
     * https://stackoverflow.com/questions/10453876/how-do-i-cancel-a-countdownlatch
     */
    private static class WaitNode extends CountDownLatch implements Job {
        private boolean canceled = false;

        public WaitNode() {
            super(1);
        }

        public void cancel() {
            if (getCount() == 0)
                return;
            this.canceled = true;
            while (getCount() > 0)
                countDown();
        }


        @Override
        public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
            final boolean await = super.await(timeout, unit);
            if (canceled)
                throw new JobCanceledException();
            return await;
        }

        @Override
        public void await() throws InterruptedException {
            super.await();
            if (canceled)
                throw new JobCanceledException();
        }

        @Override
        public void execute() {
            countDown();
        }
    }

    public static class JobCanceledException extends RuntimeException {
        private JobCanceledException() {
        }
    }
}