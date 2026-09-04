package jdk.dynalink;

/**
 * No hay ningun miembro que satisfaga la operacion pedida en el sitio de invocacion.
 *
 * <p>Es la forma que toma un `NoSuchMethodError` cuando el metodo se busca en tiempo de
 * ejecucion: `unchecked`, porque el codigo que la provoca no declara que puede fallar asi.
 *
 * @since 9
 */
public class NoSuchDynamicMethodException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public NoSuchDynamicMethodException(final String message) {
        super(message);
    }
}
