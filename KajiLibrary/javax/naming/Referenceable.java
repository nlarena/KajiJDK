package javax.naming;

/**
 * Implemented by the object that knows how to say how to rebuild itself.
 *
 * <p>Binding a live object in a naming service is not possible: the service stores data, not
 * another process's memory. What gets stored is a `Reference` --the name of a factory class plus a
 * couple of addresses-- and whoever later does a `lookup` receives it and **rebuilds** the object.
 * A `DataSource`, for example, is bound as "this factory, with this URL and this user".
 *
 * <p>The difference from `Serializable` is that the object drives the rebuilding, not the
 * mechanism: `getReference()` decides which data are needed, and they are usually far fewer than
 * the full state.
 */
public interface Referenceable {

    Reference getReference() throws NamingException;
}
