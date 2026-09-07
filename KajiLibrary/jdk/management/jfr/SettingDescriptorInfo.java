package jdk.management.jfr;

import java.util.Collections;

import javax.management.openmbean.CompositeData;

/**
 * Un {@link jdk.jfr.SettingDescriptor} visto desde el otro lado de una conexion JMX.
 *
 * <p>Mismo dato, sin las anotaciones: {@link jdk.jfr.SettingDescriptor#getAnnotationElements} no
 * tiene equivalente aca porque una anotacion no se puede representar como tipo abierto.
 *
 * <p>Lo que si sobrevive es lo que una consola necesita para dibujar un formulario de
 * configuracion: nombre, etiqueta, descripcion, tipo y valor por omision.
 *
 * @since 9
 */
public final class SettingDescriptorInfo {

    private final String name;
    private final String label;
    private final String description;
    private final String typeName;
    private final String contentType;
    private final String defaultValue;

    SettingDescriptorInfo(final String name, final String label, final String description,
            final String typeName, final String contentType, final String defaultValue) {
        this.name = name;
        this.label = label;
        this.description = description;
        this.typeName = typeName;
        this.contentType = contentType;
        this.defaultValue = defaultValue;
    }

    /**
     * El nombre del ajuste.
     *
     * @return el valor
     */
    public String getName() {
        return name;
    }

    /**
     * El nombre legible.
     *
     * @return el valor
     */
    public String getLabel() {
        return label;
    }

    /**
     * Que hace el ajuste.
     *
     * @return el valor
     */
    public String getDescription() {
        return description;
    }

    /**
     * El nombre del tipo del ajuste.
     *
     * @return el valor
     */
    public String getTypeName() {
        return typeName;
    }

    /**
     * El nombre de la anotacion que le da significado al valor, o {@code null}.
     *
     * @return el valor
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * El valor con el que arranca.
     *
     * @return el valor
     */
    public String getDefaultValue() {
        return defaultValue;
    }

    /**
     * Reconstruye el objeto desde su forma abierta.
     *
     * <p>Es el camino por el que este dato llega de una VM remota: lo que viaja por JMX es un
     * {@link CompositeData} generico y esto lo vuelve a convertir.
     *
     * @param cd la forma abierta, o {@code null}
     * @return el objeto, o {@code null} si {@code cd} era {@code null}
     * @throws IllegalArgumentException si no tiene la forma esperada
     */
    public static SettingDescriptorInfo from(final CompositeData cd) {
        if (cd == null) {
            return null;
        }
        throw new IllegalArgumentException(
                "reconstruir un SettingDescriptorInfo necesita el soporte de tipos abiertos "
                + "de JFR, que esta biblioteca no implementa");
    }

    /** {@inheritDoc} */
    public String toString() {
        final StringBuilder sb = new StringBuilder("SettingDescriptorInfo{");
        sb.append("name=").append(String.valueOf(name));
        sb.append(", ");
        sb.append("label=").append(String.valueOf(label));
        sb.append(", ");
        sb.append("description=").append(String.valueOf(description));
        sb.append(", ");
        sb.append("typeName=").append(String.valueOf(typeName));
        sb.append(", ");
        sb.append("contentType=").append(String.valueOf(contentType));
        sb.append(", ");
        sb.append("defaultValue=").append(String.valueOf(defaultValue));
        return sb.append('}').toString();
    }
}
