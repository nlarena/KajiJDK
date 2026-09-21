package javax.management;

/**
 * A query was applied to an MBean of a class it does not fit.
 *
 * <p>It has no message and no accessor: the offending value is kept but not published. It is like
 * that in the JDK and is respected -- adding a getter the JDK does not have would be invented API.
 */
public class InvalidApplicationException extends Exception {

    private static final long serialVersionUID = -3048022274675537269L;

    /**
     * @serial the object the query could not be applied to
     */
    private Object val;

    /** @param val the object the application failed on */
    public InvalidApplicationException(Object val) {
        this.val = val;
    }
}
