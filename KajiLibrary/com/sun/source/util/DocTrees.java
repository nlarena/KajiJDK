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
 * {@link Trees} extendido con todo lo que hace falta para trabajar con la documentacion.
 *
 * <h2>Lo que agrega, y por que no estaba en {@link Trees}</h2>
 *
 * <p>Porque el arbol de documentacion es opcional: un compilador puede no parsear los comentarios
 * —le cuesta tiempo y no cambia el {@code .class}— y por eso el acceso a ellos vive en una subclase
 * que solo existe cuando alguien la pide.
 *
 * <h2>El {@link BreakIterator}, que es lo menos obvio</h2>
 *
 * <p>Javadoc muestra la <strong>primera oracion</strong> en sus tablas de resumen, y decidir donde
 * termina una oracion no es buscar un punto: {@code "Ver el Sr. Perez."} tiene dos puntos y una sola
 * oracion, y hay idiomas donde el criterio es completamente distinto. De ahi que sea configurable —
 * y que {@link #getFirstSentence} exista como metodo en vez de resolverse en el parser.
 *
 * <p>{@link #setBreakIterator} con {@code null} vuelve al criterio simple del propio javadoc, que es
 * el comportamiento historico.
 */
public abstract class DocTrees extends Trees {

    /** Para las implementaciones. */
    public DocTrees() {
    }

    /**
     * La instancia asociada a esa tarea de compilacion.
     *
     * @throws IllegalArgumentException si la tarea no es de un compilador que sepa proveerla — que
     *     es siempre, en esta VM; ver {@link Trees}
     */
    public static DocTrees instance(JavaCompiler.CompilationTask task) {
        throw new IllegalArgumentException(
                "el javac de este proyecto no expone la implementacion de DocTrees");
    }

    /**
     * La instancia asociada a un entorno de procesamiento.
     *
     * @throws IllegalArgumentException idem
     */
    public static DocTrees instance(ProcessingEnvironment env) {
        throw new IllegalArgumentException(
                "el javac de este proyecto no expone la implementacion de DocTrees");
    }

    /** Como se decide donde termina una oracion, o {@code null} para el criterio de javadoc. */
    public abstract BreakIterator getBreakIterator();

    /** Cambia ese criterio; ver la nota de la clase. */
    public abstract void setBreakIterator(BreakIterator breakiterator);

    /** Si el comentario es tradicional o Markdown. */
    public abstract Elements.DocCommentKind getDocCommentKind(TreePath path);

    /** El comentario de esa declaracion, ya parseado. */
    public abstract DocCommentTree getDocCommentTree(TreePath path);

    /** El comentario de ese elemento. */
    public abstract DocCommentTree getDocCommentTree(Element e);

    /**
     * El contenido de un archivo suelto, parseado como documentacion.
     *
     * <p>Es como se leen los {@code overview.html} y los {@code package.html}: documentacion que no
     * cuelga de ninguna declaracion.
     */
    public abstract DocCommentTree getDocCommentTree(FileObject fileObject);

    /** El comentario de un archivo relativo a ese elemento. */
    public abstract DocCommentTree getDocCommentTree(Element e, String relativePath)
            throws java.io.IOException;

    /** El camino de documentacion de un archivo de paquete. */
    public abstract DocTreePath getDocTreePath(FileObject fileObject, PackageElement packageElement);

    /**
     * El elemento al que apunta una referencia.
     *
     * <p>Es lo que convierte el texto de un {@code {@link Foo#bar}} en el metodo que nombra, y
     * necesita el camino entero porque la referencia se resuelve en el contexto de importaciones de
     * donde esta escrita.
     */
    public abstract Element getElement(DocTreePath path);

    /** El tipo al que apunta una referencia. */
    public abstract TypeMirror getType(DocTreePath path);

    /** La primera oracion de esa lista; ver la nota sobre el {@link BreakIterator}. */
    public abstract List<DocTree> getFirstSentence(List<? extends DocTree> list);

    /** Las posiciones, incluidas las de los nodos de documentacion. */
    public abstract DocSourcePositions getSourcePositions();

    /** Reporta un diagnostico ubicado en un nodo de documentacion. */
    public abstract void printMessage(Diagnostic.Kind kind, CharSequence msg, DocTree t,
            DocCommentTree c, CompilationUnitTree root);

    /** Con que construir nodos de documentacion nuevos. */
    public abstract DocTreeFactory getDocTreeFactory();

    /**
     * El texto que representa una entidad HTML, o {@code null} si no se reconoce.
     *
     * <p>Resuelve {@code &amp;} a {@code "&"}. El arbol la conserva sin resolver —ver
     * {@link EntityTree}— porque una herramienta que reemite HTML tiene que poder escribirla igual;
     * esto es para las que necesitan el texto.
     */
    public abstract String getCharacters(EntityTree tree);
}
