package javax.management.openmbean;

import java.util.Set;
import javax.management.Descriptor;
import javax.management.MBeanParameterInfo;

/**
 * La implementación de {@link OpenMBeanParameterInfo}.
 *
 * <p>Hereda de `MBeanParameterInfo` para que un cliente que no sabe de tipos abiertos siga viendo
 * un parámetro normal, y le pasa a `super` el `className` del tipo abierto como `type`. Ésa es la
 * traducción entera entre los dos mundos.
 *
 * <p>Las restricciones viven en un objeto aparte, {@link Constraints}: ver ahí por qué no están
 * heredadas.
 */
public class OpenMBeanParameterInfoSupport extends MBeanParameterInfo
        implements OpenMBeanParameterInfo {

    private static final long serialVersionUID = -7235016932339159395L;

    private final Constraints constraints;

    /** Un parámetro sin restricciones. */
    public OpenMBeanParameterInfoSupport(String name, String description, OpenType<?> openType) {
        this(name, description, openType, (Descriptor) null);
    }

    /** Un parámetro sin restricciones, con ese descriptor. */
    public OpenMBeanParameterInfoSupport(String name, String description, OpenType<?> openType,
            Descriptor descriptor) {
        super(requireName(name), requireOpenType(openType), requireDescription(description), descriptor);
        try {
            this.constraints = new Constraints(openType, null, null, null, null);
        } catch (OpenDataException e) {
            // Sin restricciones no hay nada que validar, así que este camino no se alcanza. Se
            // envuelve en vez de declararse para no obligar a los dos constructores simples a
            // declarar una excepción verificada que no pueden tirar -- que es lo que hace el JDK.
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    /**
     * Un parámetro con valor por omisión.
     *
     * @throws OpenDataException si el valor no es del tipo, o si el tipo no admite omisión
     */
    public <T> OpenMBeanParameterInfoSupport(String name, String description,
            OpenType<T> openType, T defaultValue) throws OpenDataException {
        this(name, description, openType, defaultValue, (T[]) null);
    }

    /**
     * Un parámetro con valor por omisión y valores legales.
     *
     * @throws OpenDataException si alguno no es del tipo, si el valor por omisión no está entre los
     *     legales, o si el tipo no admite restricciones
     */
    public <T> OpenMBeanParameterInfoSupport(String name, String description,
            OpenType<T> openType, T defaultValue, T[] legalValues) throws OpenDataException {
        super(requireName(name), requireOpenType(openType), requireDescription(description));
        this.constraints = new Constraints(openType, defaultValue, legalValues, null, null);
    }

    /**
     * Un parámetro con valor por omisión y rango.
     *
     * @throws OpenDataException si alguno no es del tipo, si el mínimo supera al máximo, si el
     *     valor por omisión queda fuera del rango, o si el tipo no admite restricciones
     */
    public <T> OpenMBeanParameterInfoSupport(String name, String description,
            OpenType<T> openType, T defaultValue, Comparable<T> minValue, Comparable<T> maxValue)
            throws OpenDataException {
        super(requireName(name), requireOpenType(openType), requireDescription(description));
        this.constraints = new Constraints(openType, defaultValue, null, minValue, maxValue);
    }

    // Las tres validaciones corren ANTES de `super`, que es donde tienen que correr: si se hicieran
    // después, un tipo nulo ya habría reventado adentro de `exigirTipo` con un mensaje peor.
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

    /**
     * Igualdad contra cualquier {@link OpenMBeanParameterInfo}, no sólo contra otro `Support`.
     *
     * <p>Es lo que el contrato pide, y es lo que permite comparar un parámetro que llegó por la red
     * con uno construido acá.
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
