package javax.management.openmbean;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

// The constraints of an open parameter --default value, legal values, minimum and maximum-- with
// their validation and their behaviour.
//
// It exists because `OpenMBeanParameterInfoSupport` and `OpenMBeanAttributeInfoSupport` need
// exactly the same thing and **cannot share a superclass**: each extends its own `MBeanXxxInfo`
// from `javax.management`, and Java has no multiple inheritance. Delegating to an object is the
// only way for the rule to live written once.
//
// Package-private on purpose: it is a detail of how those two are implemented, not part of the
// contract.
final class Constraints {

    private final OpenType<?> openType;
    private final Object defaultValue;
    private final Set<?> legalValues;
    private final Comparable<?> minValue;
    private final Comparable<?> maxValue;

    Constraints(OpenType<?> openType, Object defaultValue, Object[] legalValues,
            Comparable<?> minValue, Comparable<?> maxValue) throws OpenDataException {
        if (openType == null) {
            throw new IllegalArgumentException("the open type cannot be null");
        }
        this.openType = openType;

        // The two ways of constraining exclude each other: a list of legal values already says
        // which ones are valid, and a range over that list is either redundant or contradicts it.
        // Accepting both would force deciding which wins, and any choice would surprise someone.
        boolean hasLegalList = legalValues != null && legalValues.length > 0;
        boolean hasRange = minValue != null || maxValue != null;
        if (hasLegalList && hasRange) {
            throw new OpenDataException(
                    "legal values and a range cannot be given at the same time");
        }
        // An `ArrayType` and a `TabularType` admit NONE of the four constraints. A `CompositeType`
        // does admit them, and that split surprises: one would expect the composite to be the most
        // restricted of the three. It was checked against the JDK 25 --the first version of this
        // class had it exactly backwards-- and the reason is that a composite value is a value with
        // identity by content, while an array or a table do not compare usefully with `equals`.
        if (openType instanceof ArrayType || openType instanceof TabularType) {
            if (hasLegalList || hasRange || defaultValue != null) {
                throw new OpenDataException(
                        "a " + openType.getClass().getSimpleName()
                                + " admits no default value or constraints");
            }
        }

        if (defaultValue != null && !openType.isValue(defaultValue)) {
            throw new OpenDataException("the default value is not of type "
                    + openType.getTypeName());
        }

        Set<Object> ls = null;
        if (hasLegalList) {
            // `LinkedHashSet` and not `HashSet`: `getLegalValues` is printed in `toString` and an
            // output that changes order between runs is a pain to compare against the JDK.
            ls = new LinkedHashSet<Object>();
            for (int i = 0; i < legalValues.length; i++) {
                Object v = legalValues[i];
                if (v == null) {
                    throw new OpenDataException("a legal value is null");
                }
                if (!openType.isValue(v)) {
                    throw new OpenDataException("the legal value " + v + " is not of type "
                            + openType.getTypeName());
                }
                ls.add(v);
            }
            if (defaultValue != null && !ls.contains(defaultValue)) {
                throw new OpenDataException(
                        "the default value is not among the legal values");
            }
        }

        if (minValue != null && !openType.isValue(minValue)) {
            throw new OpenDataException("the minimum is not of type " + openType.getTypeName());
        }
        if (maxValue != null && !openType.isValue(maxValue)) {
            throw new OpenDataException("the maximum is not of type " + openType.getTypeName());
        }
        if (minValue != null && maxValue != null && compare(minValue, maxValue) > 0) {
            throw new OpenDataException("the minimum is greater than the maximum");
        }
        if (defaultValue != null && minValue != null
                && compare(minValue, defaultValue) > 0) {
            throw new OpenDataException("the default value is less than the minimum");
        }
        if (defaultValue != null && maxValue != null
                && compare(maxValue, defaultValue) < 0) {
            throw new OpenDataException("the default value is greater than the maximum");
        }

        this.defaultValue = defaultValue;
        this.legalValues = ls == null ? null : Collections.unmodifiableSet(ls);
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    // The `unchecked` is confined to this method: both values already went through `isValue` of the
    // same open type, so they are of the same class and that class is comparable -- all the simple
    // types are. The wildcard of `Comparable<?>` is what prevents saying so without the cast.
    @SuppressWarnings("unchecked")
    static int compare(Object a, Object b) {
        Comparable<Object> c = (Comparable<Object>) a;
        return c.compareTo(b);
    }

    OpenType<?> getOpenType() {
        return this.openType;
    }

    Object getDefaultValue() {
        return this.defaultValue;
    }

    Set<?> getLegalValues() {
        return this.legalValues;
    }

    Comparable<?> getMinValue() {
        return this.minValue;
    }

    Comparable<?> getMaxValue() {
        return this.maxValue;
    }

    boolean hasDefaultValue() {
        return this.defaultValue != null;
    }

    boolean hasLegalValues() {
        return this.legalValues != null;
    }

    boolean hasMinValue() {
        return this.minValue != null;
    }

    boolean hasMaxValue() {
        return this.maxValue != null;
    }

    /** Whether the value is of the open type <b>and</b> meets the constraints. */
    boolean isValue(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!this.openType.isValue(obj)) {
            return false;
        }
        if (this.legalValues != null && !this.legalValues.contains(obj)) {
            return false;
        }
        if (this.minValue != null && compare(this.minValue, obj) > 0) {
            return false;
        }
        if (this.maxValue != null && compare(this.maxValue, obj) < 0) {
            return false;
        }
        return true;
    }

    /**
     * The equality both {@code Support} classes share: type, default, legal, minimum and maximum.
     */
    boolean sameAs(Constraints other) {
        return sameValue(this.openType, other.openType)
                && sameValue(this.defaultValue, other.defaultValue)
                && sameValue(this.legalValues, other.legalValues)
                && sameValue(this.minValue, other.minValue)
                && sameValue(this.maxValue, other.maxValue);
    }

    static boolean sameValue(Object a, Object b) {
        return a == null ? b == null : a.equals(b);
    }

    static int hash(Object o) {
        return o == null ? 0 : o.hashCode();
    }

    int partialHash() {
        return hash(this.openType) + hash(this.defaultValue) + hash(this.legalValues)
                + hash(this.minValue) + hash(this.maxValue);
    }

    /** The stretch of {@code toString} that describes the constraints. */
    void describe(StringBuilder sb) {
        sb.append(",openType=").append(this.openType.toString());
        // The order is the JDK 25's --default, minimum, maximum, legal--, checked against its
        // output. A `toString` is text for a person and nobody should parse it, but matching the
        // original makes comparing the two runs a matter of reading a difference and not of
        // translating.
        sb.append(",default=").append(String.valueOf(this.defaultValue));
        sb.append(",minValue=").append(String.valueOf(this.minValue));
        sb.append(",maxValue=").append(String.valueOf(this.maxValue));
        sb.append(",legalValues=").append(String.valueOf(this.legalValues));
    }
}
