package jdk.management.jfr;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.management.openmbean.CompositeData;

/**
 * Una {@link jdk.jfr.Configuration} vista desde el otro lado de una conexion JMX.
 *
 * <h2>Por que no se usa la clase original</h2>
 *
 * <p>Porque {@code Configuration} no viaja: JMX transporta {@code CompositeData}, que es una
 * estructura generica de nombre a valor, y no objetos arbitrarios. Una consola que se conecta a una
 * VM remota recibe eso.
 *
 * <p>Esta clase es el mismo dato del lado del cliente, con accesores tipados. {@link #from} es como
 * se reconstruye.
 *
 * <p>Es <strong>solo lectura</strong>, y tiene que serlo: describe el estado de otra VM en un
 * momento dado. Un setter haria creer que cambiarlo cambia algo del otro lado.
 *
 * @since 9
 */
public final class ConfigurationInfo {

    private final String name;
    private final String label;
    private final String description;
    private final String provider;
    private final String contents;
    private final Map<String, String> settings;

    ConfigurationInfo(final String name, final String label, final String description,
            final String provider, final String contents, final Map<String, String> settings) {
        this.name = name;
        this.label = label;
        this.description = description;
        this.provider = provider;
        this.contents = contents;
        this.settings = Collections.unmodifiableMap(new LinkedHashMap<String, String>(settings));
    }

    /**
     * El nombre de la configuracion.
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
     * Para que sirve esta configuracion.
     *
     * @return el valor
     */
    public String getDescription() {
        return description;
    }

    /**
     * Quien la escribio.
     *
     * @return el valor
     */
    public String getProvider() {
        return provider;
    }

    /**
     * El texto del archivo {@code .jfc}.
     *
     * @return el valor
     */
    public String getContents() {
        return contents;
    }

    /**
     * Los ajustes, con la clave {@code "evento#ajuste"}.
     *
     * @return el valor
     */
    public Map<String, String> getSettings() {
        return settings;
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
    public static ConfigurationInfo from(final CompositeData cd) {
        if (cd == null) {
            return null;
        }
        throw new IllegalArgumentException(
                "reconstruir un ConfigurationInfo necesita el soporte de tipos abiertos de JFR, que"
                + " esta biblioteca no implementa");
    }

    /** {@inheritDoc} */
    public String toString() {
        final StringBuilder sb = new StringBuilder("ConfigurationInfo{");
        sb.append("name=").append(String.valueOf(name));
        sb.append(", ");
        sb.append("label=").append(String.valueOf(label));
        sb.append(", ");
        sb.append("description=").append(String.valueOf(description));
        sb.append(", ");
        sb.append("provider=").append(String.valueOf(provider));
        sb.append(", ");
        sb.append("contents=").append(String.valueOf(contents));
        sb.append(", ");
        sb.append("settings=").append(String.valueOf(settings));
        return sb.append('}').toString();
    }
}
