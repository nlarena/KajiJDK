package java.net;

import java.io.IOException;

/**
 * KajiLibrary's java.net.UnknownServiceException -- the connection does not support the operation it
 * was asked for.
 *
 * <p>The distinction from {@link java.net.MalformedURLException} is the one worth keeping clear, and
 * it is the reason they are two exceptions and not one: that one says **the URL is not understood**;
 * this one says it was understood perfectly and that **there is no way to serve it**. A misspelt
 * `http://example` is the first; a well-formed `http://example` in a library that ships no HTTP client
 * is the second. Whoever confuses them will look for the error in the wrong place.
 *
 * <p>It is what {@link URL#openStream()} throws for every scheme other than `file:`.
 */
public class UnknownServiceException extends IOException {

    public UnknownServiceException() {
        super();
    }

    public UnknownServiceException(String msg) {
        super(msg);
    }
}
