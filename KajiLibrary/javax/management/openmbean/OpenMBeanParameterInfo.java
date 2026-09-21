package javax.management.openmbean;

import java.util.Set;

/**
 * The description of a parameter of an open MBean: its name, its open type and, optionally, which
 * values it accepts.
 *
 * <p>The constraints are three and <b>exclude each other</b>: either there is a list of legal
 * values, or there is a minimum and/or a maximum, or there is nothing. Declaring the first two
 * together makes no sense --a list already says which ones are valid-- and that is why the
 * implementations reject it instead of trying to combine them.
 *
 * <p>The {@code hasXxx} methods exist because {@code null} is ambiguous: a null
 * {@code getDefaultValue()} may mean "it has no default value" or "its default value is null".
 * The question/value pair separates the two.
 */
public interface OpenMBeanParameterInfo {

    /** The description, for a person. */
    String getDescription();

    /** The parameter's name. */
    String getName();

    /** Its open type. */
    OpenType<?> getOpenType();

    /** The default value, or null if it has none. See the note about the {@code hasXxx}. */
    Object getDefaultValue();

    /** The legal values, or null if they are not enumerated. */
    Set<?> getLegalValues();

    /** The minimum, or null if there is none. */
    Comparable<?> getMinValue();

    /** The maximum, or null if there is none. */
    Comparable<?> getMaxValue();

    /** Whether it has a default value. */
    boolean hasDefaultValue();

    /** Whether its legal values are enumerated. */
    boolean hasLegalValues();

    /** Whether it has a minimum. */
    boolean hasMinValue();

    /** Whether it has a maximum. */
    boolean hasMaxValue();

    /** Whether {@code obj} is a valid value: of the open type <b>and</b> within the constraints. */
    boolean isValue(Object obj);

    boolean equals(Object obj);

    int hashCode();

    String toString();
}
