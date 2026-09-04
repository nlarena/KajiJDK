package javax.naming.event;

/**
 * KajiLibrary's javax.naming.event.ObjectChangeListener -- cambios en el <b>contenido</b> de una
 * entrada.
 *
 * <p>La contracara de {@link NamespaceChangeListener}: la entrada sigue donde estaba y con el mismo
 * nombre, lo que cambio es lo que tiene adentro -- sus atributos, o el objeto al que esta atada.
 *
 * <p>El evento trae la asociacion vieja y la nueva, y compararlas es la unica forma de saber que
 * cambio: el API no manda un delta. Con las dos, un oyente puede decidir si el cambio le importa sin
 * volver a consultar el directorio.
 */
public interface ObjectChangeListener extends NamingListener {

    /** Cambio el contenido de una entrada. */
    void objectChanged(NamingEvent evt);
}
