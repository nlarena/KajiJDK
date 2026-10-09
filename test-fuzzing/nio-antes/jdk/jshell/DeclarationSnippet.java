package jdk.jshell;

/**
 * A snippet that declares something: a type, a method or a variable.
 *
 * <h2>What sets it apart from an import</h2>
 *
 * <p>Both are persistent, but a declaration can be left half done. A method calling another that
 * does not exist yet is accepted all the same --in an interactive session the order things are
 * written in need not be the order they are used in-- and is left with unresolved references, which
 * can be asked for with {@link JShell#unresolvedDependencies}. An import has no such in-between
 * state.
 *
 * @since 9
 */
public abstract class DeclarationSnippet extends PersistentSnippet {

    DeclarationSnippet(String id, String source, SubKind subkind, String name) {
        super(id, source, subkind, name);
    }
}
