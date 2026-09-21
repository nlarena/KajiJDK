package java.rmi.server;

/**
 * It decides what to do when the runtime could not create a socket.
 *
 * <p>It exists because running out of sockets is usually <strong>transient</strong>: a spike of
 * connections exhausts the descriptors and a second later there is room. Without this hook, RMI
 * would have to choose up front between giving up —losing a server to a spike— or retrying forever.
 * Returning {@code true} is asking for a retry.
 */
public interface RMIFailureHandler {

    /** @return {@code true} to retry, {@code false} to give up */
    boolean failure(Exception ex);
}
