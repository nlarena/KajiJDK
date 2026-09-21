package javax.naming.event;

/**
 * KajiLibrary's javax.naming.event.ObjectChangeListener -- changes in an entry's <b>content</b>.
 *
 * <p>The flip side of {@link NamespaceChangeListener}: the entry is still where it was and with the
 * same name, what changed is what it holds -- its attributes, or the object it is bound to.
 *
 * <p>The event carries the old and the new binding, and comparing them is the only way to know what
 * changed: the API sends no delta. With both, a listener can decide whether the change matters to
 * it without querying the directory again.
 */
public interface ObjectChangeListener extends NamingListener {

    /** The content of an entry changed. */
    void objectChanged(NamingEvent evt);
}
