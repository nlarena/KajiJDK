package javax.management;

import java.io.Serializable;

/**
 * Open metadata that hangs from any piece of an {@link MBeanInfo}.
 *
 * <p>It is the model's escape valve: {@code MBeanInfo} and friends declare what JMX knows how to
 * name --type, readable, writable-- and everything else (units, whether the value is cumulative,
 * whether it is worth showing, whatever occurs to the modeller) comes in here as named fields.
 *
 * <p><b>Field names are case-insensitive.</b> It is the rule most often forgotten: asking for
 * {@code "units"} and {@code "Units"} gives the same, and a descriptor cannot have both.
 */
public interface Descriptor extends Serializable, Cloneable {

    /**
     * The value of the field, or {@code null} if it is not there.
     *
     * @throws RuntimeOperationsException if the name is null or empty
     */
    Object getFieldValue(String fieldName) throws RuntimeOperationsException;

    /**
     * Sets a field.
     *
     * @throws RuntimeOperationsException if the name is not valid, or if the descriptor is
     *     immutable
     */
    void setField(String fieldName, Object fieldValue) throws RuntimeOperationsException;

    /** All the fields as {@code "name=value"}. */
    String[] getFields();

    /** Only the names. */
    String[] getFieldNames();

    /**
     * The values of the given names, in the same order.
     *
     * <p>With a {@code null} argument it returns <b>all</b> the values: that is the variant used
     * together with {@link #getFieldNames()}. Called with no arguments at all, the varargs array is
     * empty and so is the result. (An earlier note said the no-argument call returns everything; it
     * does not, here or in the JDK.)
     */
    Object[] getFieldValues(String... fieldNames);

    /** Removes a field; if it was not there, does nothing. */
    void removeField(String fieldName);

    /**
     * Sets several fields at once.
     *
     * @throws RuntimeOperationsException if the arrays do not have the same length or a name is not
     *     valid
     */
    void setFields(String[] fieldNames, Object[] fieldValues) throws RuntimeOperationsException;

    /** A copy. The immutable ones return themselves, which is copy enough. */
    Object clone() throws RuntimeOperationsException;

    /** Whether the fields JMX does know have admissible values. */
    boolean isValid() throws RuntimeOperationsException;

    /** By set of fields, with the names compared case-insensitively. */
    boolean equals(Object obj);

    int hashCode();
}
