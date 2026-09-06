package jdk.jshell;

/**
 * La declaracion de un metodo.
 *
 * <h2>Por que la firma se guarda como texto</h2>
 *
 * <p>Porque un metodo puede quedar declarado antes de que existan los tipos que menciona: escribir
 * un metodo que devuelve una clase que todavia no se declaro es normal en una sesion interactiva. Un
 * objeto de reflexion no se podria construir; una cadena si.
 *
 * <p>{@link #parameterTypes} sale aparte porque es lo que decide si un metodo posterior reemplaza a
 * este o lo sobrecarga: mismo nombre y mismos tipos de parametro es reemplazar.
 *
 * @since 9
 */
public class MethodSnippet extends DeclarationSnippet {

    private final String parameterTypes;
    private final String signature;

    MethodSnippet(String id, String source, SubKind subkind, String name, String parameterTypes,
            String signature) {
        super(id, source, subkind, name);
        this.parameterTypes = parameterTypes;
        this.signature = signature;
    }

    /**
     * Los tipos de los parametros, separados por comas.
     *
     * @return los tipos, o la cadena vacia si el metodo no toma nada
     */
    public String parameterTypes() {
        return this.parameterTypes;
    }

    /**
     * La firma completa, con el tipo devuelto.
     *
     * @return la firma
     */
    public String signature() {
        return this.signature;
    }

    /**
     * Para leer al depurar.
     *
     * @return lo de {@link Snippet#toString} mas los tipos de los parametros
     */
    @Override
    public String toString() {
        return super.toString() + "-" + this.parameterTypes;
    }
}
