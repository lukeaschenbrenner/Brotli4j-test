/* Copyright 2017 Google Inc. All Rights Reserved.

   Distributed under MIT license.
   See file LICENSE for detail or copy at https://opensource.org/licenses/MIT
*/
package com.aayushatharva.brotli4j.decoder;

import com.aayushatharva.brotli4j.common.annotations.Local;
import com.aayushatharva.brotli4j.common.annotations.Upstream;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;

/**
 * Base class for InputStream / Channel implementations.
 */
@Upstream
@Local
public class Decoder {
    private static final ByteBuffer EMPTY_BUFFER = ByteBuffer.allocate(0);
    private final ReadableByteChannel source;
    private final DecoderJNI.Wrapper decoder;
    ByteBuffer buffer;
    boolean closed;
    boolean eager;

    /**
     * Creates a Decoder wrapper.
     *
     * @param source          underlying source
     * @param inputBufferSize read buffer size
     * @throws IOException If any failure during initialization
     */
    public Decoder(ReadableByteChannel source, int inputBufferSize)
            throws IOException {
        if (inputBufferSize <= 0) {
            throw new IllegalArgumentException("buffer size must be positive");
        }
        if (source == null) {
            throw new NullPointerException("source can not be null");
        }
        this.source = source;
        this.decoder = new DecoderJNI.Wrapper(inputBufferSize);
    }

    /**
     * Decodes the given data buffer.
     *
     * @param data byte array of data to be decoded
     * @return {@link DirectDecompress} instance
     * @throws IOException If an error occurs during decoding
     */
    @Local
    public static DirectDecompress decompress(byte[] data) throws IOException {
        DecoderJNI.Wrapper decoder = new DecoderJNI.Wrapper(data.length);
        ArrayList<byte[]> output = new ArrayList<>();
        int totalOutputSize = 0;
        try {
            decoder.getInputBuffer().put(data);
            decoder.push(data.length);
            while (decoder.getStatus() != DecoderJNI.Status.DONE) {
                switch (decoder.getStatus()) {
                    case OK:
                        decoder.push(0);
                        break;

                    case NEEDS_MORE_OUTPUT:
                        ByteBuffer buffer = decoder.pull();
                        byte[] chunk = new byte[buffer.remaining()];
                        buffer.get(chunk);
                        output.add(chunk);
                        totalOutputSize += chunk.length;
                        break;

                    case NEEDS_MORE_INPUT:
                        // Give decoder a chance to process the remaining of the buffered byte.
                        decoder.push(0);
                        // If decoder still needs input, this means that stream is truncated.
                        if (decoder.getStatus() == DecoderJNI.Status.NEEDS_MORE_INPUT) {
                            return new DirectDecompress(decoder.getStatus(), null, null);
                        }
                        break;

                    default:
                        return new DirectDecompress(decoder.getStatus(), null, null);
                }
            }
        } finally {
            decoder.destroy();
        }
        if (output.size() == 1) {
            return new DirectDecompress(decoder.getStatus(), output.get(0), null);
        }
        byte[] result = new byte[totalOutputSize];
        int offset = 0;
        for (byte[] chunk : output) {
            System.arraycopy(chunk, 0, result, offset, chunk.length);
            offset += chunk.length;
        }
        return new DirectDecompress(decoder.getStatus(), result, null);
    }

    @Upstream
    private void fail(String message) throws IOException {
        try {
            close();
        } catch (IOException ex) {
            /* Ignore */
        }
        throw new IOException(message);
    }

    @Upstream
    void attachDictionary(ByteBuffer dictionary) throws IOException {
        if (!decoder.attachDictionary(dictionary)) {
            fail("failed to attach dictionary");
        }
    }

    @Upstream
    public void enableEagerOutput() {
        this.eager = true;
    }

    /**
     * Continue decoding.
     *
     * @return -1 if stream is finished, or number of bytes available in read buffer (> 0)
     */
    @Upstream
    int decode() throws IOException {
        while (true) {
            if (buffer != null) {
                if (!buffer.hasRemaining()) {
                    buffer = null;
                } else {
                    return buffer.remaining();
                }
            }

            switch (decoder.getStatus()) {
                case DONE:
                    return -1;

                case OK:
                    decoder.push(0);
                    break;

                case NEEDS_MORE_INPUT:
                    // In "eager" more pulling preempts pushing.
                    if (eager && decoder.hasOutput()) {
                        buffer = decoder.pull();
                        break;
                    }
                    ByteBuffer inputBuffer = decoder.getInputBuffer();
                    ((Buffer) inputBuffer).clear();
                    int bytesRead = source.read(inputBuffer);
                    if (bytesRead == -1) {
                        fail("unexpected end of input");
                    }
                    if (bytesRead == 0) {
                        // No input data is currently available.
                        buffer = EMPTY_BUFFER;
                        return 0;
                    }
                    decoder.push(bytesRead);
                    break;

                case NEEDS_MORE_OUTPUT:
                    buffer = decoder.pull();
                    break;

                default:
                    fail("corrupted input");
            }
        }
    }

    @Upstream
    void discard(int length) {
        ((Buffer) buffer).position(buffer.position() + length);
        if (!buffer.hasRemaining()) {
            buffer = null;
        }
    }

    @Upstream
    int consume(ByteBuffer dst) {
        ByteBuffer slice = buffer.slice();
        int limit = Math.min(slice.remaining(), dst.remaining());
        ((Buffer) slice).limit(limit);
        dst.put(slice);
        discard(limit);
        return limit;
    }

    @Upstream
    void close() throws IOException {
        if (closed) {
            return;
        }
        closed = true;
        decoder.destroy();
        source.close();
    }

    /** Decodes the given data buffer starting at offset till length. */
    @Upstream
    public static byte[] decompress(byte[] data, int offset, int length) throws IOException {
        DecoderJNI.Wrapper decoder = new DecoderJNI.Wrapper(length);
        ArrayList<byte[]> output = new ArrayList<>();
        int totalOutputSize = 0;
        try {
            decoder.getInputBuffer().put(data, offset, length);
            decoder.push(length);
            while (decoder.getStatus() != DecoderJNI.Status.DONE) {
                switch (decoder.getStatus()) {
                    case OK:
                        decoder.push(0);
                        break;

                    case NEEDS_MORE_OUTPUT:
                        ByteBuffer buffer = decoder.pull();
                        byte[] chunk = new byte[buffer.remaining()];
                        buffer.get(chunk);
                        output.add(chunk);
                        totalOutputSize += chunk.length;
                        break;

                    case NEEDS_MORE_INPUT:
                        // Give decoder a chance to process the remaining of the buffered byte.
                        decoder.push(0);
                        // If decoder still needs input, this means that stream is truncated.
                        if (decoder.getStatus() == DecoderJNI.Status.NEEDS_MORE_INPUT) {
                            throw new IOException("corrupted input");
                        }
                        break;

                    default:
                        throw new IOException("corrupted input");
                }
            }
        } finally {
            decoder.destroy();
        }
        if (output.size() == 1) {
            return output.get(0);
        }
        byte[] result = new byte[totalOutputSize];
        int resultOffset = 0;
        for (byte[] chunk : output) {
            System.arraycopy(chunk, 0, result, resultOffset, chunk.length);
            resultOffset += chunk.length;
        }
        return result;
    }
}
