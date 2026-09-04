package javax.naming.event;

/**
 * KajiLibrary's javax.naming.event.NamespaceChangeListener -- cambios en el <b>espacio de nombres</b>.
 *
 * <p>Los tres eventos son sobre la existencia y la ubicacion de las entradas: aparecio una,
 * desaparecio una, cambio de nombre. Lo que <b>no</b> cubre es que el contenido de una entrada cambie
 * -- eso es {@link ObjectChangeListener}.
 *
 * <p>La division importa al escuchar: implementar solo esta interfaz sobre un directorio muy activo
 * evita recibir un evento por cada modificacion de atributo, que suelen ser la mayoria.
 *
 * <p>En {@link #objectRenamed} el evento trae las dos asociaciones --la vieja y la nueva-- y una de
 * las dos puede ser null: renombrar hacia adentro o hacia afuera del alcance suscrito se ve como una
 * aparicion o una desaparicion parcial.
 */
public interface NamespaceChangeListener extends NamingListener {

    /** Aparecio una entrada nueva. */
    void objectAdded(NamingEvent evt);

    /** Desaparecio una. */
    void objectRemoved(NamingEvent evt);

    /** Una cambio de nombre. Ver la nota de la clase sobre los nulls. */
    void objectRenamed(NamingEvent evt);
}
