/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package org.apache.logging.log4j.core.appender;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Objects;

import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.layout.ByteBufferDestination;
import org.apache.logging.log4j.core.util.Constants;

/**
 * Manages an OutputStream so that it can be shared by multiple Appenders and will
 * allow appenders to reconfigure without requiring a new stream.
 */
public class OutputStreamManager extends AbstractManager implements ByteBufferDestination {
    protected final Layout<?> layout;
    protected ByteBuffer byteBuffer;
    private volatile OutputStream os;
    private boolean skipFooter = false;

    protected OutputStreamManager(final OutputStream os, final String streamName, final Layout<?> layout,
            final boolean writeHeader) {
        this(os, streamName, layout, writeHeader, ByteBuffer.wrap(new byte[Constants.ENCODER_BYTE_BUFFER_SIZE]));
    }

    /**
     *
     * @param os
     * @param streamName
     * @param layout
     * @param writeHeader
     * @param byteBuffer
     * @since 2.6
     */
    protected OutputStreamManager(final OutputStream os, final String streamName, final Layout<?> layout,
            final boolean writeHeader, final ByteBuffer byteBuffer) {
        super(streamName);
        this.os = os;
        this.layout = layout;
        if (writeHeader && layout != null) {
            final byte[] header = layout.getHeader();
            if (header != null) {
                try {
                    this.os.write(header, 0, header.length);
                } catch (final IOException e) {
                    logError("unable to write header", e);
                }
            }
        }
        this.byteBuffer = Objects.requireNonNull(byteBuffer, "byteBuffer");
    }

    /**
     * Creates a Manager.
     *
     * @param name The name of the stream to manage.
     * @param data The data to pass to the Manager.
     * @param factory The factory to use to create the Manager.
     * @param <T> The type of the OutputStreamManager.
     * @return An OutputStreamManager.
     */
    public static <T> OutputStreamManager getManager(final String name, final T data,
                                                 final ManagerFactory<? extends OutputStreamManager, T> factory) {
        return AbstractManager.getManager(name, factory, data);
    }

    /**
     * Indicate whether the footer should be skipped or not.
     * @param skipFooter true if the footer should be skipped.
     */
    public void skipFooter(boolean skipFooter) {
        this.skipFooter = skipFooter;
    }

    /**
     * Default hook to write footer during close.
     */
    @Override
    public void releaseSub() {
        writeFooter();
        close();
    }

    /**
     * Writes the footer.
     */
    protected void writeFooter() {
        if (layout == null || skipFooter) {
            return;
        }
        final byte[] footer = layout.getFooter();
        if (footer != null) {
            write(footer);
        }
    }

    /**
     * Returns the status of the stream.
     * @return true if the stream is open, false if it is not.
     */
    public boolean isOpen() {
        return getCount() > 0;
    }

    protected OutputStream getOutputStream() {
        return os;
    }

    protected void setOutputStream(final OutputStream os) {
        final byte[] header = layout.getHeader();
        if (header != null) {
            try {
                os.write(header, 0, header.length);
                this.os = os; // only update field if os.write() succeeded
            } catch (final IOException ioe) {
                logError("unable to write header", ioe);
            }
        } else {
            this.os = os;
        }
    }

    /**
     * Some output streams synchronize writes while others do not.
     * @param bytes The serialized Log event.
     * @throws AppenderLoggingException if an error occurs.
     */
    protected void write(final byte[] bytes)  {
        write(bytes, 0, bytes.length, false);
    }

    /**
     * Some output streams synchronize writes while others do not.
     * @param bytes The serialized Log event.
     * @param immediateFlush If true, flushes after writing.
     * @throws AppenderLoggingException if an error occurs.
     */
    protected void write(final byte[] bytes, final boolean immediateFlush)  {
        write(bytes, 0, bytes.length, immediateFlush);
    }

    /**
     * Some output streams synchronize writes while others do not. Synchronizing here insures that
     * log events won't be intertwined.
     * @param bytes The serialized Log event.
     * @param offset The offset into the byte array.
     * @param length The number of bytes to write.
     * @throws AppenderLoggingException if an error occurs.
     */
    protected void write(final byte[] bytes, final int offset, final int length) {
        write(bytes, offset, length, false);
    }

    /**
     * Some output streams synchronize writes while others do not. Synchronizing here insures that
     * log events won't be intertwined.
     * @param bytes The serialized Log event.
     * @param offset The offset into the byte array.
     * @param length The number of bytes to write.
     * @param immediateFlush flushes immediately after writing.
     * @throws AppenderLoggingException if an error occurs.
     */
    protected synchronized void write(final byte[] bytes, final int offset, final int length, final boolean immediateFlush) {
        if (immediateFlush && byteBuffer.position() == 0) {
            writeToDestination(bytes, offset, length);
            flushDestination();
            return;
        }
        if (length >= byteBuffer.capacity()) {
            // if request length exceeds buffer capacity, flush the buffer and write the data directly
            flush();
            writeToDestination(bytes, offset, length);
        } else {
            if (length > byteBuffer.remaining()) {
                flush();
            }
            byteBuffer.put(bytes, offset, length);
        }
        if (immediateFlush) {
            flush();
        }
    }

    /**
     * Writes the specified section of the specified byte array to the stream.
     *
     * @param bytes the array containing data
     * @param offset from where to write
     * @param length how many bytes to write
     * @since 2.6
     */
    protected synchronized void writeToDestination(final byte[] bytes, final int offset, final int length) {
        try {
            os.write(bytes, offset, length);
        } catch (final IOException ex) {
            final String msg = "Error writing to stream " + getName();
            throw new AppenderLoggingException(msg, ex);
        }
    }

    /**
     * Calls {@code flush()} on the underlying output stream.
     * @since 2.6
     */
    protected synchronized void flushDestination() {
        try {
            os.flush();
        } catch (final IOException ex) {
            final String msg = "Error flushing stream " + getName();
            throw new AppenderLoggingException(msg, ex);
        }
    }

    /**
     * Drains the ByteBufferDestination's buffer into the destination. By default this calls
     * {@link OutputStreamManager#write(byte[], int, int, boolean)} with the buffer contents.
     * The underlying stream is not {@linkplain OutputStream#flush() flushed}.
     *
     * @see #flushDestination()
     * @since 2.6
     */
    protected synchronized void flushBuffer(final ByteBuffer buf) {
        buf.flip();
        if (buf.limit() > 0) {
            writeToDestination(buf.array(), 0, buf.limit());
        }
        buf.clear();
    }

    /**
     * Flushes any buffers.
     */
    public synchronized void flush() {
        flushBuffer(byteBuffer);
        flushDestination();
    }

    protected synchronized void close() {
        flush();
        final OutputStream stream = os; // access volatile field only once per method
        if (stream == System.out || stream == System.err) {
            return;
        }
        try {
            stream.close();
        } catch (final IOException ex) {
            logError("unable to close stream", ex);
        }
    }

    /**
     * Returns this {@code ByteBufferDestination}'s buffer.
     * @return the buffer
     * @since 2.6
     */
    @Override
    public ByteBuffer getByteBuffer() {
        return byteBuffer;
    }

    /**
     * Drains the ByteBufferDestination's buffer into the destination. By default this calls
     * {@link #flushBuffer(ByteBuffer)} with the specified buffer. Subclasses may override.
     * <p>
     * Do not call this method lightly! For some subclasses this is a very expensive operation. For example,
     * {@link MemoryMappedFileManager} will assume this method was called because the end of the mapped region
     * was reached during a text encoding operation and will {@linkplain MemoryMappedFileManager#remap() remap} its
     * buffer.
     * </p><p>
     * To just flush the buffered contents to the underlying stream, call
     * {@link #flushBuffer(ByteBuffer)} directly instead.
     * </p>
     *
     * @param buf the buffer whose contents to write the the destination
     * @return the specified buffer
     * @since 2.6
     */
    @Override
    public ByteBuffer drain(final ByteBuffer buf) {
        flushBuffer(buf);
        return buf;
    }
}
