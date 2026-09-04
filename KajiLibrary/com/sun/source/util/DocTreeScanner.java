package com.sun.source.util;

import com.sun.source.doctree.*;

/**
 * Un visitante que recorre el arbol de un comentario entero y combina lo que devuelve cada nodo.
 *
 * <h2>Que aporta sobre implementar el visitante a mano</h2>
 *
 * <p>El recorrido. Cada {@code visitXxx} de aca ya sabe cuales son los hijos de ese nodo y los
 * visita; quien extiende esta clase sobrescribe solo los que le interesan y llama a
 * {@code super.visitXxx(node, p)} para que el resto siga bajando. Sin eso, olvidarse un hijo en uno
 * de los 40 metodos deja una rama del arbol sin recorrer, y no hay error que lo diga.
 *
 * <h2>Como se combinan los resultados</h2>
 *
 * <p>Con {@link #reduce}, que por omision devuelve el primero que no sea {@code null}. Sirve para
 * "encontrar el primero que cumpla"; para acumular —contar, juntar en una lista— hay que
 * sobrescribirlo.
 *
 * @param <R> lo que devuelve cada visita
 * @param <P> el dato que se arrastra por el recorrido
 */
public class DocTreeScanner<R, P> implements DocTreeVisitor<R, P> {

    public DocTreeScanner() {
    }

    /** Visita un nodo, o {@code null} si no hay. */
    public R scan(DocTree node, P p) {
        return node == null ? null : node.accept(this, p);
    }

    /** Visita todos los de la lista, combinando lo que devuelvan. */
    public R scan(Iterable<? extends DocTree> nodes, P p) {
        R r = null;
        if (nodes != null) {
            boolean primero = true;
            for (DocTree node : nodes) {
                r = primero ? scan(node, p) : reduce(scan(node, p), r);
                primero = false;
            }
        }
        return r;
    }

    private R scanAndReduce(DocTree node, P p, R r) {
        return reduce(scan(node, p), r);
    }

    private R scanAndReduce(Iterable<? extends DocTree> nodes, P p, R r) {
        return reduce(scan(nodes, p), r);
    }

    /**
     * Combina dos resultados.
     *
     * <p>Por omision gana el que no sea {@code null}, con preferencia por el primero. Es la
     * semantica de "busqueda": el recorrido sigue igual, pero lo que vuelve es el primer hallazgo.
     */
    public R reduce(R r1, R r2) {
        return r1 != null ? r1 : r2;
    }


    public R visitAttribute(AttributeTree node, P p) {
        R r = scan(node.getValue(), p);
        return r;
    }

    public R visitAuthor(AuthorTree node, P p) {
        R r = scan(node.getName(), p);
        return r;
    }

    public R visitComment(CommentTree node, P p) {
        return null;
    }

    public R visitDeprecated(DeprecatedTree node, P p) {
        R r = scan(node.getBody(), p);
        return r;
    }

    public R visitDocComment(DocCommentTree node, P p) {
        R r = scan(node.getFirstSentence(), p);
        r = scanAndReduce(node.getFullBody(), p, r);
        r = scanAndReduce(node.getBody(), p, r);
        r = scanAndReduce(node.getBlockTags(), p, r);
        r = scanAndReduce(node.getPreamble(), p, r);
        r = scanAndReduce(node.getPostamble(), p, r);
        return r;
    }

    public R visitDocRoot(DocRootTree node, P p) {
        return null;
    }

    public R visitDocType(DocTypeTree node, P p) {
        return null;
    }

    public R visitEndElement(EndElementTree node, P p) {
        return null;
    }

    public R visitEntity(EntityTree node, P p) {
        return null;
    }

    public R visitErroneous(ErroneousTree node, P p) {
        return null;
    }

    public R visitEscape(EscapeTree node, P p) {
        return null;
    }

    public R visitHidden(HiddenTree node, P p) {
        R r = scan(node.getBody(), p);
        return r;
    }

    public R visitIdentifier(IdentifierTree node, P p) {
        return null;
    }

    public R visitIndex(IndexTree node, P p) {
        R r = scan(node.getSearchTerm(), p);
        r = scanAndReduce(node.getDescription(), p, r);
        return r;
    }

    public R visitInheritDoc(InheritDocTree node, P p) {
        R r = scan(node.getSupertype(), p);
        return r;
    }

    public R visitLink(LinkTree node, P p) {
        R r = scan(node.getReference(), p);
        r = scanAndReduce(node.getLabel(), p, r);
        return r;
    }

    public R visitLiteral(LiteralTree node, P p) {
        R r = scan(node.getBody(), p);
        return r;
    }

    public R visitParam(ParamTree node, P p) {
        R r = scan(node.getName(), p);
        r = scanAndReduce(node.getDescription(), p, r);
        return r;
    }

    public R visitProvides(ProvidesTree node, P p) {
        R r = scan(node.getServiceType(), p);
        r = scanAndReduce(node.getDescription(), p, r);
        return r;
    }

    public R visitRawText(RawTextTree node, P p) {
        return null;
    }

    public R visitReference(ReferenceTree node, P p) {
        return null;
    }

    public R visitReturn(ReturnTree node, P p) {
        R r = scan(node.getDescription(), p);
        return r;
    }

    public R visitSee(SeeTree node, P p) {
        R r = scan(node.getReference(), p);
        return r;
    }

    public R visitSerial(SerialTree node, P p) {
        R r = scan(node.getDescription(), p);
        return r;
    }

    public R visitSerialData(SerialDataTree node, P p) {
        R r = scan(node.getDescription(), p);
        return r;
    }

    public R visitSerialField(SerialFieldTree node, P p) {
        R r = scan(node.getName(), p);
        r = scanAndReduce(node.getType(), p, r);
        r = scanAndReduce(node.getDescription(), p, r);
        return r;
    }

    public R visitSince(SinceTree node, P p) {
        R r = scan(node.getBody(), p);
        return r;
    }

    public R visitSnippet(SnippetTree node, P p) {
        R r = scan(node.getAttributes(), p);
        r = scanAndReduce(node.getBody(), p, r);
        return r;
    }

    public R visitSpec(SpecTree node, P p) {
        R r = scan(node.getURL(), p);
        r = scanAndReduce(node.getTitle(), p, r);
        return r;
    }

    public R visitStartElement(StartElementTree node, P p) {
        R r = scan(node.getAttributes(), p);
        return r;
    }

    public R visitSummary(SummaryTree node, P p) {
        R r = scan(node.getSummary(), p);
        return r;
    }

    public R visitSystemProperty(SystemPropertyTree node, P p) {
        return null;
    }

    public R visitText(TextTree node, P p) {
        return null;
    }

    public R visitThrows(ThrowsTree node, P p) {
        R r = scan(node.getExceptionName(), p);
        r = scanAndReduce(node.getDescription(), p, r);
        return r;
    }

    public R visitUnknownBlockTag(UnknownBlockTagTree node, P p) {
        R r = scan(node.getContent(), p);
        return r;
    }

    public R visitUnknownInlineTag(UnknownInlineTagTree node, P p) {
        R r = scan(node.getContent(), p);
        return r;
    }

    public R visitUses(UsesTree node, P p) {
        R r = scan(node.getServiceType(), p);
        r = scanAndReduce(node.getDescription(), p, r);
        return r;
    }

    public R visitValue(ValueTree node, P p) {
        R r = scan(node.getReference(), p);
        r = scanAndReduce(node.getFormat(), p, r);
        return r;
    }

    public R visitVersion(VersionTree node, P p) {
        R r = scan(node.getBody(), p);
        return r;
    }

    public R visitOther(DocTree node, P p) {
        return null;
    }
}
