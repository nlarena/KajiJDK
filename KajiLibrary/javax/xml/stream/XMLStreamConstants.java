package javax.xml.stream;

/**
 * KajiLibrary's javax.xml.stream.XMLStreamConstants -- los quince tipos de evento que puede
 * devolver un lector de StAX.
 *
 * <p>Es una interfaz sin metodos, y esa forma tiene un motivo historico que conviene saber:
 * {@link XMLStreamReader} la **extiende**, asi que quien programa contra el lector escribe
 * {@code reader.next() == START_ELEMENT} sin importar nada. Hoy se escribiria como una clase de
 * constantes o un enum; en 2004 la herencia de constantes era la forma normal de conseguir eso.
 *
 * <p>Los valores son los del contrato --1 a 15, en el orden en que estan declarados-- y hay codigo
 * que los persiste, asi que no son un detalle interno.
 *
 * <h2>Los que un lector devuelve y los que no</h2>
 *
 * <p>La lista es mas larga que los estados por los que pasa un recorrido normal, y la diferencia
 * importa:
 *
 * <ul>
 *   <li>{@link #START_DOCUMENT}, {@link #START_ELEMENT}, {@link #END_ELEMENT}, {@link #CHARACTERS},
 *       {@link #CDATA}, {@link #SPACE}, {@link #COMMENT}, {@link #PROCESSING_INSTRUCTION},
 *       {@link #ENTITY_REFERENCE}, {@link #DTD} y {@link #END_DOCUMENT} salen de
 *       {@link XMLStreamReader#next()};
 *   <li>{@link #ATTRIBUTE}, {@link #NAMESPACE}, {@link #NOTATION_DECLARATION} y
 *       {@link #ENTITY_DECLARATION} **no**: son tipos de {@link javax.xml.stream.events.XMLEvent}
 *       que existen porque el modelo de eventos necesita nombrar esas cosas, pero el lector de
 *       cursor las expone como propiedades del elemento o del DTD, no como pasos del recorrido.
 * </ul>
 */
public interface XMLStreamConstants {

    /** La apertura de un elemento: {@code <a>}. */
    int START_ELEMENT = 1;

    /** El cierre de un elemento: {@code </a>}. Un {@code <a/>} produce los dos, apertura y cierre. */
    int END_ELEMENT = 2;

    /** Una instruccion de proceso: {@code <?destino datos?>}. */
    int PROCESSING_INSTRUCTION = 3;

    /**
     * Texto.
     *
     * <p>Un mismo pedazo de texto puede llegar partido en varios eventos si la fabrica no tiene
     * puesto {@code isCoalescing}; eso no es un capricho sino la consecuencia de que el parser lea
     * por bloques.
     */
    int CHARACTERS = 4;

    /** Un comentario: {@code <!-- ... -->}. */
    int COMMENT = 5;

    /**
     * Espacio en blanco ignorable.
     *
     * <p>Solo se puede distinguir del texto comun cuando hay un DTD o un esquema que diga que ese
     * elemento no lleva contenido mixto; sin validacion, el mismo espacio llega como
     * {@link #CHARACTERS}.
     */
    int SPACE = 6;

    /** El comienzo del documento, antes de la raiz; lleva la version, la codificacion y standalone. */
    int START_DOCUMENT = 7;

    /** El fin del documento; despues de esto {@code hasNext()} da false. */
    int END_DOCUMENT = 8;

    /** Una referencia a entidad que no se reemplazo: {@code &nombre;}. */
    int ENTITY_REFERENCE = 9;

    /** Un atributo, como evento. El lector de cursor no lo devuelve; ver el encabezado. */
    int ATTRIBUTE = 10;

    /** La declaracion de tipo de documento: {@code <!DOCTYPE ...>}. */
    int DTD = 11;

    /** Una seccion {@code <![CDATA[...]]>}, cuando la fabrica no la funde con el texto de al lado. */
    int CDATA = 12;

    /** Una declaracion de espacio de nombres, como evento. Tampoco sale del lector de cursor. */
    int NAMESPACE = 13;

    /** Una {@code <!NOTATION ...>} del DTD. */
    int NOTATION_DECLARATION = 14;

    /** Una {@code <!ENTITY ...>} del DTD. */
    int ENTITY_DECLARATION = 15;
}
