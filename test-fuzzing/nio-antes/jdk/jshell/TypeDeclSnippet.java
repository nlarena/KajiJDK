package jdk.jshell;

/**
 * The declaration of a class, interface, enum, record or annotation type.
 *
 * <p>Which of those five it is comes from {@link Snippet#subKind}. All five are treated alike in
 * everything else: they have a name, they stay declared, and they replace the previous one of the
 * same name.
 *
 * @since 9
 */
public class TypeDeclSnippet extends DeclarationSnippet {

    TypeDeclSnippet(String id, String source, SubKind subkind, String name) {
        super(id, source, subkind, name);
    }
}
