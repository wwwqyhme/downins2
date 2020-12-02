package me.qyh.instd4j.parser.job;

import java.util.concurrent.Callable;

public class ParseJob<T> implements Job {

    private final Callable<T> callable;
    private final JobConsumer<T> consumer;

    public ParseJob(Callable<T> callable, JobConsumer<T> consumer) {
        this.callable = callable;
        this.consumer = consumer;
    }

    @Override
    public void execute() {
        try {
            T rst = callable.call();
            consumer.accept(rst);
        } catch (Exception e) {
            consumer.onError(e);
        } catch (Throwable e) {
            consumer.onError(new RuntimeException(e));
        }
    }
}
