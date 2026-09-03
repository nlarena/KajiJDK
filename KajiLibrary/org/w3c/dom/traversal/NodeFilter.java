package org.w3c.dom.traversal;

import org.w3c.dom.Node;

/**
 * KajiLibrary's org.w3c.dom.traversal.NodeFilter -- que nodos se ven al recorrer un documento.
 *
 * <h2>Tres respuestas, no dos</h2>
 *
 * <p>Es lo unico que hay que entender de esta interfaz, y la diferencia entre las dos negativas es
 * la que se olvida:
 *
 * <ul>
 *   <li>{@link #FILTER_ACCEPT} -- el nodo se ve.
 *   <li>{@link #FILTER_SKIP} -- el nodo no se ve, <b>pero sus hijos si</b>.
 *   <li>{@link #FILTER_REJECT} -- el nodo no se ve <b>y su subarbol entero tampoco</b>.
 * </ul>
 *
 * <p>Y hay una asimetria: un {@link NodeIterator} trata {@code FILTER_REJECT} como
 * {@code FILTER_SKIP}, porque recorre una lista plana y no tiene subarbol que podar. La distincion
 * solo cambia algo en un {@link TreeWalker}.
 *
 * <h2>El filtro y whatToShow son dos tamices en serie</h2>
 *
 * <p>El {@code whatToShow} del recorrido se aplica <b>primero</b>: un nodo de un tipo que no esta en
 * la mascara ni siquiera llega al filtro. Por eso un filtro que quiera ver comentarios no alcanza --
 * hay que pedir {@link #SHOW_COMMENT} tambien.
 */
public interface NodeFilter {

    /** El nodo se ve. */
    short FILTER_ACCEPT = 1;

    /** El nodo no se ve, y su subarbol tampoco. Ver la nota de la clase. */
    short FILTER_REJECT = 2;

    /** El nodo no se ve, pero sus hijos si. */
    short FILTER_SKIP = 3;

    /** Todos los tipos de nodo. */
    int SHOW_ALL = 0xFFFFFFFF;

    /** Elementos. */
    int SHOW_ELEMENT = 0x00000001;

    /** Atributos. Solo tiene sentido si la raiz del recorrido es un atributo. */
    int SHOW_ATTRIBUTE = 0x00000002;

    /** Nodos de texto. */
    int SHOW_TEXT = 0x00000004;

    /** Secciones CDATA. */
    int SHOW_CDATA_SECTION = 0x00000008;

    /** Referencias a entidad. */
    int SHOW_ENTITY_REFERENCE = 0x00000010;

    /** Entidades. Solo si la raiz es la entidad. */
    int SHOW_ENTITY = 0x00000020;

    /** Instrucciones de procesamiento. */
    int SHOW_PROCESSING_INSTRUCTION = 0x00000040;

    /** Comentarios. */
    int SHOW_COMMENT = 0x00000080;

    /** El nodo documento. */
    int SHOW_DOCUMENT = 0x00000100;

    /** La declaracion de tipo de documento. */
    int SHOW_DOCUMENT_TYPE = 0x00000200;

    /** Fragmentos de documento. */
    int SHOW_DOCUMENT_FRAGMENT = 0x00000400;

    /** Notaciones. Solo si la raiz es la notacion. */
    int SHOW_NOTATION = 0x00000800;

    /**
     * Decide si ese nodo se ve.
     *
     * @return una de las tres constantes {@code FILTER_*}
     */
    short acceptNode(Node n);
}
