package javax.management;

import java.io.Serializable;

/**
 * Una expresion booleana que se evalua contra un MBean: el filtro de `queryNames`/`queryMBeans`.
 *
 * <p>Los cuatro `throws` de {@link #apply} no son ruido, son el contrato: una consulta puede fallar
 * por cuatro motivos distintos --operacion de cadena desconocida, operador binario mal aplicado,
 * valor de atributo incomparable, MBean de clase equivocada-- y quien la evalua tiene que poder
 * distinguirlos para decidir si el MBean simplemente no coincide o si la consulta esta mal armada.
 *
 * <p>Es `Serializable` porque una consulta viaja al agente remoto y se evalua **alla**, no aca.
 */
public interface QueryExp extends Serializable {

    /**
     * Si el MBean llamado `name` satisface la expresion.
     *
     * <p>Las expresiones que necesitan leer atributos lo hacen contra el servidor que les fijo
     * {@link #setMBeanServer}; las que solo miran el nombre --{@link ObjectName} mismo-- lo ignoran.
     */
    boolean apply(ObjectName name)
            throws BadStringOperationException, BadBinaryOpValueExpException,
                   BadAttributeValueExpException, InvalidApplicationException;

    /**
     * Fija el servidor contra el que resolver los atributos.
     *
     * <p>Quedo marcado como obsoleto en el JDK: el servidor viaja hoy por un `ThreadLocal` de
     * {@link QueryEval}, y llamar a esto no hace falta.
     *
     * @deprecated el servidor lo lleva {@link QueryEval}
     */
    @Deprecated
    void setMBeanServer(MBeanServer s);
}
