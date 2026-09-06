package jdk.jshell;

/**
 * Un fragmento que queda declarado para los que vengan despues.
 *
 * <p>Los imports y las declaraciones --tipos, metodos, variables-- siguen existiendo despues de
 * escribirlos; una expresion suelta se evalua y se termina. La diferencia esta en que estos tienen
 * nombre, y el nombre es lo que hace que un fragmento posterior pueda reemplazarlos: escribir de
 * nuevo un metodo con la misma firma deja al anterior en {@link Snippet.Status#OVERWRITTEN}.
 *
 * @since 9
 */
public abstract class PersistentSnippet extends Snippet {

    private final String name;

    PersistentSnippet(String id, String source, SubKind subkind, String name) {
        super(id, source, subkind);
        this.name = name;
    }

    /**
     * El nombre de lo que declara.
     *
     * @return el nombre
     */
    public String name() {
        return this.name;
    }
}
