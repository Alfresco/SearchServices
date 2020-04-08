package org.alfresco.solr.client;

import java.io.BufferedReader;
import java.util.LinkedList;

import static java.util.stream.Collector.of;

import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;

/**
 * This is an enhanced {@link BufferedReader} used for transparently collect data from the incoming stream before
 * it gets consumed by the usual buffered reader logic. The data is not actually consumed, it is just buffered beside and
 * it can be printed in case we want to debug the underlying stream content.
 *
 * The name refers to the usage pattern: this reader is used for wrapping a character stream coming from a remote call and, in case
 * of issues, it collects the remaining (i.e. unread) part of the stream so it will be available for debugging purposes.
 *
 * It provides two different collecting modes:
 *
 * <ul>
 *     <li>
 *         windowing: the collected data is a window of the original stream (about 500 chars), and the character that
 *         caused a stop in the reading is more or less in the middle of that window.
 *     </li>
 *     <li>
 *         everything: the collected data is the whole character stream
 *     </li>
 * </ul>
 *
 * The two modes are activated on each instance depending on the level of the {@link Logger} passed in input.
 * Specifically:
 *
 * <ul>
 *     <li>DEBUG: enables the windowing mode</li>
 *     <li>TRACE: enables the "collect everything" mode</li>
 *     <li>other levels simply disables the buffering behaviour (i.e. nothing is collected)</li>
 * </ul>
 *
 */
public class LookAheadBufferedReader extends BufferedReader
{
    private interface BufferingMode
    {
        void append(char ch);

        void forceAppend(char ch);

        boolean canAccept(boolean force);
    }

    private static class Windowing implements BufferingMode
    {
        private final LinkedList<Character> window = new LinkedList<>();
        private final int maxSize;

        private Windowing(int maxSize)
        {
            this.maxSize = maxSize;
        }

        @Override
        public void append(char ch)
        {
            window.add(ch);
            if (window.size() == maxSize)
            {
                window.removeFirst();
            }
        }

        @Override
        public void forceAppend(char ch)
        {
            window.add(ch);
        }

        @Override
        public boolean canAccept(boolean force)
        {
            return window.size() < (maxSize * 2);
        }

        @Override
        public String toString()
        {
            return window.stream()
                    .collect(
                            of(
                                StringBuilder::new,
                                StringBuilder::append,
                                StringBuilder::append,
                                StringBuilder::toString));
        }
    }

    private static class WholeValue implements BufferingMode
    {
        private final StringBuilder content = new StringBuilder();

        @Override
        public void append(char ch)
        {
            content.append(ch);
        }

        @Override
        public void forceAppend(char ch)
        {
            content.append(ch);
        }

        @Override
        public String toString()
        {
            return content.toString();
        }

        @Override
        public boolean canAccept(boolean force)
        {
            return true;
        }
    }

    private static class NoOp implements BufferingMode
    {
        @Override
        public void append(char ch)
        {
            // Nothing to be done here
        }

        @Override
        public void forceAppend(char ch)
        {
            // Nothing to be done here
        }

        @Override
        public boolean canAccept(boolean force)
        {
            return false;
        }

        @Override
        public String toString()
        {
            return "Not available: please set the logging LEVEL to DEBUG or TRACE.";
        }
    }

    private final BufferingMode bufferingMode;

    public LookAheadBufferedReader(Reader in, Logger logger)
    {
        this(in, 250, logger);
    }

    public LookAheadBufferedReader(Reader in, final int windowSize, Logger logger)
    {
        super(in);
        if (logger.isTraceEnabled())
        {
            bufferingMode = new WholeValue();
        }
        else if(logger.isDebugEnabled())
        {
            bufferingMode = new Windowing(windowSize);
        }
        else {
            bufferingMode = new NoOp();
        }
    }

    @Override
    public int read() throws IOException
    {
        int ch  = super.read();
        bufferingMode.append((char)ch);
        return ch;
    }

    public String lookAheadAndGetBufferedContent()
    {
        try
        {
            int ch;
            while ((ch = super.read()) != -1 && bufferingMode.canAccept(true))
            {
                bufferingMode.forceAppend((char) ch);
            }
        }
        catch (Exception ignore)
        {
            // Ignore any I/O exception causing further reading on the underlying stream
            // Just return the collected data
        }
        return bufferingMode.toString();
    }
}