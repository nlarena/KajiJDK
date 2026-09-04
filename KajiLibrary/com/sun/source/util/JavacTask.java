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
 * Una compilacion que se puede correr <strong>por fases</strong>.
 *
 * <h2>Que agrega sobre {@code CompilationTask}</h2>
 *
 * <p>Un {@link JavaCompiler.CompilationTask} tiene un solo verbo: {@code call}, que compila todo.
 * Esta clase parte eso en tres — {@link #parse}, {@link #analyze}, {@link #generate} — y ahi esta
 * todo su valor: una herramienta que solo quiere el arbol de sintaxis para analizarlo llama a
 * {@code parse} y para, sin pagar el tipado ni la emision de bytecode.
 *
 * <p>Son acumulativas: {@code analyze} parsea si hace falta, {@code generate} analiza. Llamarlas en
 * orden no repite trabajo.
 *
 * <h2>Y por que declina en esta VM</h2>
 *
 * <p>Igual que {@link Trees}: el compilador de este proyecto esta escrito en Rust y no expone una
 * implementacion de esto. {@link #instance} declina en vez de devolver algo que no compilaria nada.
 */
public abstract class JavacTask implements JavaCompiler.CompilationTask {

    /** Para las implementaciones. */
    protected JavacTask() {
    }

    /**
     * La tarea asociada a un entorno de procesamiento de anotaciones.
     *
     * @throws IllegalArgumentException si el entorno no es de {@code javac} — que es siempre, en
     *     esta VM
     */
    public static JavacTask instance(ProcessingEnvironment processingEnvironment) {
        throw new IllegalArgumentException(
                "el javac de este proyecto no expone una implementacion de JavacTask");
    }

    /** Parsea, y devuelve un arbol por archivo. */
    public abstract Iterable<? extends CompilationUnitTree> parse() throws IOException;

    /** Parsea si hace falta, analiza, y devuelve los elementos que quedaron. */
    public abstract Iterable<? extends Element> analyze() throws IOException;

    /** Analiza si hace falta, emite, y devuelve los archivos escritos. */
    public abstract Iterable<? extends JavaFileObject> generate() throws IOException;

    /**
     * Pone el unico oyente de fases, reemplazando al que hubiera.
     *
     * @throws IllegalStateException si ya se agregaron oyentes con {@link #addTaskListener} — los
     *     dos mecanismos no se mezclan, porque este borraria a los otros sin avisar
     */
    public abstract void setTaskListener(TaskListener taskListener);

    /** Agrega un oyente mas, sin sacar los que haya. */
    public abstract void addTaskListener(TaskListener taskListener);

    /** Saca un oyente. */
    public abstract void removeTaskListener(TaskListener taskListener);

    /** De donde salen los nombres de parametros que el {@code .class} no trae. */
    public void setParameterNameProvider(ParameterNameProvider provider) {
        throw new UnsupportedOperationException(
                "esta tarea no soporta un proveedor de nombres de parametros");
    }

    /**
     * El tipo de la expresion que hay al final de ese camino de nodos.
     *
     * <p>Recibe un {@code Iterable} y no un nodo suelto por lo mismo que {@link Trees}: un nodo no
     * dice donde esta, y su tipo depende de eso.
     */
    public abstract TypeMirror getTypeMirror(Iterable<? extends Tree> path);

    /** Las utilidades del modelo de elementos. */
    public abstract Elements getElements();

    /** Las utilidades del modelo de tipos. */
    public abstract Types getTypes();
}
