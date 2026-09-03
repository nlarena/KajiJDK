package javax.security.auth.callback;

/**
 * KajiLibrary's javax.security.auth.callback.PasswordCallback -- pide una clave.
 *
 * <h2>Por que es char[] y no String</h2>
 *
 * <p>Un {@code String} es inmutable y vive hasta que el recolector lo levante: una clave guardada
 * ahi queda en memoria un tiempo que nadie controla, y aparece entera en un volcado. Un
 * {@code char[]} se puede <b>pisar</b> apenas se uso, y eso es lo que hace {@link #clearPassword()}.
 * Es la misma razon por la que {@code Console.readPassword()} tambien devuelve un arreglo.
 *
 * <p>El arreglo se copia al entrar y al salir. Sin la copia de entrada, quien llamo a
 * {@code setPassword} podria pisar el suyo y dejar este objeto con la clave rota; sin la de salida,
 * quien lee la clave podria pisarla y romper una segunda lectura.
 *
 * <h2>El detalle de clearPassword que se olvida</h2>
 *
 * <p><b>No pone el arreglo en null: lo llena de espacios.</b> Despues de llamarlo,
 * {@link #getPassword()} sigue devolviendo un arreglo del mismo largo, lleno de blancos. Es del JDK
 * y tiene sentido -- borrar es pisar los bytes, no soltar la referencia, que dejaria los bytes
 * donde estaban -- pero sorprende a quien espera un null.
 */
public class PasswordCallback implements Callback, java.io.Serializable {

    private static final long serialVersionUID = 2267422647454909926L;

    private final String prompt;
    private final boolean echoOn;
    private char[] inputPassword;

    /**
     * @param echoOn si lo que el usuario escribe se puede mostrar en pantalla. Casi siempre false;
     *     true es para los casos donde no hay secreto que proteger
     * @throws IllegalArgumentException si el prompt es null o vacio
     */
    public PasswordCallback(String prompt, boolean echoOn) {
        if (prompt == null || prompt.length() == 0) {
            throw new IllegalArgumentException();
        }
        this.prompt = prompt;
        this.echoOn = echoOn;
    }

    public String getPrompt() {
        return this.prompt;
    }

    public boolean isEchoOn() {
        return this.echoOn;
    }

    /** Guarda una <b>copia</b> de la clave. Ver la nota de la clase. */
    public void setPassword(char[] password) {
        this.inputPassword = password == null ? null : copy(password);
    }

    /** Una <b>copia</b> de la clave, o null si todavia nadie contesto. */
    public char[] getPassword() {
        return this.inputPassword == null ? null : copy(this.inputPassword);
    }

    /**
     * Pisa la clave con espacios.
     *
     * <p>No la pone en null; ver la nota de la clase. Llamarlo dos veces, o sin clave puesta, no
     * hace nada.
     */
    public void clearPassword() {
        if (this.inputPassword != null) {
            int i = 0;
            while (i < this.inputPassword.length) {
                this.inputPassword[i] = ' ';
                i = i + 1;
            }
        }
    }

    private static char[] copy(char[] a) {
        char[] c = new char[a.length];
        System.arraycopy(a, 0, c, 0, a.length);
        return c;
    }
}
