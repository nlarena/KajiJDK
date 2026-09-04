package javax.xml.crypto.dom;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.xml.crypto.KeySelector;
import javax.xml.crypto.URIDereferencer;
import javax.xml.crypto.XMLCryptoContext;
import org.w3c.dom.Element;

/**
 * KajiLibrary's javax.xml.crypto.dom.DOMCryptoContext -- el contexto de una operacion de firma XML
 * sobre DOM.
 *
 * <p>La implementacion comun de {@link XMLCryptoContext} para el mecanismo DOM.
 * {@code DOMSignContext} y {@code DOMValidateContext} heredan de aca todo lo que comparten.
 *
 * <h2>Tres mapas distintos, y se confunden</h2>
 *
 * <p>Es lo unico complicado de la clase. Guarda tres cosas separadas:
 *
 * <ul>
 *   <li>los <b>prefijos de espacio de nombres</b> ({@link #putNamespacePrefix}): con que prefijo
 *       escribir cada espacio de nombres al generar la firma;
 *   <li>las <b>propiedades</b> ({@link #setProperty}): configuracion de la implementacion;
 *   <li>el mapa de <b>objetos de contexto</b> ({@link #put}): datos que las partes de la operacion se
 *       pasan entre si mientras corre.
 * </ul>
 *
 * <p>Los tres tienen su par de metodos y <b>no se cruzan</b>: lo que se guarda con {@code setProperty}
 * no sale por {@code get}. Comprobado contra el JDK 25.
 *
 * <h2>Los identificadores tienen que declararse</h2>
 *
 * <p>{@link #getElementById} no busca en el documento: busca en un registro que se llena a mano con
 * {@link #setIdAttributeNS}.
 *
 * <p>Suena incomodo y es una decision de seguridad. Un DOM sin esquema no sabe que atributos son
 * identificadores, y adivinar --tomar cualquier atributo llamado {@code Id}-- permite que un documento
 * hostil declare un identificador falso y haga que la firma valide contra otro contenido. Registrarlos
 * explicitamente es lo que cierra esa puerta.
 *
 * <p>{@link #iterator} recorre ese registro, y es de solo lectura: intentar sacar una entrada con el
 * iterador lanza {@link UnsupportedOperationException}.
 */
public class DOMCryptoContext implements XMLCryptoContext {

    /** Espacio de nombres a prefijo. */
    private final HashMap<String, String> nsMap = new HashMap<String, String>();

    /** Identificador a elemento. Ver la nota de la clase. */
    private final HashMap<String, Element> idMap = new HashMap<String, Element>();

    /** Los objetos que las partes de la operacion se pasan. */
    private final HashMap<Object, Object> objMap = new HashMap<Object, Object>();

    /** La base contra la que se resuelven los URI relativos. */
    private String baseURI;

    /** Con que se eligen las claves. */
    private KeySelector ks;

    /** Como se resuelven las referencias. */
    private URIDereferencer dereferencer;

    /** La configuracion de la implementacion. */
    private final HashMap<String, Object> propMap = new HashMap<String, Object>();

    /** El prefijo para el espacio de nombres por omision. */
    private String defaultPrefix;

    /** Para las subclases; no se instancia directo. */
    protected DOMCryptoContext() {
    }

    /**
     * Con que prefijo escribir ese espacio de nombres.
     *
     * @param defaultPrefix que devolver si no hay ninguno registrado
     * @throws NullPointerException si el espacio de nombres es null
     */
    public String getNamespacePrefix(String namespaceURI, String defaultPrefix) {
        if (namespaceURI == null) {
            throw new NullPointerException("namespaceURI cannot be null");
        }
        String prefix = this.nsMap.get(namespaceURI);
        if (prefix != null) {
            return prefix;
        }
        return defaultPrefix;
    }

    /**
     * Registra un prefijo.
     *
     * @return el que estaba antes, o null
     * @throws NullPointerException si el espacio de nombres es null
     */
    public String putNamespacePrefix(String namespaceURI, String prefix) {
        if (namespaceURI == null) {
            throw new NullPointerException("namespaceURI is null");
        }
        return this.nsMap.put(namespaceURI, prefix);
    }

    /** El prefijo para el espacio de nombres por omision, o null. */
    public String getDefaultNamespacePrefix() {
        return this.defaultPrefix;
    }

    /** Lo fija; null vuelve al comportamiento por omision. */
    public void setDefaultNamespacePrefix(String defaultPrefix) {
        this.defaultPrefix = defaultPrefix;
    }

    /** Contra que se resuelven los URI relativos, o null. */
    public String getBaseURI() {
        return this.baseURI;
    }

    /**
     * La fija.
     *
     * <p>Se valida al fijarla y no al usarla: un URI mal formado se descubre en el sitio que lo
     * escribio, no adentro de una firma.
     *
     * @throws IllegalArgumentException si no es un URI valido
     */
    public void setBaseURI(String baseURI) {
        if (baseURI != null) {
            try {
                new URI(baseURI);
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        }
        this.baseURI = baseURI;
    }

    /** Como se resuelven las referencias, o null para el de la implementacion. */
    public URIDereferencer getURIDereferencer() {
        return this.dereferencer;
    }

    /** Lo cambia; null vuelve al de la implementacion. */
    public void setURIDereferencer(URIDereferencer dereferencer) {
        this.dereferencer = dereferencer;
    }

    /**
     * Una propiedad de la implementacion. Ver la nota de la clase: no es el mapa de {@link #get}.
     *
     * @throws NullPointerException si el nombre es null
     */
    public Object getProperty(String name) {
        if (name == null) {
            throw new NullPointerException("name is null");
        }
        return this.propMap.get(name);
    }

    /**
     * La fija.
     *
     * @return la que estaba antes, o null
     * @throws NullPointerException si el nombre es null
     */
    public Object setProperty(String name, Object value) {
        if (name == null) {
            throw new NullPointerException("name is null");
        }
        return this.propMap.put(name, value);
    }

    /** Con que se eligen las claves, o null. */
    public KeySelector getKeySelector() {
        return this.ks;
    }

    /** Lo cambia. */
    public void setKeySelector(KeySelector ks) {
        this.ks = ks;
    }

    /**
     * El elemento con ese identificador, o null.
     *
     * <p>Solo encuentra los que se registraron con {@link #setIdAttributeNS}; ver la nota de la clase.
     *
     * @throws NullPointerException si el identificador es null
     */
    public Element getElementById(String idValue) {
        if (idValue == null) {
            throw new NullPointerException("idValue is null");
        }
        return this.idMap.get(idValue);
    }

    /**
     * Declara que ese atributo de ese elemento es su identificador.
     *
     * <p>Ver la nota de la clase sobre por que hace falta declararlo.
     *
     * @param namespaceURI el del atributo, o null si no tiene
     * @param localName el nombre local del atributo
     * @throws NullPointerException si el elemento o el nombre local son null
     * @throws IllegalArgumentException si el elemento no tiene ese atributo
     */
    public void setIdAttributeNS(Element element, String namespaceURI, String localName) {
        if (element == null) {
            throw new NullPointerException("element is null");
        }
        if (localName == null) {
            throw new NullPointerException("localName is null");
        }
        String idValue = element.getAttributeNS(namespaceURI, localName);
        if (idValue == null || idValue.length() == 0) {
            throw new IllegalArgumentException(localName + " is not an attribute");
        }
        this.idMap.put(idValue, element);
    }

    /**
     * Recorre los identificadores registrados.
     *
     * <p>De solo lectura: el {@code remove} del iterador lanza
     * {@link UnsupportedOperationException}.
     */
    public Iterator<Map.Entry<String, Element>> iterator() {
        return java.util.Collections.unmodifiableMap(this.idMap).entrySet().iterator();
    }

    /**
     * Un objeto del mapa de contexto. Ver la nota de la clase: no es el de las propiedades.
     *
     * <p>Admite clave null, a diferencia de las otras dos.
     */
    public Object get(Object key) {
        return this.objMap.get(key);
    }

    /**
     * Lo guarda.
     *
     * @return el que estaba antes, o null
     */
    public Object put(Object key, Object value) {
        return this.objMap.put(key, value);
    }
}
