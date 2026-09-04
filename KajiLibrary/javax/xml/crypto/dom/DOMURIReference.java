package javax.xml.crypto.dom;

import javax.xml.crypto.URIReference;
import org.w3c.dom.Node;

/**
 * KajiLibrary's javax.xml.crypto.dom.DOMURIReference -- una referencia por URI que sabe desde donde
 * apunta.
 *
 * <p>Agrega un metodo sobre {@link URIReference}: {@link #getHere}, el nodo donde <b>aparece</b> la
 * referencia.
 *
 * <p>Suena redundante y no lo es. Una referencia con URI {@code ""} significa "el documento entero", y
 * una que empieza con {@code #} apunta dentro del mismo documento; para resolver cualquiera de las dos
 * hace falta saber en que documento esta escrita la referencia. Con el URI solo no alcanza.
 *
 * <p>Ademas, la transformada XPath del estandar de firma define la variable {@code here()}
 * exactamente como este nodo -- de ahi el nombre del metodo.
 */
public interface DOMURIReference extends URIReference {

    /** El nodo donde aparece esta referencia. Ver la nota de la clase. */
    Node getHere();
}
