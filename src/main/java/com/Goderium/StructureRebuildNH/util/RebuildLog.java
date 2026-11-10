package com.Goderium.StructureRebuildNH.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

/** Buffered per-operation diagnostics. Logging failures never change a rebuild's protection or write decisions. */
public final class RebuildLog implements AutoCloseable {
    private static final AtomicLong IDS = new AtomicLong();
    private final long started = System.nanoTime();
    private BufferedWriter writer;
    private File file;
    private String failure;
    private long lines;
    public RebuildLog(File directory, boolean enabled) {
        if (!enabled) return;
        try {
            Files.createDirectories(directory.toPath());
            String stamp = new SimpleDateFormat("yyyyMMdd-HHmmss-SSS", Locale.ROOT).format(new Date());
            file = new File(directory, "rebuild-" + stamp + "-" + IDS.incrementAndGet() + ".log");
            writer = new BufferedWriter(Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE), 65536);
        } catch (IOException error) { failed(error); }
    }
    public boolean enabled() { return writer != null; }
    public String path() { return file == null ? "disabled" : file.getAbsolutePath(); }
    public String failure() { return failure; }
    public long elapsedMillis() { return (System.nanoTime() - started) / 1000000; }
    public void line(String message) {
        if (writer == null) return;
        try {
            writer.write("t+" + elapsedMillis() + "ms " + message.replace("\r", "\\r").replace("\n", "\\n"));
            writer.newLine();
            lines++;
        } catch (IOException error) { failed(error); }
    }
    public void checkpoint(String message) {
        line(message);
        if (writer != null) try { writer.flush(); } catch (IOException error) { failed(error); }
    }
    public void error(Throwable error) {
        StringWriter text = new StringWriter();
        error.printStackTrace(new PrintWriter(text));
        for (String line : text.toString().split("\\r?\\n")) line("ERROR " + line);
        checkpoint("ERROR_END");
    }
    private void failed(IOException error) {
        if (failure == null) failure = error.toString();
        if (writer != null) try { writer.close(); } catch (IOException ignored) {}
        writer = null;
    }
    @Override public void close() {
        if (writer == null) return;
        checkpoint("LOG_END lines=" + lines);
        if (writer != null) try { writer.close(); } catch (IOException error) { failed(error); }
        writer = null;
    }
}
