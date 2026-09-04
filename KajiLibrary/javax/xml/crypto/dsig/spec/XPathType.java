package javax.xml.crypto.dsig.spec;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * KajiLibrary's javax.xml.crypto.dsig.spec.XPathType -- una expresion XPath con su operacion de
 * conjunto.
 *
 * <p>Es la pieza de {@link XPathFilter2ParameterSpec}: cada expresion viene con una operacion que
 * dice que hacer con lo que selecciona.
 *
 * <ul>
 *   <li>{@link Filter#INTERSECT} -- se queda con lo que este en los dos;
 *   <li>{@link Filter#SUBTRACT} -- saca lo seleccionado, <b>con todo su subarbol</b>;
 *   <li>{@link Filter#UNION} -- lo agrega.
 * </ul>
 *
 * <p>Que trabajen por subarboles y no por nodo es la diferencia de fondo con la transformacion XPath
 * original: seleccionar un elemento se lleva sus descendientes, que es lo que uno espera y lo que
 * hace que el filtrado sea rapido.
 *
 * <p>El orden importa: las expresiones se aplican en secuencia sobre el resultado acumulado, asi que
 * restar antes o despues de unir da conjuntos distintos.
 */
public class XPathType {

    /** La expresion. */
    private final String expression;

    /** Que hacer con lo que selecciona. */
    private final Filter filter;

    /** Prefijo a espacio de nombres; nunca null. */
    private final Map<String, String> nsMap;

    /**
     * Sin espacios de nombres.
     *
     * @throws NullPointerException si alguno es null
     */
    public XPathType(String expression, Filter filter) {
        if (expression == null || filter == null) {
            throw new NullPointerException("expression and filter cannot be null");
        }
        this.expression = expression;
        this.filter = filter;
        this.nsMap = Collections.emptyMap();
    }

    /**
     * Con los prefijos declarados; el mapa se copia.
     *
     * @throws NullPointerException si alguno es null
     */
    public XPathType(String expression, Filter filter, Map<String, String> namespaceMap) {
        if (expression == null || filter == null || namespaceMap == null) {
            throw new NullPointerException(
                "expression, filter and namespaceMap cannot be null");
        }
        this.expression = expression;
        this.filter = filter;
        this.nsMap = Collections.unmodifiableMap(new HashMap<String, String>(namespaceMap));
    }

    /** La expresion. */
    public String getExpression() {
        return this.expression;
    }

    /** Que hacer con lo que selecciona. */
    public Filter getFilter() {
        return this.filter;
    }

    /** Los prefijos declarados. No modificable. */
    public Map<String, String> getNamespaceMap() {
        return this.nsMap;
    }

    /**
     * Las tres operaciones de conjunto.
     *
     * <p>No es un enum porque la clase es de 2005; son tres constantes con constructor privado, el
     * patron de enum a mano de la epoca.
     */
    public static class Filter {

        /** Se queda con lo que este en los dos. */
        public static final Filter INTERSECT = new Filter("intersect");

        /** Lo saca, con todo su subarbol. */
        public static final Filter SUBTRACT = new Filter("subtract");

        /** Lo agrega. */
        public static final Filter UNION = new Filter("union");

        private final String operation;

        private Filter(String operation) {
            this.operation = operation;
        }

        /** El nombre de la operacion, tal como va en el XML. */
        public String toString() {
            return this.operation;
        }
    }
}
