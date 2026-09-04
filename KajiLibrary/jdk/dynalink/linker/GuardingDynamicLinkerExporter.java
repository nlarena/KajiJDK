package jdk.dynalink.linker;

import java.util.List;
import java.util.function.Supplier;

/**
 * El punto por donde un enlazador de terceros entra a Dynalink sin que nadie lo nombre.
 *
 * <h2>Como funciona</h2>
 *
 * <p>Es un servicio de {@code ServiceLoader}: quien quiera aportar enlazadores extiende esta
 * clase, la declara en su modulo con {@code provides} y {@code DynamicLinkerFactory} la encuentra
 * sola al armar la cadena. El que hospeda a Dynalink no tiene que enterarse.
 *
 * <p>Extiende {@link Supplier} y no devuelve un enlazador sino una <strong>lista</strong>: una
 * biblioteca que aporta soporte para varios tipos de objeto los publica todos de una.
 *
 * <h2>La constante que quedo</h2>
 *
 * <p>{@link #AUTOLOAD_PERMISSION_NAME} nombraba el permiso que hacia falta para que la carga
 * automatica ocurriera. Sigue publica y {@code final} porque es API, aunque el
 * {@code SecurityManager} este deshabilitado desde JDK 24 y el chequeo ya no pase.
 *
 * @since 9
 */
public abstract class GuardingDynamicLinkerExporter
        implements Supplier<List<GuardingDynamicLinker>> {

    /** Nombre del {@code RuntimePermission} que guardaba la carga automatica de enlazadores. */
    public static final String AUTOLOAD_PERMISSION_NAME = "dynalink.exportLinkersAutomatically";

    /** Para las subclases. */
    protected GuardingDynamicLinkerExporter() {
    }
}
