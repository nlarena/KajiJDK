package jdk.dynalink.linker;

import java.lang.invoke.MethodHandle;

/**
 * Envuelve un metodo con algo que se le agrega a todos por igual.
 *
 * <p>El uso previsto es el filtro de objetos internos: un lenguaje que representa sus valores con
 * clases propias no quiere que esas clases se escapen a quien lo hospeda, y en vez de acordarse
 * de convertir en cada punto de salida instala una transformacion que lo hace en todos.
 *
 * @since 9
 */
@FunctionalInterface
public interface MethodHandleTransformer {

    /**
     * El metodo transformado.
     *
     * @param target el metodo original
     * @return el transformado; nunca {@code null}
     */
    MethodHandle transform(MethodHandle target);
}
