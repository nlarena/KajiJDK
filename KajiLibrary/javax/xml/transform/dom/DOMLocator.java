package javax.xml.transform.dom;

import javax.xml.transform.SourceLocator;
import org.w3c.dom.Node;

/**
 * KajiLibrary's javax.xml.transform.dom.DOMLocator -- donde fallo, cuando la fuente era un arbol.
 *
 * <p>Extiende {@link SourceLocator} agregando un solo metodo, y ese metodo existe porque lo que el
 * padre ofrece --numero de linea y de columna-- <b>no significa nada</b> en un arbol DOM. Un nodo en
 * memoria no tiene linea: si el documento se construyo a mano nunca la tuvo, y si se leyo de un
 * archivo, el DOM no la guarda.
 *
 * <p>Asi que un error en una transformacion sobre {@link DOMSource} solo puede senalar el lugar
 * apuntando al nodo mismo, y eso es {@link #getOriginatingNode}. Quien atrapa el error lo usa para
 * mostrar el contexto: el nombre del elemento, sus atributos, su camino hasta la raiz.
 */
public interface DOMLocator extends SourceLocator {

    /** El nodo donde ocurrio lo que se esta reportando. */
    Node getOriginatingNode();
}
