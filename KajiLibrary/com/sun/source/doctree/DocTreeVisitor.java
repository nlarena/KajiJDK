package com.sun.source.doctree;

/**
 * Un visitante del arbol de un comentario de documentacion.
 *
 * <h2>Para que sirve, y por que no un {@code switch}</h2>
 *
 * <p>{@link DocTree#getKind} alcanza para preguntar por un nodo suelto. Este visitante es para
 * cuando hay que atender a <strong>todos</strong>, y lo que aporta es que el compilador cuente: si
 * el JDK agrega un tipo de nodo y falta el metodo, una implementacion de esta interfaz deja de
 * compilar. Un {@code switch} sobre el {@code Kind} se queda callado y devuelve mal en silencio.
 *
 * <h2>Cuales son {@code default} y por que</h2>
 *
 * <p>Los que se agregaron despues de que la interfaz ya existia. Volverlos abstractos habria roto a
 * todo el que la implementaba, asi que llegan con un cuerpo que delega en {@link #visitOther}: un
 * visitante viejo sigue compilando y trata al nodo nuevo como desconocido, que es exactamente lo
 * correcto — no lo entiende.
 *
 * <p>Es la razon de que {@link #visitOther} exista y de que sea el unico que recibe un
 * {@link DocTree} pelado en vez de un tipo preciso.
 *
 * @param <R> lo que devuelve cada visita
 * @param <P> el dato que se arrastra por el recorrido
 */
public interface DocTreeVisitor<R, P> {

    R visitAttribute(AttributeTree node, P p);

    R visitAuthor(AuthorTree node, P p);

    R visitComment(CommentTree node, P p);

    R visitDeprecated(DeprecatedTree node, P p);

    R visitDocComment(DocCommentTree node, P p);

    R visitDocRoot(DocRootTree node, P p);

    /** Agregado despues; por defecto delega en {@link #visitOther}. */
    default R visitDocType(DocTypeTree node, P p) {
        return visitOther(node, p);
    }

    R visitEndElement(EndElementTree node, P p);

    R visitEntity(EntityTree node, P p);

    R visitErroneous(ErroneousTree node, P p);

    /** Agregado despues; por defecto delega en {@link #visitOther}. */
    default R visitEscape(EscapeTree node, P p) {
        return visitOther(node, p);
    }

    /** Agregado despues; por defecto delega en {@link #visitOther}. */
    default R visitHidden(HiddenTree node, P p) {
        return visitOther(node, p);
    }

    R visitIdentifier(IdentifierTree node, P p);

    /** Agregado despues; por defecto delega en {@link #visitOther}. */
    default R visitIndex(IndexTree node, P p) {
        return visitOther(node, p);
    }

    R visitInheritDoc(InheritDocTree node, P p);

    R visitLink(LinkTree node, P p);

    R visitLiteral(LiteralTree node, P p);

    R visitParam(ParamTree node, P p);

    /** Agregado despues; por defecto delega en {@link #visitOther}. */
    default R visitProvides(ProvidesTree node, P p) {
        return visitOther(node, p);
    }

    /** Agregado despues; por defecto delega en {@link #visitOther}. */
    default R visitRawText(RawTextTree node, P p) {
        return visitOther(node, p);
    }

    R visitReference(ReferenceTree node, P p);

    R visitReturn(ReturnTree node, P p);

    R visitSee(SeeTree node, P p);

    R visitSerial(SerialTree node, P p);

    R visitSerialData(SerialDataTree node, P p);

    R visitSerialField(SerialFieldTree node, P p);

    R visitSince(SinceTree node, P p);

    /** Agregado despues; por defecto delega en {@link #visitOther}. */
    default R visitSnippet(SnippetTree node, P p) {
        return visitOther(node, p);
    }

    /** Agregado despues; por defecto delega en {@link #visitOther}. */
    default R visitSpec(SpecTree node, P p) {
        return visitOther(node, p);
    }

    R visitStartElement(StartElementTree node, P p);

    /** Agregado despues; por defecto delega en {@link #visitOther}. */
    default R visitSummary(SummaryTree node, P p) {
        return visitOther(node, p);
    }

    /** Agregado despues; por defecto delega en {@link #visitOther}. */
    default R visitSystemProperty(SystemPropertyTree node, P p) {
        return visitOther(node, p);
    }

    R visitText(TextTree node, P p);

    R visitThrows(ThrowsTree node, P p);

    R visitUnknownBlockTag(UnknownBlockTagTree node, P p);

    R visitUnknownInlineTag(UnknownInlineTagTree node, P p);

    /** Agregado despues; por defecto delega en {@link #visitOther}. */
    default R visitUses(UsesTree node, P p) {
        return visitOther(node, p);
    }

    R visitValue(ValueTree node, P p);

    R visitVersion(VersionTree node, P p);

    /**
     * Un nodo que este visitante no conoce: una implementacion propia, o un tipo que el JDK
     * agrego despues de que se escribiera esta implementacion.
     */
    R visitOther(DocTree node, P p);
}
