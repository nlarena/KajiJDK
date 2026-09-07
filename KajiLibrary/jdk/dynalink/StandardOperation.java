package jdk.dynalink;

/**
 * The five verbs dynalink knows how to link.
 *
 * <p>On their own they say nothing: a `GET` with no {@link Namespace} does not specify what is being
 * read. The complete form is assembled by composition —
 * `StandardOperation.GET.withNamespace(StandardNamespace.PROPERTY)` — and that is why the enum has
 * not a single method of its own.
 *
 * @since 9
 */
public enum StandardOperation implements Operation {

    /** Read a value from the given namespace. */
    GET,

    /** Write a value into the given namespace. */
    SET,

    /** Remove a member from the given namespace. */
    REMOVE,

    /** Invoke the receiver object. */
    CALL,

    /** Build an instance with the receiver object as the constructor. */
    NEW
}
