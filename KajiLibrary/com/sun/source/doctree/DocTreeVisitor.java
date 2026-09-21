package com.sun.source.doctree;

/**
 * A visitor of the tree of a documentation comment.
 *
 * <h2>What it is for, and why not a {@code switch}</h2>
 *
 * <p>{@link DocTree#getKind} is enough in order to ask about a loose node. This visitor is for
 * when <strong>all</strong> of them have to be attended to, and what it contributes is that the
 * compiler should count: if the JDK adds a kind of node and the method is missing, an
 * implementation of this interface stops compiling. A {@code switch} over the {@code Kind} keeps
 * quiet and returns wrongly in silence.
 *
 * <h2>Which are {@code default} and why</h2>
 *
 * <p>Those that were added after the interface already existed. Making them abstract would have
 * broken everybody who was implementing it, so they arrive with a body that delegates to
 * {@link #visitOther}: an old visitor goes on compiling and treats the new node as unknown, which
 * is exactly what is right -- it does not understand it.
 *
 * <p>It is the reason {@link #visitOther} exists and is the only one that receives a bare
 * {@link DocTree} instead of a precise type.
 *
 * @param <R> what each visit returns
 * @param <P> the datum that is carried along the walk
 */
public interface DocTreeVisitor<R, P> {

    R visitAttribute(AttributeTree node, P p);

    R visitAuthor(AuthorTree node, P p);

    R visitComment(CommentTree node, P p);

    R visitDeprecated(DeprecatedTree node, P p);

    R visitDocComment(DocCommentTree node, P p);

    R visitDocRoot(DocRootTree node, P p);

    /** Added later; by default it delegates to {@link #visitOther}. */
    default R visitDocType(DocTypeTree node, P p) {
        return visitOther(node, p);
    }

    R visitEndElement(EndElementTree node, P p);

    R visitEntity(EntityTree node, P p);

    R visitErroneous(ErroneousTree node, P p);

    /** Added later; by default it delegates to {@link #visitOther}. */
    default R visitEscape(EscapeTree node, P p) {
        return visitOther(node, p);
    }

    /** Added later; by default it delegates to {@link #visitOther}. */
    default R visitHidden(HiddenTree node, P p) {
        return visitOther(node, p);
    }

    R visitIdentifier(IdentifierTree node, P p);

    /** Added later; by default it delegates to {@link #visitOther}. */
    default R visitIndex(IndexTree node, P p) {
        return visitOther(node, p);
    }

    R visitInheritDoc(InheritDocTree node, P p);

    R visitLink(LinkTree node, P p);

    R visitLiteral(LiteralTree node, P p);

    R visitParam(ParamTree node, P p);

    /** Added later; by default it delegates to {@link #visitOther}. */
    default R visitProvides(ProvidesTree node, P p) {
        return visitOther(node, p);
    }

    /** Added later; by default it delegates to {@link #visitOther}. */
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

    /** Added later; by default it delegates to {@link #visitOther}. */
    default R visitSnippet(SnippetTree node, P p) {
        return visitOther(node, p);
    }

    /** Added later; by default it delegates to {@link #visitOther}. */
    default R visitSpec(SpecTree node, P p) {
        return visitOther(node, p);
    }

    R visitStartElement(StartElementTree node, P p);

    /** Added later; by default it delegates to {@link #visitOther}. */
    default R visitSummary(SummaryTree node, P p) {
        return visitOther(node, p);
    }

    /** Added later; by default it delegates to {@link #visitOther}. */
    default R visitSystemProperty(SystemPropertyTree node, P p) {
        return visitOther(node, p);
    }

    R visitText(TextTree node, P p);

    R visitThrows(ThrowsTree node, P p);

    R visitUnknownBlockTag(UnknownBlockTagTree node, P p);

    R visitUnknownInlineTag(UnknownInlineTagTree node, P p);

    /** Added later; by default it delegates to {@link #visitOther}. */
    default R visitUses(UsesTree node, P p) {
        return visitOther(node, p);
    }

    R visitValue(ValueTree node, P p);

    R visitVersion(VersionTree node, P p);

    /**
     * A node this visitor does not know: an implementation of one's own, or a type the JDK
     * added after this implementation was written.
     */
    R visitOther(DocTree node, P p);
}
