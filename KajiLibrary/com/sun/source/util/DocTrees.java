package com.sun.source.util;

import java.text.BreakIterator;
import java.util.List;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.JavaCompiler;

import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.EntityTree;
import com.sun.source.tree.CompilationUnitTree;

/**
 * {@link Trees} extended with everything that is needed in order to work with the
 * documentation.
 *
 * <h2>What it adds, and why it was not in {@link Trees}</h2>
 *
 * <p>Because the documentation tree is optional: a compiler may not parse the comments -- it
 * costs it time and does not change the {@code .class} -- and that is why access to them lives
 * in a subclass that only exists when somebody asks for it.
 *
 * <h2>The {@link BreakIterator}, which is the least obvious thing</h2>
 *
 * <p>Javadoc shows the <strong>first sentence</strong> in its summary tables, and deciding
 * where a sentence ends is not looking for a full stop: {@code "See Mr. Perez."} has two full
 * stops and a single sentence, and there are languages where the criterion is completely
 * different. Hence it is configurable -- and hence {@link #getFirstSentence} exists as a method
 * instead of being resolved in the parser.
 *
 * <p>{@link #setBreakIterator} with {@code null} goes back to javadoc's own simple criterion,
 * which is the historical behaviour.
 */
public abstract class DocTrees extends Trees {

    /** For the implementations. */
    public DocTrees() {
    }

    /**
     * The instance associated with that compilation task.
     *
     * @throws IllegalArgumentException if the task is not from a compiler that knows how to provide
     *     it -- which is always, on this VM; see {@link Trees}
     */
    public static DocTrees instance(JavaCompiler.CompilationTask task) {
        throw new IllegalArgumentException(
                "this project's javac does not expose the implementation of DocTrees");
    }

    /**
     * The instance associated with a processing environment.
     *
     * @throws IllegalArgumentException the same
     */
    public static DocTrees instance(ProcessingEnvironment env) {
        throw new IllegalArgumentException(
                "this project's javac does not expose the implementation of DocTrees");
    }

    /** How where a sentence ends is decided, or {@code null} for javadoc's criterion. */
    public abstract BreakIterator getBreakIterator();

    /** It changes that criterion; see the class note. */
    public abstract void setBreakIterator(BreakIterator breakiterator);

    /** Whether the comment is traditional or Markdown. */
    public abstract Elements.DocCommentKind getDocCommentKind(TreePath path);

    /** That declaration's comment, already parsed. */
    public abstract DocCommentTree getDocCommentTree(TreePath path);

    /** That element's comment. */
    public abstract DocCommentTree getDocCommentTree(Element e);

    /**
     * The contents of a loose file, parsed as documentation.
     *
     * <p>It is how the {@code overview.html} and the {@code package.html} are read: documentation
     * that hangs off no declaration.
     */
    public abstract DocCommentTree getDocCommentTree(FileObject fileObject);

    /** The comment of a file relative to that element. */
    public abstract DocCommentTree getDocCommentTree(Element e, String relativePath)
            throws java.io.IOException;

    /** A package file's documentation path. */
    public abstract DocTreePath getDocTreePath(FileObject fileObject, PackageElement packageElement);

    /**
     * The element a reference points at.
     *
     * <p>It is what turns the text of a {@code {@link Foo#bar}} into the method it names, and it
     * needs the whole path because the reference is resolved in the import context of where it is
     * written.
     */
    public abstract Element getElement(DocTreePath path);

    /** The type a reference points at. */
    public abstract TypeMirror getType(DocTreePath path);

    /** That list's first sentence; see the note about the {@link BreakIterator}. */
    public abstract List<DocTree> getFirstSentence(List<? extends DocTree> list);

    /** The positions, including those of the documentation nodes. */
    public abstract DocSourcePositions getSourcePositions();

    /** It reports a diagnostic placed at a documentation node. */
    public abstract void printMessage(Diagnostic.Kind kind, CharSequence msg, DocTree t,
            DocCommentTree c, CompilationUnitTree root);

    /** What to build new documentation nodes with. */
    public abstract DocTreeFactory getDocTreeFactory();

    /**
     * The text an HTML entity represents, or {@code null} if it is not recognized.
     *
     * <p>It resolves {@code &amp;} to {@code "&"}. The tree keeps it unresolved -- see
     * {@link EntityTree} -- because a tool that re-emits HTML has to be able to write it the same;
     * this is for those that need the text.
     */
    public abstract String getCharacters(EntityTree tree);
}
