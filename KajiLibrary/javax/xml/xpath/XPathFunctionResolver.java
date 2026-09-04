package javax.xml.xpath;

import javax.xml.namespace.QName;

/**
 * KajiLibrary's javax.xml.xpath.XPathFunctionResolver -- de donde salen las funciones propias.
 *
 * <p>Se le pregunta por nombre <b>y cantidad de argumentos</b>, porque en XPath dos funciones con el
 * mismo nombre y distinta aridad son funciones distintas. No hay forma de enumerar lo que un
 * resolvedor ofrece: solo se le puede preguntar por una en concreto.
 *
 * <p>El nombre viene calificado con espacio de nombres, y eso no es decoracion: una funcion propia
 * <b>tiene</b> que estar en un espacio de nombres propio. Sin prefijo, el nombre cae en el de las
 * funciones incorporadas de XPath, donde no se puede agregar nada.
 */
public interface XPathFunctionResolver {

    /**
     * La funcion con ese nombre y esa aridad.
     *
     * @return null si este resolvedor no la conoce
     */
    XPathFunction resolveFunction(QName functionName, int arity);
}
