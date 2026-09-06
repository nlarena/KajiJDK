package jdk.jshell;

/**
 * El codigo del usuario tiro una excepcion.
 *
 * <h2>Por que el nombre de la clase va como cadena</h2>
 *
 * <p>Porque la excepcion nacio en otra maquina virtual y su clase puede no existir de este lado: el
 * usuario pudo haber declarado su propia excepcion en la sesion. Lo que viaja es el nombre, el
 * mensaje y la traza; lo que llega es esta excepcion, que es de este lado.
 *
 * <p>{@link #getCause} devuelve otro {@link EvalException} cuando la excepcion original tenia causa,
 * envuelta de la misma manera.
 *
 * @since 9
 */
public class EvalException extends JShellException {

    private static final long serialVersionUID = 1L;

    private final String exceptionClass;

    EvalException(String message, String exceptionClass, JShellException cause) {
        super(message, cause);
        this.exceptionClass = exceptionClass;
    }

    /**
     * El nombre de la clase de la excepcion original.
     *
     * @return el nombre completo de la clase
     */
    public String getExceptionClassName() {
        return this.exceptionClass;
    }

    /**
     * La causa, envuelta igual que esta.
     *
     * @return la causa, o {@code null}
     */
    @Override
    public JShellException getCause() {
        return (JShellException) super.getCause();
    }
}
