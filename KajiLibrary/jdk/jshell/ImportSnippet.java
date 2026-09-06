package jdk.jshell;

/**
 * Un import.
 *
 * <h2>Los dos nombres</h2>
 *
 * <p>{@link #name} es el nombre simple --lo que se escribe despues para referirse al tipo-- y
 * {@link #fullname} es el nombre completo, con paquete. En un import con asterisco no hay nombre
 * simple que valga, asi que {@link #name} devuelve el ultimo segmento y el asterisco queda en
 * {@link #fullname}.
 *
 * @since 9
 */
public class ImportSnippet extends PersistentSnippet {

    private final String fullname;
    private final boolean isStatic;

    ImportSnippet(String id, String source, SubKind subkind, String name, String fullname,
            boolean isStatic) {
        super(id, source, subkind, name);
        this.fullname = fullname;
        this.isStatic = isStatic;
    }

    /**
     * El nombre completo de lo que se importa.
     *
     * @return el nombre con paquete
     */
    public String fullname() {
        return this.fullname;
    }

    /**
     * Si es un import estatico.
     *
     * @return cierto si lo es
     */
    public boolean isStatic() {
        return this.isStatic;
    }
}
