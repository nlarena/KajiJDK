package com.sun.source.util;

import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;

/**
 * Que fase de la compilacion empezo o termino, y sobre que.
 *
 * <p>Los cuatro constructores son la misma idea con distinto detalle disponible, y ese detalle
 * <strong>depende de la fase</strong>: al empezar a parsear solo se sabe el archivo; despues de
 * parsear hay unidad de compilacion; en {@code ANALYZE} y {@code GENERATE} hay ademas un tipo, y
 * puede haber varios eventos por archivo — uno por clase.
 *
 * <p>Es inmutable, y por eso es {@code final}: un oyente puede guardarse el evento sin que lo que
 * dice cambie despues.
 */
public final class TaskEvent {

    /**
     * Las fases.
     *
     * <p>{@link #COMPILATION} envuelve a todas las demas: su {@code started} es el primer evento y
     * su {@code finished} el ultimo. Sirve para medir el total sin sumar las partes.
     */
    public enum Kind {

        /** Leer y parsear un archivo. */
        PARSE,
        /** Entrar los simbolos en la tabla. */
        ENTER,
        /** Analizar y tipar una clase. */
        ANALYZE,
        /** Emitir el {@code .class} de una clase. */
        GENERATE,
        /** Todo el procesamiento de anotaciones. */
        ANNOTATION_PROCESSING,
        /** Una ronda de procesamiento de anotaciones. */
        ANNOTATION_PROCESSING_ROUND,
        /** La compilacion entera; envuelve a todas las anteriores. */
        COMPILATION
    }

    private final Kind kind;
    private final JavaFileObject file;
    private final com.sun.source.tree.CompilationUnitTree unit;
    private final TypeElement clazz;

    /** Solo la fase. */
    public TaskEvent(Kind kind) {
        this(kind, null, null, null);
    }

    /** La fase y el archivo. */
    public TaskEvent(Kind kind, JavaFileObject sourceFile) {
        this(kind, sourceFile, null, null);
    }

    /** La fase y la unidad de compilacion; el archivo sale de ella. */
    public TaskEvent(Kind kind, com.sun.source.tree.CompilationUnitTree unit) {
        this(kind, unit.getSourceFile(), unit, null);
    }

    /** La fase, la unidad y la clase concreta. */
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

    /** La fase. */
    public Kind getKind() {
        return this.kind;
    }

    /** El archivo, o {@code null}. */
    public JavaFileObject getSourceFile() {
        return this.file;
    }

    /** La unidad de compilacion, o {@code null} si la fase es anterior a tenerla. */
    public com.sun.source.tree.CompilationUnitTree getCompilationUnit() {
        return this.unit;
    }

    /** La clase, o {@code null} si la fase no es por clase. */
    public TypeElement getTypeElement() {
        return this.clazz;
    }

    public String toString() {
        return "TaskEvent[" + String.valueOf(this.kind) + "," + String.valueOf(this.file)
                + "," + String.valueOf(this.clazz) + "]";
    }
}
