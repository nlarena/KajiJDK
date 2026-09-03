package javax.security.auth;

/**
 * KajiLibrary's javax.security.auth.DestroyFailedException -- no se pudo borrar una credencial.
 *
 * <p>Vale la pena decir cuando es correcto lanzarla, porque el caso comun es al reves. Un objeto que
 * guarda su secreto en memoria propia lo borra y listo. Esta excepcion es para el que **no puede**:
 * una clave que vive adentro de un modulo de hardware, o una que el sistema copio a un lugar que la
 * biblioteca no controla. Ahi la unica respuesta honesta es avisar que el secreto sigue existiendo,
 * y no devolver en silencio como si se hubiera borrado.
 */
public class DestroyFailedException extends Exception {

    private static final long serialVersionUID = -7790152857890440085L;

    public DestroyFailedException() {
        super();
    }

    public DestroyFailedException(String msg) {
        super(msg);
    }
}
