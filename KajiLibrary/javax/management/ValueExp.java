package javax.management;

import java.io.Serializable;

/**
 * Un valor dentro de una consulta: constante, atributo de un MBean, o cuenta entre dos.
 *
 * <p>La firma que la define es {@link #apply}: devuelve **otro** `ValueExp`, no un `Object`. Eso es
 * lo que hace que una expresion se resuelva en pasos --`a + b` aplica a los dos lados, cada uno
 * devuelve una constante, y la suma devuelve una constante-- sin salirse nunca del tipo.
 */
public interface ValueExp extends Serializable {

    /**
     * Resuelve la expresion para el MBean dado y devuelve el valor, ya constante.
     */
    ValueExp apply(ObjectName name)
            throws BadStringOperationException, BadBinaryOpValueExpException,
                   BadAttributeValueExpException, InvalidApplicationException;

    /**
     * @deprecated el servidor lo lleva {@link QueryEval}
     */
    @Deprecated
    void setMBeanServer(MBeanServer s);
}
