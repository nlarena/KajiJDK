package com.sun.source.util;

import com.sun.source.doctree.*;

/**
 * A visitor that walks a whole comment's tree and combines what each node returns.
 *
 * <h2>What it contributes over implementing the visitor by hand</h2>
 *
 * <p>The walk. Each {@code visitXxx} here already knows which that node's children are and
 * visits them; whoever extends this class overrides only those that are of interest and calls
 * {@code super.visitXxx(node, p)} so that the rest go on going down. Without that, forgetting a
 * child in one of the 40 methods leaves a branch of the tree unwalked, and there is no error
 * that says so.
 *
 * <h2>How the results are combined</h2>
 *
 * <p>With {@link #reduce}, which by default returns the first that is not {@code null}. It
 * serves for "find the first that holds"; for accumulating -- counting, gathering into a list
 * -- it has to be overridden.
 *
 * @param <R> what each visit returns
 * @param <P> the datum that is carried along the walk
 */
public class DocTreeScanner<R, P> implements DocTreeVisitor<R, P> {

    public DocTreeScanner() {
    }

    /** It visits a node, or {@code null} if there is none. */
    public R scan(DocTree node, P p) {
        return node == null ? null : node.accept(this, p);
    }

    /** It visits every one of the list, combining what they return. */
    public R scan(Iterable<? extends DocTree> nodes, P p) {
        R r = null;
        if (nodes != null) {
            boolean first = true;
            for (DocTree node : nodes) {
                r = first ? scan(node, p) : reduce(scan(node, p), r);
                first = false;
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
     * It combines two results.
     *
     * <p>By default the one that is not {@code null} wins, with preference for the first. It is
     * the semantics of a "search": the walk goes on all the same, but what comes back is the
     * first find.
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
