package javax.security.sasl;

/**
 * KajiLibrary's javax.security.sasl.AuthenticationException -- las credenciales no sirven.
 *
 * <p>Separa "no te pude autenticar" de "algo salio mal en el camino", que es la unica distincion que
 * de verdad importa al fallar una negociacion: la primera no se arregla reintentando y la segunda
 * quizas si.
 *
 * <p>La nota que trae la especificacion vale repetirla: un servidor no deberia mandarle esta
 * distincion al cliente. Decirle a quien intenta entrar que la contrasena estaba mal --y no que el
 * usuario no existe-- le confirma que el usuario existe, que es medio trabajo hecho para quien esta
 * probando nombres. Esta clase es para el registro del lado del servidor, no para la respuesta.
 */
public class AuthenticationException extends SaslException {

    private static final long serialVersionUID = -3579708765071815007L;

    /** Sin detalle. */
    public AuthenticationException() {
        super();
    }

    /** Con un mensaje. */
    public AuthenticationException(String detail) {
        super(detail);
    }

    /** Con la causa de abajo. */
    public AuthenticationException(String detail, Throwable ex) {
        super(detail, ex);
    }
}
