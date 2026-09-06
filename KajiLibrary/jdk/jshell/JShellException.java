package jdk.jshell;

/**
 * Algo que salio mal del otro lado: en el codigo que el usuario escribio, no en el interprete.
 *
 * <h2>Por que no se reusa la excepcion original</h2>
 *
 * <p>Porque el codigo del usuario corre en otra maquina virtual --de ahi
 * {@code jdk.jshell.execution}--, y una excepcion de alla no es un objeto que se pueda traer: su
 * clase puede no existir de este lado. Lo que viaja es la descripcion, y de este lado se arma un
 * {@link EvalException} con el nombre de la clase original adentro.
 *
 * @since 9
 */
public class JShellException extends Exception {

    private static final long serialVersionUID = 1L;

    JShellException() {
        super();
    }

    JShellException(String message) {
        super(message);
    }

    JShellException(String message, Throwable cause) {
        super(message, cause);
    }
}
