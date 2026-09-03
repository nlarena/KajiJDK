package org.w3c.dom;

/**
 * KajiLibrary's org.w3c.dom.UserDataHandler -- el aviso de que a un nodo con datos del usuario le
 * paso algo.
 *
 * <p>Se registra al guardar el dato, en {@link Node#setUserData}, y sirve para resolver una pregunta
 * que el DOM no puede contestar solo: si a un nodo con un objeto Java colgado se lo **clona**, el
 * clon deberia tener el mismo objeto, una copia, o nada. Depende enteramente de que sea ese objeto
 * --una cache es descartable, un identificador hay que copiarlo, una conexion abierta no se
 * duplica-- y el unico que lo sabe es quien lo colgo. Por eso el DOM no copia nada por su cuenta:
 * avisa, y el que avisa decide.
 *
 * <p>Los cinco motivos son 1 a 5 y salen de la especificacion. Notar cual **no** esta:
 * {@code NODE_DELETED} existe pero la norma advierte que las implementaciones en lenguajes con
 * recoleccion de basura, Java incluido, tipicamente **no** lo invocan nunca, porque no hay un
 * momento definido en que un nodo se destruya. Contar con esa notificacion para liberar un recurso
 * es apoyarse en algo que no va a llegar.
 *
 * <p>Interfaz declarada entera.
 */
public interface UserDataHandler {

    /** El nodo se duplico con {@link Node#cloneNode}. */
    public static final short NODE_CLONED = 1;

    /** El nodo se importo a otro documento con {@link Document#importNode}. */
    public static final short NODE_IMPORTED = 2;

    /**
     * El nodo se destruyo.
     *
     * <p>En Java tipicamente no se invoca nunca: no hay un momento definido en que un nodo muera.
     */
    public static final short NODE_DELETED = 3;

    /** El nodo se renombro con {@link Document#renameNode}. */
    public static final short NODE_RENAMED = 4;

    /** El nodo se adopto con {@link Document#adoptNode}. */
    public static final short NODE_ADOPTED = 5;

    /**
     * @param operation uno de los {@code NODE_*}
     * @param key la clave con que se habia guardado el dato
     * @param data el dato guardado
     * @param src el nodo que se clono, importo, renombro o adopto; {@code null} si se borro
     * @param dst el nodo resultante; {@code null} si se borro o se renombro en el lugar
     */
    public void handle(short operation, String key, Object data, Node src, Node dst);
}
