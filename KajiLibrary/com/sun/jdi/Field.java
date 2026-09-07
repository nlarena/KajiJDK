package com.sun.jdi;

/**
 * Un campo de un tipo de la maquina depurada.
 *
 * <p>El valor no esta aca: se pide con {@code ObjectReference.getValue} para un campo de instancia
 * o con {@code ReferenceType.getValue} para uno estatico.
 *
 * @since 1.3
 */
public interface Field extends TypeComponent, Comparable<Field> {

    /**
     * El type name.
     *
     * @return el resultado
     */
    String typeName();

    /**
     * El tipo.
     *
     * @return el resultado
     * @throws ClassNotLoadedException si corresponde
     */
    Type type()
            throws ClassNotLoadedException;

    /**
     * Si transient.
     *
     * @return el resultado
     */
    boolean isTransient();

    /**
     * Si volatile.
     *
     * @return el resultado
     */
    boolean isVolatile();

    /**
     * Si enum constant.
     *
     * @return el resultado
     */
    boolean isEnumConstant();

    /**
     * Dos reflejos son iguales si nombran a lo mismo en la misma VM.
     *
     * @param obj el Object
     * @return el resultado
     */
    boolean equals(Object obj);

    /**
     * Coherente con {@link #equals}.
     *
     * @return el resultado
     */
    int hashCode();
}
