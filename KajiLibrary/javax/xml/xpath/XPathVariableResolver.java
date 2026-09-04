package javax.xml.xpath;

import javax.xml.namespace.QName;

/**
 * KajiLibrary's javax.xml.xpath.XPathVariableResolver -- de donde salen las variables.
 *
 * <p>Una expresion puede nombrar variables con {@code $nombre}, y XPath no tiene forma de
 * asignarlas: se resuelven por afuera, con esto.
 *
 * <p>Es lo que evita armar expresiones concatenando texto, que es el equivalente en XPath de la
 * inyeccion SQL: un valor con una comilla adentro cambia lo que la expresion selecciona. Con una
 * variable, el valor nunca pasa por el parser.
 *
 * <p>Se lo consulta <b>en cada evaluacion</b>, no al compilar. Por eso la misma expresion compilada
 * sirve para muchos valores distintos, que es el otro motivo para usarlas.
 */
public interface XPathVariableResolver {

    /**
     * El valor de esa variable.
     *
     * @return null si este resolvedor no la conoce
     */
    Object resolveVariable(QName variableName);
}
