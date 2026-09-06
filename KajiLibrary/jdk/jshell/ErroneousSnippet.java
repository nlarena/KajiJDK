package jdk.jshell;

/**
 * Un fragmento que no se pudo entender.
 *
 * <h2>Por que existe en vez de tirar</h2>
 *
 * <p>En un interprete, escribir algo mal es lo normal, y no puede costar el estado de la sesion. El
 * fragmento erroneo se guarda igual, con sus errores, y se puede listar y consultar como cualquier
 * otro. {@link #probableKind} es lo que el analizador cree que se estaba intentando escribir --sirve
 * para dar un mensaje util--, y puede ser {@link Snippet.Kind#ERRONEOUS} si no llego ni a eso.
 *
 * @since 9
 */
public class ErroneousSnippet extends Snippet {

    private final Kind probableKind;

    ErroneousSnippet(String id, String source, SubKind subkind, Kind probableKind) {
        super(id, source, subkind);
        this.probableKind = probableKind;
    }

    /**
     * Que se estaba intentando escribir, hasta donde se pudo saber.
     *
     * @return la clase probable
     */
    public Kind probableKind() {
        return this.probableKind;
    }
}
