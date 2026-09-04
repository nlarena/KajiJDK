package com.sun.source.util;

import com.sun.source.doctree.*;

/**
 * Un visitante que manda todo a un solo lugar.
 *
 * <h2>Para que sirve</h2>
 *
 * <p>Para atender <strong>unos pocos</strong> tipos de nodo sin escribir los 40 metodos.
 * Sobrescribiendo solo los que interesan, el resto cae en {@link #defaultAction}.
 *
 * <p>Es lo contrario de implementar {@link DocTreeVisitor} directamente, que obliga a escribirlos todos
 * — y esa obligacion tambien tiene su valor: es lo que hace que agregar sintaxis al lenguaje rompa
 * la compilacion de las herramientas en vez de que la ignoren en silencio. Esta clase renuncia a eso
 * a cambio de brevedad.
 *
 * <p><strong>No recorre.</strong> Para eso esta {@link DocTreeScanner}.
 *
 * @param <R> lo que devuelve cada visita
 * @param <P> el dato que se arrastra
 */
public class SimpleDocTreeVisitor<R, P> implements DocTreeVisitor<R, P> {

    /** Lo que devuelve {@link #defaultAction} si no se lo sobrescribe. */
    protected final R DEFAULT_VALUE;

    /** Con {@code null} como valor por omision. */
    protected SimpleDocTreeVisitor() {
        this.DEFAULT_VALUE = null;
    }

    /** Con ese valor por omision. */
    protected SimpleDocTreeVisitor(R defaultValue) {
        this.DEFAULT_VALUE = defaultValue;
    }

    /** Lo que se hace con un nodo que no se sobrescribio. */
    protected R defaultAction(DocTree node, P p) {
        return this.DEFAULT_VALUE;
    }

    /** Visita un nodo. {@code final}: el punto de extension es {@link #defaultAction}. */
    public final R visit(DocTree node, P p) {
        return node == null ? null : node.accept(this, p);
    }

    /** Visita todos, y devuelve lo del ultimo. */
    public final R visit(Iterable<? extends DocTree> nodes, P p) {
        R r = null;
        if (nodes != null) {
            for (DocTree node : nodes) {
                r = visit(node, p);
            }
        }
        return r;
    }


    public R visitAttribute(AttributeTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitAuthor(AuthorTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitComment(CommentTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitDeprecated(DeprecatedTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitDocComment(DocCommentTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitDocRoot(DocRootTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitDocType(DocTypeTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitEndElement(EndElementTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitEntity(EntityTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitErroneous(ErroneousTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitEscape(EscapeTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitHidden(HiddenTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitIdentifier(IdentifierTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitIndex(IndexTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitInheritDoc(InheritDocTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitLink(LinkTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitLiteral(LiteralTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitParam(ParamTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitProvides(ProvidesTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitRawText(RawTextTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitReference(ReferenceTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitReturn(ReturnTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitSee(SeeTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitSerial(SerialTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitSerialData(SerialDataTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitSerialField(SerialFieldTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitSince(SinceTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitSnippet(SnippetTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitSpec(SpecTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitStartElement(StartElementTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitSummary(SummaryTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitSystemProperty(SystemPropertyTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitText(TextTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitThrows(ThrowsTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitUnknownBlockTag(UnknownBlockTagTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitUnknownInlineTag(UnknownInlineTagTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitUses(UsesTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitValue(ValueTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitVersion(VersionTree node, P p) {
        return defaultAction(node, p);
    }

    public R visitOther(DocTree node, P p) {
        return defaultAction(node, p);
    }
}
