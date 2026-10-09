package com.sun.source.util;

import java.io.IOException;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;

/**
 * A compilation that may be run <strong>by phases</strong>.
 *
 * <h2>What it adds over {@code CompilationTask}</h2>
 *
 * <p>A {@link JavaCompiler.CompilationTask} has a single verb: {@code call}, which compiles
 * everything. This class splits that into three -- {@link #parse}, {@link #analyze},
 * {@link #generate} -- and there is all of its value: a tool that only wants the syntax tree in
 * order to analyse it calls {@code parse} and stops, without paying for the typing or the
 * bytecode emission.
 *
 * <p>They are cumulative: {@code analyze} parses if it is needed, {@code generate} analyses.
 * Calling them in order does not repeat work.
 *
 * <h2>And why it declines on this VM</h2>
 *
 * <p>The same as {@link Trees}: this project's compiler is written in Rust and does not expose
 * an implementation of this. {@link #instance} declines instead of returning something that
 * would compile nothing.
 */
public abstract class JavacTask implements JavaCompiler.CompilationTask {

    /** For the implementations. */
    protected JavacTask() {
    }

    /**
     * The task associated with an annotation processing environment.
     *
     * @throws IllegalArgumentException if the environment is not {@code javac}'s -- which is
     *     always, on this VM
     */
    public static JavacTask instance(ProcessingEnvironment processingEnvironment) {
        throw new IllegalArgumentException(
                "this project's javac does not expose an implementation of JavacTask");
    }

    /** It parses, and returns one tree per file. */
    public abstract Iterable<? extends CompilationUnitTree> parse() throws IOException;

    /** It parses if it is needed, analyses, and returns the elements that were left. */
    public abstract Iterable<? extends Element> analyze() throws IOException;

    /** It analyses if it is needed, emits, and returns the files that were written. */
    public abstract Iterable<? extends JavaFileObject> generate() throws IOException;

    /**
     * It sets the only phase listener, replacing whichever there was.
     *
     * @throws IllegalStateException if listeners were already added with {@link #addTaskListener}
     *     -- the two mechanisms do not mix, because this one would erase the others without saying
     *     so
     */
    public abstract void setTaskListener(TaskListener taskListener);

    /** It adds one more listener, without taking out those there may be. */
    public abstract void addTaskListener(TaskListener taskListener);

    /** It takes a listener out. */
    public abstract void removeTaskListener(TaskListener taskListener);

    /** Where the parameter names the {@code .class} does not bring come from. */
    public void setParameterNameProvider(ParameterNameProvider provider) {
        throw new UnsupportedOperationException(
                "this task does not support a parameter name provider");
    }

    /**
     * The type of the expression there is at the end of that path of nodes.
     *
     * <p>It receives an {@code Iterable} and not a loose node for the same reason as {@link Trees}:
     * a node does not say where it is, and its type depends on that.
     */
    public abstract TypeMirror getTypeMirror(Iterable<? extends Tree> path);

    /** The element model's utilities. */
    public abstract Elements getElements();

    /** The type model's utilities. */
    public abstract Types getTypes();
}
