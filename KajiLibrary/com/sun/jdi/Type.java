package com.sun.jdi;

/**
 * Un tipo de la maquina depurada.
 *
 * <p>{@link PrimitiveType} para los ocho primitivos y {@link ReferenceType} para todo lo demas. La
 * diferencia importa porque solo un {@code ReferenceType} tiene miembros, codigo y un cargador de
 * clases detras.
 *
 * @since 1.3
 */
public interface Type extends Mirror {

    /**
     * El signature.
     *
     * @return el resultado
     */
    String signature();

    /**
     * El nombre.
     *
     * @return el resultado
     */
    String name();
}
