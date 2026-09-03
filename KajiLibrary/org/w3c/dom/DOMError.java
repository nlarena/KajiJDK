package org.w3c.dom;

/**
 * KajiLibrary's org.w3c.dom.DOMError -- un problema encontrado al procesar un documento.
 *
 * <p>No tiene nada que ver con {@link DOMException} y conviene no mezclarlas. Una
 * {@code DOMException} se **lanza** cuando el llamador pidio algo imposible y corta ahi mismo; un
 * {@code DOMError} se **reporta** a un {@link DOMErrorHandler} durante una operacion larga
 * --{@link Document#normalizeDocument}, una validacion, una carga-- que quiere seguir adelante y
 * juntar todos los problemas en vez de morir en el primero.
 *
 * <p>De ahi las tres severidades, que como en cualquier reporte de errores no se distinguen por
 * gravedad sino por **que puede pasar despues**: con {@link #SEVERITY_WARNING} el procesamiento
 * sigue normal; con {@link #SEVERITY_ERROR} se puede seguir pero el resultado ya no es confiable; y
 * con {@link #SEVERITY_FATAL_ERROR} no se puede continuar.
 *
 * <p>Los tres valores son 1, 2 y 3 y salen de la especificacion.
 *
 * <p>Interfaz declarada entera.
 */
public interface DOMError {

    /** El procesamiento sigue normalmente. */
    public static final short SEVERITY_WARNING = 1;

    /** Se puede continuar, pero el resultado ya no es confiable. */
    public static final short SEVERITY_ERROR = 2;

    /** No se puede continuar. */
    public static final short SEVERITY_FATAL_ERROR = 3;

    /** Una de las tres constantes {@code SEVERITY_*}. */
    public short getSeverity();

    /** El mensaje para leer, en el idioma de la implementacion. */
    public String getMessage();

    /**
     * El tipo del error, una cadena de la norma como {@code "wf-invalid-character"} o
     * {@code "unbound-prefix-in-entity-reference"}.
     *
     * <p>Es lo que hay que mirar para decidir por programa: el mensaje esta pensado para una
     * persona, esto para un {@code switch}.
     */
    public String getType();

    /** La excepcion que lo origino, si la hubo. */
    public Object getRelatedException();

    /** El dato relacionado --tipicamente el nodo culpable-- o {@code null}. */
    public Object getRelatedData();

    /** Donde ocurrio. */
    public DOMLocator getLocation();
}
