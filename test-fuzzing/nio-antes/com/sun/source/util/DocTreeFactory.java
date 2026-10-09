package com.sun.source.util;

import java.util.List;

import javax.lang.model.element.Name;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

import com.sun.source.doctree.*;

/**
 * It builds documentation nodes.
 *
 * <h2>What building a documentation tree is for</h2>
 *
 * <p>For rewriting it. A tool that inherits documentation, that expands a template or that
 * corrects a tag needs to <strong>produce</strong> nodes, not only to read them -- and the
 * nodes are interfaces with no public constructor, on purpose: each compiler has its own
 * representation. This factory is the only portable way of creating them.
 *
 * <h2>The position, which is the only thing with state</h2>
 *
 * <p>{@link #at} fixes at which position of the source the nodes that are created afterwards
 * end up, and returns the same factory. It matters because a diagnostic over a built node
 * needs to point somewhere; without that, an error over generated documentation would have no
 * line.
 */
public interface DocTreeFactory {

    AttributeTree newAttributeTree(Name a0, AttributeTree.ValueKind a1, List<? extends DocTree> a2);

    AuthorTree newAuthorTree(List<? extends DocTree> a0);

    /**
     * The node of {@code {@code}}. It returns a {@link LiteralTree} just like
     * {@link #newLiteralTree}: the two tags share an interface and are told apart by their
     * {@code Kind}.
     */
    LiteralTree newCodeTree(TextTree a0);

    CommentTree newCommentTree(String a0);

    DeprecatedTree newDeprecatedTree(List<? extends DocTree> a0);

    DocCommentTree newDocCommentTree(List<? extends DocTree> a0, List<? extends DocTree> a1);

    DocCommentTree newDocCommentTree(List<? extends DocTree> a0, List<? extends DocTree> a1, List<? extends DocTree> a2, List<? extends DocTree> a3);

    DocRootTree newDocRootTree();

    DocTypeTree newDocTypeTree(String a0);

    EndElementTree newEndElementTree(Name a0);

    EntityTree newEntityTree(Name a0);

    ErroneousTree newErroneousTree(String a0, Diagnostic<JavaFileObject> a1);

    /** The node of a Markdown escape. */
    EscapeTree newEscapeTree(char a0);

    /** The node of {@code @exception}; same type as {@link #newThrowsTree}. */
    ThrowsTree newExceptionTree(ReferenceTree a0, List<? extends DocTree> a1);

    HiddenTree newHiddenTree(List<? extends DocTree> a0);

    IdentifierTree newIdentifierTree(Name a0);

    IndexTree newIndexTree(DocTree a0, List<? extends DocTree> a1);

    InheritDocTree newInheritDocTree();

    default InheritDocTree newInheritDocTree(ReferenceTree a0) {
        throw new UnsupportedOperationException(
                "this factory does not support newInheritDocTree");
    }

    LinkTree newLinkTree(ReferenceTree a0, List<? extends DocTree> a1);

    /** The node of {@code {@linkplain}}; same type as {@link #newLinkTree}. */
    LinkTree newLinkPlainTree(ReferenceTree a0, List<? extends DocTree> a1);

    LiteralTree newLiteralTree(TextTree a0);

    ParamTree newParamTree(boolean a0, IdentifierTree a1, List<? extends DocTree> a2);

    ProvidesTree newProvidesTree(ReferenceTree a0, List<? extends DocTree> a1);

    /** Text in a format this tree does not interpret, with the {@code Kind} that says which. */
    RawTextTree newRawTextTree(DocTree.Kind a0, String a1);

    ReferenceTree newReferenceTree(String a0);

    ReturnTree newReturnTree(List<? extends DocTree> a0);

    default ReturnTree newReturnTree(boolean a0, List<? extends DocTree> a1) {
        throw new UnsupportedOperationException(
                "this factory does not support newReturnTree");
    }

    SeeTree newSeeTree(List<? extends DocTree> a0);

    SerialTree newSerialTree(List<? extends DocTree> a0);

    SerialDataTree newSerialDataTree(List<? extends DocTree> a0);

    SerialFieldTree newSerialFieldTree(IdentifierTree a0, ReferenceTree a1, List<? extends DocTree> a2);

    SinceTree newSinceTree(List<? extends DocTree> a0);

    SnippetTree newSnippetTree(List<? extends DocTree> a0, TextTree a1);

    SpecTree newSpecTree(TextTree a0, List<? extends DocTree> a1);

    StartElementTree newStartElementTree(Name a0, List<? extends DocTree> a1, boolean a2);

    default SummaryTree newSummaryTree(List<? extends DocTree> a0) {
        throw new UnsupportedOperationException(
                "this factory does not support newSummaryTree");
    }

    SystemPropertyTree newSystemPropertyTree(Name a0);

    TextTree newTextTree(String a0);

    ThrowsTree newThrowsTree(ReferenceTree a0, List<? extends DocTree> a1);

    UnknownBlockTagTree newUnknownBlockTagTree(Name a0, List<? extends DocTree> a1);

    UnknownInlineTagTree newUnknownInlineTagTree(Name a0, List<? extends DocTree> a1);

    UsesTree newUsesTree(ReferenceTree a0, List<? extends DocTree> a1);

    ValueTree newValueTree(ReferenceTree a0);

    default ValueTree newValueTree(TextTree a0, ReferenceTree a1) {
        throw new UnsupportedOperationException(
                "this factory does not support newValueTree");
    }

    VersionTree newVersionTree(List<? extends DocTree> a0);

    /**
     * It fixes the source position of the nodes that are created afterwards.
     *
     * <p>It returns the same factory, not a new one: it is a knob with state, and chaining
     * {@code at(p).newTextTree(s)} is the intended way of using it.
     */
    com.sun.source.util.DocTreeFactory at(int a0);

    /** That list's first sentence, with the same criterion javadoc uses. */
    List<DocTree> getFirstSentence(List<? extends DocTree> a0);
}
