package javax.xml.crypto.dsig.dom;

import java.security.Key;
import javax.xml.crypto.KeySelector;
import javax.xml.crypto.dom.DOMCryptoContext;
import javax.xml.crypto.dsig.XMLValidateContext;
import org.w3c.dom.Node;

/**
 * KajiLibrary's javax.xml.crypto.dsig.dom.DOMValidateContext -- que firma validar, sobre DOM.
 *
 * <p>El espejo de {@link DOMSignContext}, y mas simple: un solo nodo, el elemento
 * {@code Signature} que se quiere validar. No hay padre ni hermano porque validar <b>no modifica</b>
 * el arbol.
 *
 * <h2>Clave o selector: aca la diferencia importa</h2>
 *
 * <p>Con una {@link Key} se valida contra esa clave y solo esa. Con un {@link KeySelector} la clave se
 * elige mirando el {@code KeyInfo} de la propia firma.
 *
 * <p>Y ahi esta el riesgo que hay que entender: un {@code KeyInfo} lo escribe quien firmo, que puede
 * ser cualquiera. Un selector que confie en el valida cualquier firma bien armada, con la clave que el
 * atacante quiera. La clave directa, o un selector que consulte un almacen de confianza propio, son
 * las dos formas correctas.
 *
 * <h2>Los identificadores</h2>
 *
 * <p>Si la firma tiene referencias de la forma {@code #id}, hay que registrar esos identificadores con
 * {@code setIdAttributeNS} antes de validar; ver {@link DOMCryptoContext}. Es la parte que mas seguido
 * hace fallar una validacion que deberia andar.
 */
public class DOMValidateContext extends DOMCryptoContext implements XMLValidateContext {

    /** El elemento de firma a validar. */
    private Node node;

    /**
     * Valida lo que el selector elija. Ver la nota de la clase sobre el riesgo.
     *
     * @throws NullPointerException si alguno de los dos es null
     */
    public DOMValidateContext(KeySelector ks, Node node) {
        if (ks == null) {
            throw new NullPointerException("key selector is null");
        }
        if (node == null) {
            throw new NullPointerException("node is null");
        }
        this.node = node;
        setKeySelector(ks);
    }

    /**
     * Valida contra esa clave y solo esa.
     *
     * @throws NullPointerException si alguno de los dos es null
     */
    public DOMValidateContext(Key validatingKey, Node node) {
        if (validatingKey == null) {
            throw new NullPointerException("validatingKey is null");
        }
        if (node == null) {
            throw new NullPointerException("node is null");
        }
        this.node = node;
        setKeySelector(KeySelector.singletonKeySelector(validatingKey));
    }

    /**
     * Cambia el elemento a validar.
     *
     * @throws NullPointerException si es null
     */
    public void setNode(Node node) {
        if (node == null) {
            throw new NullPointerException();
        }
        this.node = node;
    }

    /** El elemento a validar. */
    public Node getNode() {
        return this.node;
    }
}
