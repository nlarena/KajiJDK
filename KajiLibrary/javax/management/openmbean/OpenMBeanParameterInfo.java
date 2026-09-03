package javax.management.openmbean;

import java.util.Set;

/**
 * La descripción de un parámetro de un MBean abierto: su nombre, su tipo abierto y, opcionalmente,
 * qué valores acepta.
 *
 * <p>Las restricciones son tres y **se excluyen entre sí**: o hay una lista de valores legales, o
 * hay un mínimo y/o un máximo, o no hay nada. Declarar las dos primeras juntas no tiene sentido
 * --una lista ya dice cuáles valen-- y por eso las implementaciones lo rechazan en vez de intentar
 * combinarlas.
 *
 * <p>Los `hasXxx` existen porque `null` es ambiguo: un `getDefaultValue()` nulo puede significar
 * "no tiene valor por omisión" o "su valor por omisión es nulo". El par pregunta/valor separa las
 * dos cosas.
 */
public interface OpenMBeanParameterInfo {

    /** La descripción, para una persona. */
    String getDescription();

    /** El nombre del parámetro. */
    String getName();

    /** Su tipo abierto. */
    OpenType<?> getOpenType();

    /** El valor por omisión, o nulo si no tiene. Ver la nota sobre los `hasXxx`. */
    Object getDefaultValue();

    /** Los valores legales, o nulo si no están enumerados. */
    Set<?> getLegalValues();

    /** El mínimo, o nulo si no hay. */
    Comparable<?> getMinValue();

    /** El máximo, o nulo si no hay. */
    Comparable<?> getMaxValue();

    /** Si tiene valor por omisión. */
    boolean hasDefaultValue();

    /** Si sus valores legales están enumerados. */
    boolean hasLegalValues();

    /** Si tiene mínimo. */
    boolean hasMinValue();

    /** Si tiene máximo. */
    boolean hasMaxValue();

    /** Si `obj` es un valor válido: del tipo abierto **y** dentro de las restricciones. */
    boolean isValue(Object obj);

    boolean equals(Object obj);

    int hashCode();

    String toString();
}
