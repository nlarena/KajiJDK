package jdk.dynalink.linker;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;

/**
 * Como adaptar un metodo a otra firma cuando {@code MethodHandle.asType} no alcanza.
 *
 * <h2>Cuando no alcanza</h2>
 *
 * <p>{@code asType} solo hace las conversiones de Java: ensanchar un primitivo, encajonar,
 * ampliar una referencia. Un lenguaje dinamico casi siempre tiene mas — convertir un numero a
 * cadena, una cadena a numero, cualquier objeto a booleano. Esas no las puede hacer el runtime
 * solo, y esta interfaz es donde el lenguaje las aporta.
 *
 * <p>Se aplica <strong>despues</strong> de las conversiones de Java, no en lugar de ellas: lo que
 * llega aca es lo que {@code asType} no supo resolver.
 *
 * @since 9
 */
@FunctionalInterface
public interface MethodTypeConversionStrategy {

    /**
     * El metodo adaptado a la firma pedida.
     *
     * @param target el metodo original
     * @param newType la firma pedida
     * @return el adaptado, o el original si no hay nada que hacer
     */
    MethodHandle asType(MethodHandle target, MethodType newType);
}
