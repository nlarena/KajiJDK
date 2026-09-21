package com.sun.source.util;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.JavaCompiler;

import com.sun.source.tree.CatchTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Scope;
import com.sun.source.tree.Tree;

/**
 * The bridge between the syntax tree and the element model.
 *
 * <h2>What problem it resolves</h2>
 *
 * <p>There are two views of the same program and each one knows something the other does not.
 * The <strong>tree</strong> ({@code com.sun.source.tree}) has the shape as it was written: the
 * parentheses, the order, where each thing is in the file. The <strong>model</strong>
 * ({@code javax.lang.model}) has what is resolved: what each name points at, what the type of
 * each expression is, who inherits from whom.
 *
 * <p>An annotation processor receives the model. When it also needs the source -- in order to
 * report an error on the exact line, or to look at how something was written -- it has to
 * cross from one view to the other, and this class is that crossing. Without it, the two APIs
 * would exist with no way of relating them.
 *
 * <h2>Why almost everything asks for a {@link TreePath} and not a {@link Tree}</h2>
 *
 * <p>Because a loose node is ambiguous. The same {@code IdentifierTree} for {@code x} appears
 * in many places and means a different variable in each; resolving it needs to know
 * <em>where</em> it is, and that is exactly what a path contributes and a node does not.
 *
 * <h2>And why it declines on this VM</h2>
 *
 * <p>{@link #instance} asks for a {@code javac} compilation task and returns the
 * implementation the compiler brings inside. This project's compiler is written in Rust and
 * does not expose that implementation, so the two factories decline instead of returning
 * something that would cross nothing. The API is left whole for whoever compiles against it.
 */
public abstract class Trees {

    /** For the implementations. */
    public Trees() {
    }

    /**
     * The instance associated with that compilation task.
     *
     * @throws IllegalArgumentException if the task is not from a compiler that knows how to
     *     provide it -- which is always, on this VM
     */
    public static Trees instance(JavaCompiler.CompilationTask task) {
        throw new IllegalArgumentException(
                "this project's javac does not expose the implementation of Trees");
    }

    /**
     * The instance associated with an annotation processing environment.
     *
     * @throws IllegalArgumentException the same
     */
    public static Trees instance(ProcessingEnvironment env) {
        throw new IllegalArgumentException(
                "this project's javac does not expose the implementation of Trees");
    }

    /** The positions in the source. */
    public abstract SourcePositions getSourcePositions();

    /**
     * The node where that element was declared, or {@code null} if it did not come from a source.
     */
    public abstract Tree getTree(Element element);

    /** That type's declaration. */
    public abstract ClassTree getTree(TypeElement element);

    /** That method's declaration. */
    public abstract MethodTree getTree(ExecutableElement method);

    /** The node of that annotation over that element. */
    public abstract Tree getTree(Element e, AnnotationMirror a);

    /** The node of that value inside that annotation. */
    public abstract Tree getTree(Element e, AnnotationMirror a, AnnotationValue v);

    /** The path as far as that node inside that unit. */
    public abstract TreePath getPath(CompilationUnitTree unit, Tree node);

    /** The path as far as that element's declaration. */
    public abstract TreePath getPath(Element e);

    /** The path as far as that annotation. */
    public abstract TreePath getPath(Element e, AnnotationMirror a);

    /** The path as far as that annotation value. */
    public abstract TreePath getPath(Element e, AnnotationMirror a, AnnotationValue v);

    /** The element that path resolves to, or {@code null}. */
    public abstract Element getElement(TreePath path);

    /** The type of what there is at that path, or {@code null}. */
    public abstract TypeMirror getTypeMirror(TreePath path);

    /** The lexical scope at that point. */
    public abstract Scope getScope(TreePath path);

    /** That declaration's documentation comment, or {@code null}. */
    public abstract String getDocComment(TreePath path);

    /** Whether that type is accessible from that scope. */
    public abstract boolean isAccessible(Scope scope, TypeElement type);

    /** Whether that member is accessible from that scope. */
    public abstract boolean isAccessible(Scope scope, Element member, DeclaredType type);

    /**
     * The type the compiler had before giving up.
     *
     * <p>When a type does not resolve, the model hands over an {@link ErrorType} so as to be able
     * to go on compiling. This recovers what was known about it, which is what allows a useful
     * message to be given instead of "unknown type".
     */
    public abstract TypeMirror getOriginalType(ErrorType errorType);

    /**
     * It reports a diagnostic placed at that node.
     *
     * <p>It is what allows a processor to underline the exact line instead of saying the file's
     * name and nothing more.
     */
    public abstract void printMessage(Diagnostic.Kind kind, CharSequence msg, Tree t,
            CompilationUnitTree root);

    /**
     * The tightest common supertype of the exceptions of a multi-{@code catch}.
     *
     * <p>It is needed because the type of the variable of a {@code catch (A | B e)} is neither
     * {@code A} nor {@code B} but their upper bound, and that one is not written anywhere in the
     * source.
     */
    public abstract TypeMirror getLub(CatchTree tree);
}
