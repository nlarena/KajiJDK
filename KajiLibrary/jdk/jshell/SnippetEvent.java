package jdk.jshell;

/**
 * Lo que le paso a un fragmento.
 *
 * <h2>Por que evaluar produce varios</h2>
 *
 * <p>Porque un fragmento arrastra a los demas. Reescribir un metodo deja al anterior en
 * {@link Snippet.Status#OVERWRITTEN} y puede volver valido a un tercero que lo estaba esperando: son
 * tres sucesos por una sola evaluacion. {@link #causeSnippet} es lo que los distingue --el fragmento
 * que se evaluo tiene {@code null}, los arrastrados apuntan a el--.
 *
 * <h2>{@link #isSignatureChange}</h2>
 *
 * <p>Dice si lo que cambio fue la forma de lo declarado y no solo su cuerpo. Importa porque un
 * cambio de firma obliga a recompilar todo lo que dependia, y un cambio de cuerpo no.
 *
 * @since 9
 */
public class SnippetEvent {

    private final Snippet snippet;
    private final Snippet.Status previousStatus;
    private final Snippet.Status status;
    private final boolean isSignatureChange;
    private final Snippet causeSnippet;
    private final JShellException exception;
    private final String value;

    SnippetEvent(Snippet snippet, Snippet.Status previousStatus, Snippet.Status status,
            boolean isSignatureChange, Snippet causeSnippet, JShellException exception,
            String value) {
        this.snippet = snippet;
        this.previousStatus = previousStatus;
        this.status = status;
        this.isSignatureChange = isSignatureChange;
        this.causeSnippet = causeSnippet;
        this.exception = exception;
        this.value = value;
    }

    /**
     * De que fragmento se trata.
     *
     * @return el fragmento
     */
    public Snippet snippet() {
        return this.snippet;
    }

    /**
     * En que situacion estaba antes.
     *
     * @return la situacion anterior
     */
    public Snippet.Status previousStatus() {
        return this.previousStatus;
    }

    /**
     * En que situacion quedo.
     *
     * @return la situacion nueva
     */
    public Snippet.Status status() {
        return this.status;
    }

    /**
     * Si cambio la forma de lo declarado y no solo su cuerpo.
     *
     * @return cierto si cambio la firma
     */
    public boolean isSignatureChange() {
        return this.isSignatureChange;
    }

    /**
     * Que fragmento arrastro a este.
     *
     * @return el fragmento que se evaluo, o {@code null} si este es el que se evaluo
     */
    public Snippet causeSnippet() {
        return this.causeSnippet;
    }

    /**
     * Que excepcion tiro el codigo del usuario.
     *
     * @return la excepcion, o {@code null} si no tiro ninguna
     */
    public JShellException exception() {
        return this.exception;
    }

    /**
     * El valor que produjo, ya convertido a texto.
     *
     * <p>Viene como texto y no como objeto porque el valor vive en la otra maquina virtual, y su
     * clase puede no existir de este lado.
     *
     * @return el valor, o {@code null} si el fragmento no produce ninguno
     */
    public String value() {
        return this.value;
    }

    /**
     * Para leer al depurar.
     *
     * @return el fragmento, las dos situaciones y el valor
     */
    @Override
    public String toString() {
        return "SnippetEvent(" + this.snippet + " " + this.previousStatus + "=>" + this.status
                + " sig=" + this.isSignatureChange + " value=" + this.value + ")";
    }
}
