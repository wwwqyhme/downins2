package me.qyh.instd4j.parser.job;

import java.util.function.Consumer;

public interface JobConsumer<T> extends Consumer<T> {

    void onError(Exception ex);

}
