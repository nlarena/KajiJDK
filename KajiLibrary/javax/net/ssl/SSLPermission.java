package javax.net.ssl;

import java.security.BasicPermission;

/**
 * The permission over sensitive SSL operations.
 *
 * <p>The two defined names are {@code "setHostnameVerifier"} and {@code "getSSLSessionContext"},
 * and the first explains why this exists: whoever can change the name verifier can make it accept
 * any certificate, that is turn off half of TLS's protection without anything else changing.
 */
public final class SSLPermission extends BasicPermission {

    private static final long serialVersionUID = -3456898025505876775L;

    /** A permission with that name. */
    public SSLPermission(String name) {
        super(name);
    }

    /** The same; {@code actions} is ignored, and the JDK does the same. */
    public SSLPermission(String name, String actions) {
        super(name, actions);
    }
}
