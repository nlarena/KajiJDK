package javax.xml.crypto.dsig.spec;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * KajiLibrary's javax.xml.crypto.dsig.spec.XPathFilterParameterSpec -- la expresion de una
 * transformacion XPath.
 *
 * <p>La transformacion XPath selecciona <b>que parte</b> del documento se firma. La expresion se
 * evalua sobre cada nodo y el nodo entra si da verdadero.
 *
 * <p>Ese modelo --nodo por nodo-- la hace lenta sobre documentos grandes, y por eso existe
 * {@link XPathFilter2ParameterSpec}, que trabaja por subarboles.
 *
 * <p>El mapa de espacios de nombres hace falta por lo mismo que en {@code javax.xml.xpath}: sin
 * prefijos declarados, una expresion no encuentra nada en un documento con espacios de nombres. Aca
 * pesa mas que en otros lados -- una expresion que no selecciona nada produce una firma que no cubre
 * nada, y valida igual.
 */
public final class XPathFilterParameterSpec implements TransformParameterSpec {

    /** La expresion. */
    private final String xPath;

    /** Prefijo a espacio de nombres; nunca null. */
    private final Map<String, String> nsMap;

    /**
     * Sin espacios de nombres.
     *
     * @throws NullPointerException si la expresion es null
     */
    public XPathFilterParameterSpec(String xPath) {
        if (xPath == null) {
            throw new NullPointerException("xPath cannot be null");
        }
        this.xPath = xPath;
        this.nsMap = Collections.emptyMap();
    }

    /**
     * Con los prefijos declarados.
     *
     * <p>El mapa se copia. Ver la nota de la clase sobre por que casi siempre hace falta.
     *
     * @throws NullPointerException si alguno de los dos es null
     */
    public XPathFilterParameterSpec(String xPath, Map<String, String> namespaceMap) {
        if (xPath == null || namespaceMap == null) {
            throw new NullPointerException("xPath and namespaceMap cannot be null");
        }
        this.xPath = xPath;
        this.nsMap = Collections.unmodifiableMap(new HashMap<String, String>(namespaceMap));
    }

    /** La expresion. */
    public String getXPath() {
        return this.xPath;
    }

    /** Los prefijos declarados. No modificable. */
    public Map<String, String> getNamespaceMap() {
        return this.nsMap;
    }
}
