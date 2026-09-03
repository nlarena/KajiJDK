package org.w3c.dom.ls;

import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * KajiLibrary's org.w3c.dom.ls.LSParser -- el analizador de la especificacion del W3C.
 *
 * <p>Hace lo mismo que {@code javax.xml.parsers.DocumentBuilder} y no es un duplicado por descuido:
 * este viene de la especificacion DOM Level 3 y aquel de la plataforma Java. Conviven porque codigo
 * escrito contra el W3C tiene que poder correr en Java sin reescribirse.
 *
 * <p>Lo que este tiene y el otro no es {@link #parseWithContext}, que analiza un fragmento y lo
 * inserta <b>dentro</b> de un documento que ya existe. No es azucar: analizar un fragmento suelto y
 * despues importarlo pierde el contexto --los espacios de nombres declarados en los ancestros, las
 * entidades del documento-- y puede dar un arbol distinto.
 *
 * <p>La configuracion no son metodos sino un {@link DOMConfiguration} con parametros por nombre. Es
 * mas flojo de tipos y a cambio deja que una implementacion agregue opciones sin cambiar la
 * interfaz.
 */
public interface LSParser {

    /** El fragmento se agrega al final de los hijos del nodo de contexto. */
    short ACTION_APPEND_AS_CHILDREN = 1;

    /** Reemplaza a todos los hijos del nodo de contexto. */
    short ACTION_REPLACE_CHILDREN = 2;

    /** Se inserta antes del nodo de contexto, como hermano. */
    short ACTION_INSERT_BEFORE = 3;

    /** Se inserta despues del nodo de contexto, como hermano. */
    short ACTION_INSERT_AFTER = 4;

    /** Reemplaza al nodo de contexto. */
    short ACTION_REPLACE = 5;

    /** Los parametros del analizador, por nombre. Ver la nota de la clase. */
    DOMConfiguration getDomConfig();

    /** El filtro que decide que entra al arbol, o null. */
    LSParserFilter getFilter();

    /** Ver {@link #getFilter}. */
    void setFilter(LSParserFilter filter);

    /**
     * Si trabaja en segundo plano.
     *
     * <p>Es una propiedad del analizador y no de la llamada: se elige al crearlo con
     * {@link DOMImplementationLS#createLSParser} y despues no cambia.
     */
    boolean getAsync();

    /**
     * Si esta ocupado con un analisis.
     *
     * <p>Llamar a {@code parse} sobre uno ocupado es un error, y esta es la forma de preguntarlo sin
     * provocarlo.
     */
    boolean getBusy();

    /**
     * Analiza y devuelve un documento nuevo.
     *
     * @throws LSException con {@link LSException#PARSE_ERR} si no se pudo
     */
    Document parse(LSInput input) throws DOMException, LSException;

    /** Idem, leyendo de un URI. */
    Document parseURI(String uri) throws DOMException, LSException;

    /**
     * Analiza un fragmento y lo mete en un documento que ya existe.
     *
     * <p>Ver la nota de la clase sobre por que esto no es lo mismo que analizar aparte e importar.
     *
     * @param contextArg el nodo relativo al cual se ubica lo leido
     * @param action una de las cinco constantes de arriba
     * @return el nodo resultante, que puede no ser el mismo que se leyo
     */
    Node parseWithContext(LSInput input, Node contextArg, short action)
        throws DOMException, LSException;

    /**
     * Corta un analisis en curso.
     *
     * <p>Tiene sentido sobre todo en el modo asincronico; sobre uno sincronico solo lo puede llamar
     * el filtro o un manejador de eventos, que son los unicos que corren mientras se analiza.
     */
    void abort();
}
