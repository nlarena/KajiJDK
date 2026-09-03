package org.w3c.dom.events;

/**
 * KajiLibrary's org.w3c.dom.events.Event -- algo que paso en el documento.
 *
 * <h2>El recorrido en tres fases</h2>
 *
 * <p>Un evento no se entrega solo en el nodo donde paso: recorre el arbol dos veces.
 *
 * <ol>
 *   <li>{@link #CAPTURING_PHASE} -- baja desde la raiz hasta el objetivo. Solo lo ven los escuchas
 *       registrados con {@code useCapture = true}.
 *   <li>{@link #AT_TARGET} -- llega al nodo donde paso.
 *   <li>{@link #BUBBLING_PHASE} -- sube de vuelta hasta la raiz, si el evento burbujea.
 * </ol>
 *
 * <p>De ahi sale la diferencia entre {@link #getTarget()} --donde <b>paso</b>, siempre el mismo-- y
 * {@link #getCurrentTarget()} --por donde <b>va pasando</b>, distinto en cada escucha--. Leer el
 * segundo creyendo que es el primero es el error clasico de este API.
 *
 * <h2>Detener no es cancelar</h2>
 *
 * <p>Los dos metodos de control hacen cosas distintas y son independientes:
 *
 * <ul>
 *   <li>{@link #stopPropagation()} corta el <b>recorrido</b>: los nodos que faltan no se enteran. La
 *       accion por omision igual ocurre.
 *   <li>{@link #preventDefault()} cancela la <b>accion</b> --seguir un enlace, enviar un
 *       formulario-- y el recorrido sigue. Solo sirve si el evento es cancelable.
 * </ul>
 */
public interface Event {

    /** Bajando desde la raiz hacia el objetivo. */
    short CAPTURING_PHASE = 1;

    /** En el nodo donde paso. */
    short AT_TARGET = 2;

    /** Subiendo desde el objetivo hacia la raiz. */
    short BUBBLING_PHASE = 3;

    /** El nombre del evento: {@code "click"}, {@code "DOMNodeInserted"}. Sin prefijo {@code "on"}. */
    String getType();

    /** Donde <b>paso</b>. No cambia durante el recorrido. */
    EventTarget getTarget();

    /** Por donde <b>va pasando</b>. Cambia en cada escucha; ver la nota de la clase. */
    EventTarget getCurrentTarget();

    /** En cual de las tres fases esta. */
    short getEventPhase();

    /** Si sube por la fase de burbujeo. Un evento que no burbujea solo llega al objetivo. */
    boolean getBubbles();

    /** Si {@link #preventDefault()} tiene algun efecto sobre el. */
    boolean getCancelable();

    /**
     * Cuando ocurrio, en milisegundos desde la epoca.
     *
     * <p>Puede ser 0: el estandar admite que una implementacion no tenga un reloj con suficiente
     * resolucion, y devolver 0 es como lo dice.
     */
    long getTimeStamp();

    /** Corta el recorrido. No cancela la accion; ver la nota de la clase. */
    void stopPropagation();

    /** Cancela la accion por omision. No corta el recorrido. */
    void preventDefault();

    /**
     * Inicializa un evento recien creado por {@code DocumentEvent.createEvent}.
     *
     * <p>Hace falta porque el evento se crea vacio: la fabrica no toma argumentos. Llamarlo sobre un
     * evento que ya se esta despachando no hace nada.
     */
    void initEvent(String eventTypeArg, boolean canBubbleArg, boolean cancelableArg);
}
