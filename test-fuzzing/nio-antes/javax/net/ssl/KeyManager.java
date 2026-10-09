package javax.net.ssl;

/**
 * Whoever provides the own credentials during a handshake: the certificate presented and the
 * private key backing it.
 *
 * <p>It declares no method, and that is not an oversight. The credentials depend on the kind of
 * authentication --X.509, Kerberos, PSK-- and each needs different questions, so the common
 * interface can have none. What it does is <strong>mark</strong>: it is the type
 * {@link SSLContext#init} accepts, and whoever really implements it does so through a subinterface
 * such as {@link X509KeyManager}.
 */
public interface KeyManager {
}
