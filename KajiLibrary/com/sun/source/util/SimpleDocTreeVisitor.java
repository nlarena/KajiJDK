package com.sun.source.util;

import com.sun.source.doctree.*;

/**
 * A visitor that sends everything to a single place.
 *
 * <h2>What it is for</h2>
 *
 * <p>For attending <strong>a few</strong> kinds of node without writing the 40 methods. By
 * overriding only those that are of interest, the rest fall into {@link #defaultAction}.
 *
 * <p>It is the opposite of implementing {@link DocTreeVisitor} directly, which forces them all to
 * be written -- and that obligation has its value too: it is what makes adding syntax to the
 * language break the compilation of the tools instead of their ignoring it silently. This class
 * gives that up in exchange for brevity.
 *
 * <p><strong>It does not walk.</strong> {@link DocTreeScanner} is there for that.
 *
 * @param <R> what each visit returns
 * @param <P> the datum that is carried along
 */
public class SimpleDocTreeVisitor<R, P> implements DocTreeVisitor<R, P> {

    /** What {@link #defaultAction} returns if it is not overridden. */
    protected final R DEFAULT_VALUE;

    /** With {@code null} as the default value. */
    protected SimpleDocTreeVisitor() {
        this.DEFAULT_VALUE = null;
    }

    /** With that default value. */
    protected SimpleDocTreeVisitor(R defaultValue) {
        this.DEFAULT_VALUE = defaultValue;
    }

    /** What is done with a node that was not overridden. */
    protected R defaultAction(DocTree node, P p) {
        return this.DEFAULT_VALUE;
    }

    /** It visits a node. {@code final}: the extension point is {@link #defaultAction}. */
    public final R visit(DocTree node, P p) {
        return node == null ? null : node.accept(this, p);
    }

    /** It visits them all, and returns the last one's. */
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
