package javax.xml.xpath;

import java.util.Iterator;
import org.w3c.dom.Node;

/**
 * KajiLibrary's javax.xml.xpath.XPathNodes -- un conjunto de nodos que se puede recorrer con
 * {@code for}.
 *
 * <p>Existe para reemplazar a {@code org.w3c.dom.NodeList} como resultado, y la diferencia es toda
 * de comodidad: {@code NodeList} es de 1998, no es {@code Iterable}, y recorrerla pide un bucle con
 * indice y un cast por elemento. Esta es {@code Iterable<Node>}, asi que entra en un {@code for}
 * mejorado sin ceremonia.
 *
 * <p>{@link #get} lanza en vez de devolver null fuera de rango, al reves que {@code NodeList#item}.
 * Es el criterio moderno y es el correcto: un indice fuera de rango es un error de quien programa, y
 * un null se propaga hasta explotar en otro lado.
 */
public interface XPathNodes extends Iterable<Node> {

    /** Los nodos, en orden de documento. */
    Iterator<Node> iterator();

    /** Cuantos hay. */
    int size();

    /**
     * El de esa posicion.
     *
     * @throws XPathException si el indice esta fuera de rango; ver la nota de la clase
     */
    Node get(int index) throws XPathException;
}
