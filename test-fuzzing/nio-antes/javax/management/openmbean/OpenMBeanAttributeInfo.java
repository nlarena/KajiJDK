package javax.management.openmbean;

/**
 * The description of an attribute of an open MBean: a parameter plus how it is accessed.
 *
 * <p>{@code isIs} tells a getter called {@code isFoo()} from one called {@code getFoo()}. It can
 * only be {@code true} if the type is {@code SimpleType.BOOLEAN}, which is the only way Java allows
 * the {@code is} prefix.
 */
public interface OpenMBeanAttributeInfo extends OpenMBeanParameterInfo {

    /** Whether it can be read. */
    boolean isReadable();

    /** Whether it can be written. */
    boolean isWritable();

    /** Whether its getter is called {@code isXxx} instead of {@code getXxx}. */
    boolean isIs();

    boolean equals(Object obj);

    int hashCode();

    String toString();
}
