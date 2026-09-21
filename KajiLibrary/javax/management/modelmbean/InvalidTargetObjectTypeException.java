package javax.management.modelmbean;

/**
 * KajiLibrary's javax.management.modelmbean.InvalidTargetObjectTypeException -- the managed
 * resource is not of a type this MBean knows how to handle.
 *
 * <p>{@code setManagedResource} throws it when the second argument --the reference <b>type</b>--
 * is not one of those the implementation supports. The only one {@link RequiredModelMBean}
 * supports is {@code "ObjectReference"}: a direct Java reference.
 *
 * <p>The other types the specification names --{@code "Handle"}, {@code "IOR"},
 * {@code "EJBHandle"}, {@code "RMIReference"}-- are from a world that no longer exists. That
 * they are not supported is not a limitation of this library but of the JDK, which does not
 * support them either.
 */
public class InvalidTargetObjectTypeException extends Exception {

    private static final long serialVersionUID = 1190536278266811217L;

    /** Without detail. */
    public InvalidTargetObjectTypeException() {
        super("Invalid target object type exception");
    }

    /** With the type that was passed. */
    public InvalidTargetObjectTypeException(String s) {
        super("Invalid target object type exception: " + s);
    }

    /** With the cause and a message. */
    public InvalidTargetObjectTypeException(Exception e, String s) {
        super("Invalid target object type exception: " + s
            + ((e == null) ? "" : " " + e.toString()));
    }
}
