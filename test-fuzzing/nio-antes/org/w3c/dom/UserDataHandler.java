package org.w3c.dom;

/**
 * KajiLibrary's org.w3c.dom.UserDataHandler -- the notice that something happened to a node with
 * user data.
 *
 * <p>It is registered when keeping the datum, in {@link Node#setUserData}, and it serves for
 * resolving a question the DOM cannot answer on its own: if a node with a Java object hung on it is
 * **cloned**, should the clone have the same object, a copy, or nothing. It depends entirely on
 * what that object is --a cache is disposable, an identifier has to be copied, an open connection
 * is not duplicated-- and the only one who knows is whoever hung it. That is why the DOM copies
 * nothing on its own: it notifies, and the one notified decides.
 *
 * <p>The five reasons are 1 to 5 and come from the specification. Note which one does **not**
 * arrive: {@code NODE_DELETED} exists but the standard warns that implementations in languages with
 * garbage collection, Java included, typically **never** invoke it, because there is no defined
 * moment at which a node is destroyed. Counting on that notification to release a resource is
 * leaning on something that is not going to come.
 *
 * <p>The interface is declared whole.
 */
public interface UserDataHandler {

    /** The node was duplicated with {@link Node#cloneNode}. */
    public static final short NODE_CLONED = 1;

    /** The node was imported into another document with {@link Document#importNode}. */
    public static final short NODE_IMPORTED = 2;

    /**
     * The node was destroyed.
     *
     * <p>In Java it is typically never invoked: there is no defined moment at which a node dies.
     */
    public static final short NODE_DELETED = 3;

    /** The node was renamed with {@link Document#renameNode}. */
    public static final short NODE_RENAMED = 4;

    /** The node was adopted with {@link Document#adoptNode}. */
    public static final short NODE_ADOPTED = 5;

    /**
     * @param operation one of the {@code NODE_*}
     * @param key the key the datum was kept with
     * @param data the datum kept
     * @param src the node that was cloned, imported, renamed or adopted; {@code null} if it was
     *     deleted
     * @param dst the resulting node; {@code null} if it was deleted or renamed in place
     */
    public void handle(short operation, String key, Object data, Node src, Node dst);
}
