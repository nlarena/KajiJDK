package javax.management;

import java.io.Serializable;

/**
 * Metadatos abiertos que se cuelgan de cualquier pieza de un {@link MBeanInfo}.
 *
 * <p>Es la valvula de escape del modelo: `MBeanInfo` y compa&ntilde;ia declaran lo que JMX sabe
 * nombrar --tipo, lectura, escritura-- y todo lo demas (unidades, si el valor es acumulativo, si
 * conviene mostrarlo, lo que se le ocurra al que modela) entra por aca como campos con nombre.
 *
 * <p><b>Los nombres de campo no distinguen mayusculas.</b> Es la regla que mas se olvida: pedir
 * {@code "units"} y {@code "Units"} da lo mismo, y un descriptor no puede tener los dos.
 */
public interface Descriptor extends Serializable, Cloneable {

    /**
     * El valor del campo, o `null` si no esta.
     *
     * @throws RuntimeOperationsException si el nombre es nulo o vacio
     */
    Object getFieldValue(String fieldName) throws RuntimeOperationsException;

    /**
     * Fija un campo.
     *
     * @throws RuntimeOperationsException si el nombre no sirve, o si el descriptor es inmutable
     */
    void setField(String fieldName, Object fieldValue) throws RuntimeOperationsException;

    /** Todos los campos como {@code "nombre=valor"}. */
    String[] getFields();

    /** Solo los nombres. */
    String[] getFieldNames();

    /**
     * Los valores de los nombres dados, en el mismo orden.
     *
     * <p>Sin argumentos devuelve <b>todos</b> los valores, no ninguno: es la variante que se usa
     * junto con {@link #getFieldNames()}.
     */
    Object[] getFieldValues(String... fieldNames);

    /** Saca un campo; si no estaba, no hace nada. */
    void removeField(String fieldName);

    /**
     * Fija varios campos de una.
     *
     * @throws RuntimeOperationsException si los arreglos no miden lo mismo o algun nombre no sirve
     */
    void setFields(String[] fieldNames, Object[] fieldValues) throws RuntimeOperationsException;

    /** Una copia. Los inmutables se devuelven a si mismos, que es copia suficiente. */
    Object clone() throws RuntimeOperationsException;

    /** Si los campos que JMX si conoce tienen valores admisibles. */
    boolean isValid() throws RuntimeOperationsException;

    /** Por conjunto de campos, con los nombres comparados sin distinguir mayusculas. */
    boolean equals(Object obj);

    int hashCode();
}
