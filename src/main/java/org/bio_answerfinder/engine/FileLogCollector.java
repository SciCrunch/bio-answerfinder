package org.bio_answerfinder.engine;

import org.bio_answerfinder.common.CharSetEncoding;
import org.bio_answerfinder.util.FileUtils;
import org.bio_answerfinder.util.GenUtils;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;

/**
 * Created by bozyurt on 7/17/19.
 */
public class FileLogCollector implements ILogCollector, Closeable {
    BufferedWriter out;

    public FileLogCollector(String logFile) throws IOException {
        out = FileUtils.getBufferedWriter(logFile, CharSetEncoding.UTF8);
    }

    public FileLogCollector(BufferedWriter out) {
        this.out = out;
    }

    public void shutdown() {
        FileUtils.close(out);
    }

    @Override
    public void log(String msg) {
        if (!GenUtils.isEmpty(msg)) {
            try {
                out.write(msg);
                out.newLine();
                out.flush();
            } catch (IOException x) {
                //
            }
        }
    }

    @Override
    public void close() throws IOException {
        FileUtils.close(out);
    }
}
