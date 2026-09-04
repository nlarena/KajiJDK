package javax.xml.crypto.dsig.dom;

import java.security.Key;
import javax.xml.crypto.KeySelector;
import javax.xml.crypto.dom.DOMCryptoContext;
import javax.xml.crypto.dsig.XMLSignContext;
import org.w3c.dom.Node;

/**
 * KajiLibrary's javax.xml.crypto.dsig.dom.DOMSignContext -- donde y con que firmar, sobre DOM.
 *
 * <p>Lleva dos cosas: la clave, y <b>en que lugar del arbol</b> escribir el elemento de firma.
 *
 * <h2>Padre y hermano siguiente</h2>
 *
 * <p>El padre es obligatorio y dice bajo que elemento cuelga la firma. El hermano siguiente es
 * opcional y decide la posicion exacta: la firma se inserta <b>antes</b> de el, y sin el se agrega al
 * final.
 *
 * <p>Importa mas de lo que parece. Hay esquemas que fijan el orden de los hijos, y una firma agregada
 * al final rompe la validacion del esquema aunque la firma en si sea correcta.
 *
 * <h2>Clave o selector</h2>
 *
 * <p>Los constructores vienen de a pares. Con una {@link Key} se firma con esa y punto; con un
 * {@link KeySelector} la clave se elige durante la operacion, mirando el {@code KeyInfo}. Para firmar
 * lo normal es la clave directa -- el selector tiene mas sentido al validar.
 *
 * <h2>El arbol se modifica</h2>
 *
 * <p>Firmar <b>inserta</b> el elemento de firma en el documento que se paso. No es una operacion de
 * solo lectura, y el documento tiene que ser modificable.
 */
public class DOMSignContext extends DOMCryptoContext implements XMLSignContext {

    /** Bajo que elemento cuelga la firma. */
    private Node parent;

    /** Antes de cual insertarla, o null para el final. */
    private Node nextSibling;

    /**
     * Firma con esa clave, colgando del final de ese elemento.
     *
     * @throws NullPointerException si alguno de los dos es null
     */
    public DOMSignContext(Key signingKey, Node parent) {
        if (signingKey == null) {
            throw new NullPointerException("signingKey cannot be null");
        }
        if (parent == null) {
            throw new NullPointerException("parent cannot be null");
        }
        this.parent = parent;
        setKeySelector(KeySelector.singletonKeySelector(signingKey));
    }

    /**
     * Idem, insertando antes de ese hermano. Ver la nota de la clase.
     *
     * @throws NullPointerException si la clave, el padre o el hermano son null
     */
    public DOMSignContext(Key signingKey, Node parent, Node nextSibling) {
        this(signingKey, parent);
        if (nextSibling == null) {
            throw new NullPointerException("nextSibling cannot be null");
        }
        this.nextSibling = nextSibling;
    }

    /**
     * Firma con la clave que elija ese selector.
     *
     * @throws NullPointerException si alguno de los dos es null
     */
    public DOMSignContext(KeySelector ks, Node parent) {
        if (ks == null) {
            throw new NullPointerException("key selector cannot be null");
        }
        if (parent == null) {
            throw new NullPointerException("parent cannot be null");
        }
        this.parent = parent;
        setKeySelector(ks);
    }

    /**
     * Idem, insertando antes de ese hermano.
     *
     * @throws NullPointerException si alguno de los tres es null
     */
    public DOMSignContext(KeySelector ks, Node parent, Node nextSibling) {
        this(ks, parent);
        if (nextSibling == null) {
            throw new NullPointerException("nextSibling cannot be null");
        }
        this.nextSibling = nextSibling;
    }

    /**
     * Cambia bajo que elemento cuelga.
     *
     * @throws NullPointerException si es null; el padre no es opcional
     */
    public void setParent(Node parent) {
        if (parent == null) {
            throw new NullPointerException("parent is null");
        }
        this.parent = parent;
    }

    /**
     * Cambia antes de cual insertarla.
     *
     * <p>Null es valido aca y significa "al final"; ver la nota de la clase.
     */
    public void setNextSibling(Node nextSibling) {
        this.nextSibling = nextSibling;
    }

    /** Bajo que elemento cuelga. */
    public Node getParent() {
        return this.parent;
    }

    /** Antes de cual se inserta, o null. */
    public Node getNextSibling() {
        return this.nextSibling;
    }
}
