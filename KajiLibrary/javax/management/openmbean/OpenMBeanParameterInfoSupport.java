package javax.management.openmbean;

import java.util.Set;
import javax.management.Descriptor;
import javax.management.MBeanParameterInfo;

/**
 * The implementation of {@link OpenMBeanParameterInfo}.
 *
 * <p>It extends {@code MBeanParameterInfo} so that a client that knows nothing about open types
 * still sees a normal parameter, and passes {@code super} the open type's {@code className} as
 * {@code type}. That is the whole translation between the two worlds.
 *
 * <p>The constraints live in a separate object, {@link Constraints}: see there why they are not
 * inherited.
 */
public class OpenMBeanParameterInfoSupport extends MBeanParameterInfo
        implements OpenMBeanParameterInfo {

    private static final long serialVersionUID = -7235016932339159395L;

    private final Constraints constraints;

    /** A parameter without constraints. */
    public OpenMBeanParameterInfoSupport(String name, String description, OpenType<?> openType) {
        this(name, description, openType, (Descriptor) null);
    }

    /** A parameter without constraints, with that descriptor. */
    public OpenMBeanParameterInfoSupport(String name, String description, OpenType<?> openType,
            Descriptor descriptor) {
        super(requireName(name), requireOpenType(openType), requireDescription(description), descriptor);
        try {
            this.constraints = new Constraints(openType, null, null, null, null);
        } catch (OpenDataException e) {
            // Without constraints there is nothing to validate, so this path is never reached. It
            // is wrapped instead of declared so as not to force the two simple constructors to
            // declare a checked exception they cannot throw -- which is what the JDK does.
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    /**
     * A parameter with a default value.
     *
     * @throws OpenDataException if the value is not of the type, or if the type admits no default
     */
    public <T> OpenMBeanParameterInfoSupport(String name, String description,
            OpenType<T> openType, T defaultValue) throws OpenDataException {
        this(name, description, openType, defaultValue, (T[]) null);
    }

    /**
     * A parameter with a default value and legal values.
     *
     * @throws OpenDataException if one is not of the type, if the default value is not among the
     *     legal ones, or if the type admits no constraints
     */
    public <T> OpenMBeanParameterInfoSupport(String name, String description,
            OpenType<T> openType, T defaultValue, T[] legalValues) throws OpenDataException {
        super(requireName(name), requireOpenType(openType), requireDescription(description));
        this.constraints = new Constraints(openType, defaultValue, legalValues, null, null);
    }

    /**
     * A parameter with a default value and a range.
     *
     * @throws OpenDataException if one is not of the type, if the minimum exceeds the maximum, if
     *     the default value falls outside the range, or if the type admits no constraints
     */
    public <T> OpenMBeanParameterInfoSupport(String name, String description,
            OpenType<T> openType, T defaultValue, Comparable<T> minValue, Comparable<T> maxValue)
            throws OpenDataException {
        super(requireName(name), requireOpenType(openType), requireDescription(description));
        this.constraints = new Constraints(openType, defaultValue, null, minValue, maxValue);
    }

    // The three validations run BEFORE `super`, which is where they have to run: if they ran
    // after, a null type would already have blown up inside `requireType` with a worse message.
    private static String requireName(String name) {
        if (name == null || name.trim().length() == 0) {
            throw new IllegalArgumentException("the name cannot be blank");
        }
        return name;
    }

    private static String requireDescription(String description) {
        if (description == null || description.trim().length() == 0) {
            throw new IllegalArgumentException("the description cannot be blank");
        }
        return description;
    }

    private static String requireOpenType(OpenType<?> openType) {
        if (openType == null) {
            throw new IllegalArgumentException("the open type cannot be null");
        }
        return openType.getClassName();
    }

    public OpenType<?> getOpenType() {
        return this.constraints.getOpenType();
    }

    public Object getDefaultValue() {
        return this.constraints.getDefaultValue();
    }

    public Set<?> getLegalValues() {
        return this.constraints.getLegalValues();
    }

    public Comparable<?> getMinValue() {
        return this.constraints.getMinValue();
    }

    public Comparable<?> getMaxValue() {
        return this.constraints.getMaxValue();
    }

    public boolean hasDefaultValue() {
        return this.constraints.hasDefaultValue();
    }

    public boolean hasLegalValues() {
        return this.constraints.hasLegalValues();
    }

    public boolean hasMinValue() {
        return this.constraints.hasMinValue();
    }

    public boolean hasMaxValue() {
        return this.constraints.hasMaxValue();
    }

    public boolean isValue(Object obj) {
        return this.constraints.isValue(obj);
    }

    /**
     * Equality against any {@link OpenMBeanParameterInfo}, not only against another {@code
     * Support}.
     *
     * <p>It is what the contract asks for, and it is what allows comparing a parameter that arrived
     * over the network with one built here.
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof OpenMBeanParameterInfo)) {
            return false;
        }
        OpenMBeanParameterInfo other = (OpenMBeanParameterInfo) obj;
        return this.getName().equals(other.getName())
                && Constraints.sameValue(this.getOpenType(), other.getOpenType())
                && Constraints.sameValue(this.getDefaultValue(), other.getDefaultValue())
                && Constraints.sameValue(this.getLegalValues(), other.getLegalValues())
                && Constraints.sameValue(this.getMinValue(), other.getMinValue())
                && Constraints.sameValue(this.getMaxValue(), other.getMaxValue());
    }

    public int hashCode() {
        return this.getName().hashCode() + this.constraints.partialHash();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(OpenMBeanParameterInfoSupport.class.getName());
        sb.append("(name=").append(this.getName());
        this.constraints.describe(sb);
        sb.append(")");
        return sb.toString();
    }
}
