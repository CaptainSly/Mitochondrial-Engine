package com.duckcraftian.upf.system;

import org.tinylog.Level;
import org.tinylog.core.LogEntry;
import org.tinylog.writers.AbstractFormatPatternWriter;

import java.io.PrintStream;
import java.util.Map;

public class UPFConsoleWriter extends AbstractFormatPatternWriter {

    public static final String WHITE = "\u001B[37m";
    public static final String YELLOW = "\u001B[33m";
    public static final String GREEN = "\u001B[32m";
    public static final String RESET = "\u001B[0m";

    private PrintStream outStream;
    private PrintStream errStream;

    public UPFConsoleWriter(Map<String, String> properties) {
        super(properties);
        outStream = System.out;
        errStream = System.err;
    }

    @Override
    public void write(LogEntry logEntry) throws Exception {
        String text = render(logEntry);
        Level level = logEntry.getLevel();

        switch (level) {
            case DEBUG -> outStream.print(GREEN + text + RESET);
            case ERROR -> errStream.print(text);
            case WARN -> errStream.print(YELLOW + text + RESET);
            default -> outStream.print(WHITE + text + RESET);
        }

    }

    @Override
    public void flush() throws Exception {
        outStream.flush();
        errStream.flush();

    }

    @Override
    public void close() throws Exception {
    }
}