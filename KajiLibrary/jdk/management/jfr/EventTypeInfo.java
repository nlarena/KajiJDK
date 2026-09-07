package jdk.management.jfr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.management.openmbean.CompositeData;

/**
 * Un {@link jdk.jfr.EventType} visto desde el otro lado de una conexion JMX.
 *
 * <p>Trae lo que hace falta para <strong>elegir</strong> que grabar: nombre, etiqueta, categorias y
 * los ajustes que admite.
 *
 * <p>Lo que no trae son los campos del evento ({@code getFields}). Es coherente con para que sirve:
 * una consola remota usa esto para armar la pantalla de configuracion, y los campos recien importan
 * al leer la grabacion, que se hace con el archivo en la mano.
 *
 * @since 9
 */
public final class EventTypeInfo {

    private final String name;
    private final String label;
    private final String description;
    private final long id;
    private final List<String> categoryNames;
    private final List<SettingDescriptorInfo> settingDescriptors;

    EventTypeInfo(final String name, final String label, final String description,
            final long id, final List<String> categoryNames,
            final List<SettingDescriptorInfo> settingDescriptors) {
        this.name = name;
        this.label = label;
        this.description = description;
        this.id = id;
        this.categoryNames = Collections.unmodifiableList(new ArrayList<String>(categoryNames));
        this.settingDescriptors = Collections.unmodifiableList(
                new ArrayList<SettingDescriptorInfo>(settingDescriptors));
    }

    /**
     * El nombre del tipo de evento.
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
     * Que registra este evento.
     *
     * @return el valor
     */
    public String getDescription() {
        return description;
    }

    /**
     * El identificador del tipo.
     *
     * @return el valor
     */
    public long getId() {
        return id;
    }

    /**
     * Las categorias, de la mas general a la mas especifica.
     *
     * @return el valor
     */
    public List<String> getCategoryNames() {
        return categoryNames;
    }

    /**
     * Los ajustes que este tipo de evento admite.
     *
     * @return el valor
     */
    public List<SettingDescriptorInfo> getSettingDescriptors() {
        return settingDescriptors;
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
    public static EventTypeInfo from(final CompositeData cd) {
        if (cd == null) {
            return null;
        }
        throw new IllegalArgumentException(
                "reconstruir un EventTypeInfo necesita el soporte de tipos abiertos de JFR, que"
                + " esta biblioteca no implementa");
    }

    /** {@inheritDoc} */
    public String toString() {
        final StringBuilder sb = new StringBuilder("EventTypeInfo{");
        sb.append("name=").append(String.valueOf(name));
        sb.append(", ");
        sb.append("label=").append(String.valueOf(label));
        sb.append(", ");
        sb.append("description=").append(String.valueOf(description));
        sb.append(", ");
        sb.append("id=").append(String.valueOf(id));
        sb.append(", ");
        sb.append("categoryNames=").append(String.valueOf(categoryNames));
        sb.append(", ");
        sb.append("settingDescriptors=").append(String.valueOf(settingDescriptors));
        return sb.append('}').toString();
    }
}
