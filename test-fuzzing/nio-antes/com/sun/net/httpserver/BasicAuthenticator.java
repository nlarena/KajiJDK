package com.sun.net.httpserver;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * HTTP basic authentication: user and password in a header.
 *
 * <h2>What has to be known before using it</h2>
 *
 * <p>The password travels in <strong>Base64, which is not encryption</strong>: it is a
 * reversible encoding anybody undoes from memory. Over plain HTTP, sending basic authentication
 * is sending the password in plain text. It only makes sense over TLS.
 *
 * <p>And it goes in <em>every</em> request, not only in the first: there is no session, so the
 * password is repeated indefinitely for as long as the browsing lasts.
 *
 * <h2>The only thing that has to be written</h2>
 *
 * <p>{@link #checkCredentials}. Everything else -- parsing the header, decoding, building the
 * challenge with the realm, telling "it sent nothing" from "it sent something wrong" -- is
 * already there.
 *
 * <h2>The character set</h2>
 *
 * <p>The two-argument constructor exists because the original specification did not say how to
 * encode the non-ASCII ones, and each client did its own thing. Fixing it at UTF-8 is what is
 * right today; the one-argument constructor uses that same value.
 */
public abstract class BasicAuthenticator extends Authenticator {

    /** The realm that is announced in the challenge. */
    protected final String realm;

    private final Charset charset;

    /**
     * In that realm, with UTF-8.
     *
     * @throws IllegalArgumentException if the realm is empty or has non-ASCII characters -- it
     *     travels in a header, and nothing else fits there
     */
    public BasicAuthenticator(String realm) {
        this(realm, StandardCharsets.UTF_8);
    }

    /**
     * In that realm, with that character set for decoding the credentials.
     *
     * @throws NullPointerException if either is {@code null}
     * @throws IllegalArgumentException if the realm is empty or is not ASCII
     */
    public BasicAuthenticator(String realm, Charset charset) {
        if (realm == null) {
            throw new NullPointerException("realm");
        }
        if (charset == null) {
            throw new NullPointerException("charset");
        }
        if (realm.isEmpty()) {
            throw new IllegalArgumentException("the realm cannot be empty");
        }
        for (int i = 0; i < realm.length(); i++) {
            if (realm.charAt(i) > 127) {
                throw new IllegalArgumentException("the realm has to be ASCII: " + realm);
            }
        }
        this.realm = realm;
        this.charset = charset;
    }

    /** The realm. */
    public String getRealm() {
        return this.realm;
    }

    /**
     * It parses the header and consults {@link #checkCredentials}.
     *
     * <p>It returns {@link Retry} when no credential came or it came malformed, and also when the
     * credential does not validate. The second is on purpose and not an oversight: a
     * {@link Failure} would tell the browser to stop trying, when what is right is to ask for the
     * password again.
     */
    public Result authenticate(HttpExchange t) {
        String header = t.getRequestHeaders().getFirst("Authorization");
        if (header == null) {
            return challenge(t);
        }
        int sp = header.indexOf(' ');
        if (sp == -1 || !header.substring(0, sp).equalsIgnoreCase("Basic")) {
            return challenge(t);
        }
        byte[] raw;
        try {
            raw = Base64.getDecoder().decode(header.substring(sp + 1).trim());
        } catch (IllegalArgumentException e) {
            return challenge(t);
        }
        String userpass = new String(raw, this.charset);
        // The FIRST `:`, not the last: a password may contain a colon and a user may not.
        int cut = userpass.indexOf(':');
        if (cut == -1) {
            return challenge(t);
        }
        String user = userpass.substring(0, cut);
        String key = userpass.substring(cut + 1);
        if (!checkCredentials(user, key)) {
            return challenge(t);
        }
        return new Success(new HttpPrincipal(user, this.realm));
    }

    private Result challenge(HttpExchange t) {
        t.getResponseHeaders().set("WWW-Authenticate",
                "Basic realm=\"" + this.realm + "\", charset=\"" + this.charset.name() + "\"");
        return new Retry(401);
    }

    /**
     * Whether those credentials are valid.
     *
     * <p>It is best to compare them in constant time: a {@code String}'s {@code equals} stops at
     * the first difference, and that allows how many characters were right to be measured.
     */
    public abstract boolean checkCredentials(String username, String password);
}
