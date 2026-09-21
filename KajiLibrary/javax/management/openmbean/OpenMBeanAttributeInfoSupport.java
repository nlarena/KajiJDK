package javax.management.openmbean;

import java.util.Set;
import javax.management.Descriptor;
import javax.management.MBeanAttributeInfo;

/**
 * The implementation of {@link OpenMBeanAttributeInfo}: an {@link OpenMBeanParameterInfoSupport}
 * with {@code isReadable}/{@code isWritable}/{@code isIs} on top.
 *
 * <p>It repeats the delegation to {@link Constraints} instead of extending
 * {@code OpenMBeanParameterInfoSupport}, and it cannot be otherwise: it extends
 * {@code MBeanAttributeInfo}, which is not an {@code MBeanParameterInfo}. It is the same reason
 * {@code Constraints} exists.
 */
public class OpenMBeanAttributeInfoSupport extends MBeanAttributeInfo
        implements OpenMBeanAttributeInfo {

    private static final long serialVersionUID = -4867215622149721849L;

    private final Constraints constraints;

    /** An attribute without constraints. */
    public OpenMBeanAttributeInfoSupport(String name, String description, OpenType<?> openType,
            boolean isReadable, boolean isWritable, boolean isIs) {
        this(name, description, openType, isReadable, isWritable, isIs, (Descriptor) null);
    }

    /** An attribute without constraints, with that descriptor. */
    public OpenMBeanAttributeInfoSupport(String name, String description, OpenType<?> openType,
            boolean isReadable, boolean isWritable, boolean isIs, Descriptor descriptor) {
        super(requireName(name), requireOpenType(openType), requireDescription(description),
                isReadable, isWritable, isIs, descriptor);
        try {
            this.constraints = new Constraints(openType, null, null, null, null);
        } catch (OpenDataException e) {
            // Unreachable without constraints; see the same note in
            // `OpenMBeanParameterInfoSupport`.
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    /**
     * An attribute with a default value.
     *
     * @throws OpenDataException if the value is not of the type, or if the type admits no default
     */
    public <T> OpenMBeanAttributeInfoSupport(String name, String description, OpenType<T> openType,
            boolean isReadable, boolean isWritable, boolean isIs, T defaultValue)
            throws OpenDataException {
        this(name, description, openType, isReadable, isWritable, isIs, defaultValue, (T[]) null);
    }

    /**
     * An attribute with a default value and legal values.
     *
     * @throws OpenDataException if one is not of the type, if the default value is not among the
     *     legal ones, or if the type admits no constraints
     */
    public <T> OpenMBeanAttributeInfoSupport(String name, String description, OpenType<T> openType,
            boolean isReadable, boolean isWritable, boolean isIs, T defaultValue, T[] legalValues)
            throws OpenDataException {
        super(requireName(name), requireOpenType(openType), requireDescription(description),
                isReadable, isWritable, isIs);
        this.constraints = new Constraints(openType, defaultValue, legalValues, null, null);
    }

    /**
     * An attribute with a default value and a range.
     *
     * @throws OpenDataException if one is not of the type, if the minimum exceeds the maximum, if
     *     the default value falls outside the range, or if the type admits no constraints
     */
    public <T> OpenMBeanAttributeInfoSupport(String name, String description, OpenType<T> openType,
            boolean isReadable, boolean isWritable, boolean isIs, T defaultValue,
            Comparable<T> minValue, Comparable<T> maxValue) throws OpenDataException {
        super(requireName(name), requireOpenType(openType), requireDescription(description),
                isReadable, isWritable, isIs);
        this.constraints = new Constraints(openType, defaultValue, null, minValue, maxValue);
    }

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

    /** Equality against any {@link OpenMBeanAttributeInfo}, including the three accesses. */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof OpenMBeanAttributeInfo)) {
            return false;
        }
        OpenMBeanAttributeInfo other = (OpenMBeanAttributeInfo) obj;
        return this.getName().equals(other.getName())
                && this.isReadable() == other.isReadable()
                && this.isWritable() == other.isWritable()
                && this.isIs() == other.isIs()
                && Constraints.sameValue(this.getOpenType(), other.getOpenType())
                && Constraints.sameValue(this.getDefaultValue(), other.getDefaultValue())
                && Constraints.sameValue(this.getLegalValues(), other.getLegalValues())
                && Constraints.sameValue(this.getMinValue(), other.getMinValue())
                && Constraints.sameValue(this.getMaxValue(), other.getMaxValue());
    }

    public int hashCode() {
        int h = this.getName().hashCode() + this.constraints.partialHash();
        if (this.isReadable()) {
            h = h + 1;
        }
        if (this.isWritable()) {
            h = h + 2;
        }
        if (this.isIs()) {
            h = h + 4;
        }
        return h;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(OpenMBeanAttributeInfoSupport.class.getName());
        sb.append("(name=").append(this.getName());
        this.constraints.describe(sb);
        sb.append(",isReadable=").append(this.isReadable());
        sb.append(",isWritable=").append(this.isWritable());
        sb.append(",isIs=").append(this.isIs());
        sb.append(")");
        return sb.toString();
    }
}
