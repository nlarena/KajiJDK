package jdk.jshell;

/**
 * La declaracion de una clase, interfaz, enumeracion, registro o tipo de anotacion.
 *
 * <p>Cual de esas cinco es lo dice {@link Snippet#subKind}. Las cinco se tratan igual en todo lo
 * demas: tienen nombre, quedan declaradas, y reemplazan a la anterior del mismo nombre.
 *
 * @since 9
 */
public class TypeDeclSnippet extends DeclarationSnippet {

    TypeDeclSnippet(String id, String source, SubKind subkind, String name) {
        super(id, source, subkind, name);
    }
}
