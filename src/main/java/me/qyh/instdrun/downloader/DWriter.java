package me.qyh.instdrun.downloader;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

class DWriter<T> {

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path file;
    private List<T> ts;

    public DWriter(Path file) {
        this.file = file;
        this.ts = readTs();
    }

    public synchronized void consume(DConsumer<T> consumer) {
        if (consumer.consume(ts)) {
            write();
        }
    }

    private List<T> readTs() {
        if (!Files.exists(file)) {
            return new ArrayList<>();
        }
        String content;
        try {
            content = Files.readString(file);
        } catch (IOException ioException) {
            throw new UncheckedIOException(ioException);
        }
        if (content.isEmpty()) {
            return new ArrayList<>();
        }
        Type collectionType = TypeToken.getParameterized(List.class, getType()).getType();
        return gson.fromJson(content, collectionType);
    }

    protected Class<T> getType() {
        throw new UnsupportedOperationException();
    }

    protected void write() {
        Util.createFile(file);
        try {
            Files.writeString(file, gson.toJson(ts));
        } catch (IOException ex) {
            this.ts = readTs();
            throw new UncheckedIOException(ex);
        }
    }

    public interface DConsumer<T> {
        boolean consume(List<T> ts);
    }
}
