package com.sun.net.httpserver;

/**
 * It decides whether a request may pass, and with what identity.
 *
 * <h2>Why the result is an object and not a boolean</h2>
 *
 * <p>Because authenticating has <strong>three</strong> answers, not two, and the third is the
 * one that makes HTTP work. A request may come well ({@link Success}), come badly with no
 * possible fix ({@link Failure}) or come <em>with no credentials yet</em> -- which is not a
 * rejection but the normal first step: the browser sends the bare request, the server answers
 * with a challenge, and only the second request brings the password. That is {@link Retry}.
 *
 * <p>Confusing {@code Retry} with {@code Failure} breaks the whole authentication: the client
 * never receives the challenge and never knows it had to send something.
 *
 * <p>The other half of the answer is the <strong>identity</strong>. {@link Success} carries an
 * {@link HttpPrincipal} because knowing that somebody is legitimate is not enough: the handler
 * needs to know who they are in order to decide what they may do.
 */
public abstract class Authenticator {

    /** For the implementations. */
    protected Authenticator() {
    }

    /**
     * The result of authenticating.
     *
     * <p>Abstract with a {@code protected} constructor and three fixed subclasses, which is the
     * way of writing a sum type in Java without sealing it: nobody from outside may add a fourth
     * case, so the server may hand out by type with the certainty of having covered them all.
     */
    public abstract static class Result {

        protected Result() {
        }
    }

    /** The request is authenticated, and this is the identity. */
    public static class Success extends Result {

        private final HttpPrincipal principal;

        public Success(HttpPrincipal p) {
            this.principal = p;
        }

        /** Who sent the request. */
        public HttpPrincipal getPrincipal() {
            return this.principal;
        }
    }

    /**
     * The request is not authenticated and there is nothing to retry.
     *
     * <p>Different from {@link Retry}: here the credentials arrived and were wrong, or the
     * resource is not accessible to anybody. Sending a challenge would be an invitation to try
     * again with the same.
     */
    public static class Failure extends Result {

        private final int responseCode;

        public Failure(int responseCode) {
            this.responseCode = responseCode;
        }

        /** The HTTP code to return. */
        public int getResponseCode() {
            return this.responseCode;
        }
    }

    /**
     * Credentials are missing: the client has to be challenged and hopefully comes back.
     *
     * <p>The normal case of the first request. The handler does <strong>not</strong> run: the
     * server answers with the code and the challenge headers the authenticator has already set in
     * the response.
     */
    public static class Retry extends Result {

        private final int responseCode;

        public Retry(int responseCode) {
            this.responseCode = responseCode;
        }

        /** The challenge's HTTP code, typically {@code 401}. */
        public int getResponseCode() {
            return this.responseCode;
        }
    }

    /**
     * It authenticates the request.
     *
     * <p>It may write headers into the response -- it is how {@link Retry}'s challenge is sent --
     * but it must not write the body nor close it.
     */
    public abstract Result authenticate(HttpExchange exch);
}
