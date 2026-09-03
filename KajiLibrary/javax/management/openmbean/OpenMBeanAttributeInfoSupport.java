package javax.management.openmbean;

import java.util.Set;
import javax.management.Descriptor;
import javax.management.MBeanAttributeInfo;

/**
 * La implementación de {@link OpenMBeanAttributeInfo}: un {@link OpenMBeanParameterInfoSupport} con
 * `isReadable`/`isWritable`/`isIs` encima.
 *
 * <p>Repite la delegación en {@link Constraints} en vez de heredar de
 * `OpenMBeanParameterInfoSupport`, y no puede ser de otra forma: hereda de `MBeanAttributeInfo`,
 * que no es un `MBeanParameterInfo`. Es el mismo motivo por el que `Constraints` existe.
 */
public class OpenMBeanAttributeInfoSupport extends MBeanAttributeInfo
        implements OpenMBeanAttributeInfo {

    private static final long serialVersionUID = -4867215622149721849L;

    private final Constraints constraints;

    /** Un atributo sin restricciones. */
    public OpenMBeanAttributeInfoSupport(String name, String description, OpenType<?> openType,
            boolean isReadable, boolean isWritable, boolean isIs) {
        this(name, description, openType, isReadable, isWritable, isIs, (Descriptor) null);
    }

    /** Un atributo sin restricciones, con ese descriptor. */
    public OpenMBeanAttributeInfoSupport(String name, String description, OpenType<?> openType,
            boolean isReadable, boolean isWritable, boolean isIs, Descriptor descriptor) {
        super(requireName(name), requireOpenType(openType), requireDescription(description),
                isReadable, isWritable, isIs, descriptor);
        try {
            this.constraints = new Constraints(openType, null, null, null, null);
        } catch (OpenDataException e) {
            // Inalcanzable sin restricciones; ver la nota igual en
            // `OpenMBeanParameterInfoSupport`.
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    /**
     * Un atributo con valor por omisión.
     *
     * @throws OpenDataException si el valor no es del tipo, o si el tipo no admite omisión
     */
    public <T> OpenMBeanAttributeInfoSupport(String name, String description, OpenType<T> openType,
            boolean isReadable, boolean isWritable, boolean isIs, T defaultValue)
            throws OpenDataException {
        this(name, description, openType, isReadable, isWritable, isIs, defaultValue, (T[]) null);
    }

    /**
     * Un atributo con valor por omisión y valores legales.
     *
     * @throws OpenDataException si alguno no es del tipo, si el valor por omisión no está entre los
     *     legales, o si el tipo no admite restricciones
     */
    public <T> OpenMBeanAttributeInfoSupport(String name, String description, OpenType<T> openType,
            boolean isReadable, boolean isWritable, boolean isIs, T defaultValue, T[] legalValues)
            throws OpenDataException {
        super(requireName(name), requireOpenType(openType), requireDescription(description),
                isReadable, isWritable, isIs);
        this.constraints = new Constraints(openType, defaultValue, legalValues, null, null);
    }

    /**
     * Un atributo con valor por omisión y rango.
     *
     * @throws OpenDataException si alguno no es del tipo, si el mínimo supera al máximo, si el
     *     valor por omisión queda fuera del rango, o si el tipo no admite restricciones
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
            throw new IllegalArgumentException("el nombre no puede estar en blanco");
        }
        return name;
    }

    private static String requireDescription(String description) {
        if (description == null || description.trim().length() == 0) {
            throw new IllegalArgumentException("la descripción no puede estar en blanco");
        }
        return description;
    }

    private static String requireOpenType(OpenType<?> openType) {
        if (openType == null) {
            throw new IllegalArgumentException("el tipo abierto no puede ser nulo");
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

    /** Igualdad contra cualquier {@link OpenMBeanAttributeInfo}, incluidos los tres accesos. */
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
