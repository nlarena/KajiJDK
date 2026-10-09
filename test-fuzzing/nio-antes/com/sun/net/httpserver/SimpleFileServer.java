package com.sun.net.httpserver;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Path;

/**
 * The file server that backs the {@code jwebserver} command.
 *
 * <h2>What it is and what it is not</h2>
 *
 * <p>It is for serving a directory in a test or a demo, and its documentation says so plainly:
 * <strong>it is not meant for production</strong>. It only attends {@code GET} and
 * {@code HEAD}, it has neither authentication nor encryption, and it interprets nothing -- it
 * serves bytes.
 *
 * <p>The path it receives has to be <strong>absolute</strong>, and that is not formalism: it is
 * what fixes the root each request is resolved against, that is, the only thing that keeps a
 * {@code ../../} from taking the rest of the disk away.
 *
 * <p>With no server provider installed, {@link #createFileServer} throws
 * {@link UnsupportedOperationException} -- see {@link HttpServer}.
 */
public final class SimpleFileServer {

    private SimpleFileServer() {
    }

    /**
     * How much the output filter records.
     *
     * <p>{@link #NONE} is not the same as setting no filter: it goes on existing in the chain, and
     * the difference shows if somebody walks it.
     */
    public enum OutputLevel {

        /** Nothing. */
        NONE,
        /** One line per request: method, URI, code. */
        INFO,
        /** Besides, every header of the request and of the response. */
        VERBOSE
    }

    /**
     * A server that serves {@code rootDirectory}.
     *
     * @throws IllegalArgumentException if the path is not absolute or is not a directory
     * @throws UnsupportedOperationException if there is no server provider
     */
    public static HttpServer createFileServer(InetSocketAddress addr, Path rootDirectory,
            OutputLevel outputLevel) {
        throw new UnsupportedOperationException(
                "this VM brings no HttpServer provider; see com.sun.net.httpserver.spi");
    }

    /**
     * Only the handler, in order to mount it on a path of a server of one's own.
     *
     * @throws IllegalArgumentException if the path is not absolute or is not a directory
     */
    public static HttpHandler createFileHandler(Path rootDirectory) {
        throw new UnsupportedOperationException(
                "this VM does not implement jwebserver's file handler");
    }

    /**
     * Only the logging filter, which serves for any handler and not only for this one.
     *
     * @throws NullPointerException if the output or the level are missing
     */
    public static Filter createOutputFilter(OutputStream out, OutputLevel outputLevel) {
        throw new UnsupportedOperationException(
                "this VM does not implement jwebserver's logging filter");
    }
}
