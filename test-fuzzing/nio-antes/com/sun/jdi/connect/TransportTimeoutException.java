package com.sun.jdi.connect;

import java.io.IOException;

/**
 * A transport operation's term ran out.
 *
 * <p>It comes out of the three places where a connector waits for the other end: attaching,
 * starting to listen, and accepting. It is an {@link IOException} because for the caller that
 * is what it is: a failure of the medium, not of the protocol.
 *
 * <p>It is worth telling it from "it could not connect": here the term is the **client's**,
 * and trying again with a longer one may work.
 */
public class TransportTimeoutException extends IOException {

    private static final long serialVersionUID = 4107035242623365074L;

    /** A timeout with no detail. */
    public TransportTimeoutException() {
        super();
    }

    /**
     * A timeout with a detail.
     *
     * @param message the detail
     */
    public TransportTimeoutException(String message) {
        super(message);
    }
}
