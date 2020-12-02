package me.qyh.instdrun.downloader;

import java.nio.file.Path;

class ErrorFileWriter extends DWriter<ErrorFile> {

    public ErrorFileWriter(Path file) {
        super(file);
    }

    @Override
    protected Class<ErrorFile> getType() {
        return ErrorFile.class;
    }

    public synchronized void delete(ErrorFile errorFile) {
        super.consume(files->{
            files.removeIf(e -> e.equals(errorFile));
            return true;
        });
    }
}
