package me.qyh.instdrun.downloader;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Util {

    private Util() {
        super();
    }

    public static void createFile(Path file)  {
        if (!Files.exists(file)) {
            synchronized (Util.class) {
                if (!Files.exists(file)) {
                    try {
                        Files.createDirectories(file.getParent());
                        Files.createFile(file);
                    } catch (IOException ioException) {
                        throw new UncheckedIOException(ioException);
                    }
                }
            }
        }
    }
}
