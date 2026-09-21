package com.sun.source.util;

import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;

/**
 * Which phase of the compilation started or finished, and over what.
 *
 * <p>The four constructors are the same idea with different detail available, and that detail
 * <strong>depends on the phase</strong>: on starting to parse only the file is known; after
 * parsing there is a compilation unit; in {@code ANALYZE} and {@code GENERATE} there is also a
 * type, and there may be several events per file -- one per class.
 *
 * <p>It is immutable, and that is why it is {@code final}: a listener may keep the event
 * without what it says changing afterwards.
 */
public final class TaskEvent {

    /**
     * The phases.
     *
     * <p>{@link #COMPILATION} wraps all the others: its {@code started} is the first event and its
     * {@code finished} the last. It serves in order to measure the total without adding the
     * parts up.
     */
    public enum Kind {

        /** Read and parse a file. */
        PARSE,
        /** Enter the symbols into the table. */
        ENTER,
        /** Analyse and type a class. */
        ANALYZE,
        /** Emit a class's {@code .class}. */
        GENERATE,
        /** All the annotation processing. */
        ANNOTATION_PROCESSING,
        /** One round of annotation processing. */
        ANNOTATION_PROCESSING_ROUND,
        /** The whole compilation; it wraps all the previous ones. */
        COMPILATION
    }

    private final Kind kind;
    private final JavaFileObject file;
    private final com.sun.source.tree.CompilationUnitTree unit;
    private final TypeElement clazz;

    /** Only the phase. */
    public TaskEvent(Kind kind) {
        this(kind, null, null, null);
    }

    /** The phase and the file. */
    public TaskEvent(Kind kind, JavaFileObject sourceFile) {
        this(kind, sourceFile, null, null);
    }

    /** The phase and the compilation unit; the file comes from it. */
    public TaskEvent(Kind kind, com.sun.source.tree.CompilationUnitTree unit) {
        this(kind, unit.getSourceFile(), unit, null);
    }

    /** The phase, the unit and the concrete class. */
    public TaskEvent(Kind kind, com.sun.source.tree.CompilationUnitTree unit, TypeElement clazz) {
        this(kind, unit.getSourceFile(), unit, clazz);
    }

    private TaskEvent(Kind kind, JavaFileObject file,
            com.sun.source.tree.CompilationUnitTree unit, TypeElement clazz) {
        this.kind = kind;
        this.file = file;
        this.unit = unit;
        this.clazz = clazz;
    }

    /** The phase. */
    public Kind getKind() {
        return this.kind;
    }

    /** The file, or {@code null}. */
    public JavaFileObject getSourceFile() {
        return this.file;
    }

    /** The compilation unit, or {@code null} if the phase is earlier than having it. */
    public com.sun.source.tree.CompilationUnitTree getCompilationUnit() {
        return this.unit;
    }

    /** The class, or {@code null} if the phase is not per class. */
    public TypeElement getTypeElement() {
        return this.clazz;
    }

    public String toString() {
        return "TaskEvent[" + String.valueOf(this.kind) + "," + String.valueOf(this.file)
                + "," + String.valueOf(this.clazz) + "]";
    }
}
