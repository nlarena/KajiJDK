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
 * El puente entre el arbol de sintaxis y el modelo de elementos.
 *
 * <h2>Que problema resuelve</h2>
 *
 * <p>Hay dos vistas del mismo programa y cada una sabe algo que la otra no. El
 * <strong>arbol</strong> ({@code com.sun.source.tree}) tiene la forma como se escribio: los
 * parentesis, el orden, donde esta cada cosa en el archivo. El <strong>modelo</strong>
 * ({@code javax.lang.model}) tiene lo resuelto: a que apunta cada nombre, cual es el tipo de cada
 * expresion, quien hereda de quien.
 *
 * <p>Un procesador de anotaciones recibe el modelo. Cuando ademas necesita el fuente —para reportar
 * un error en la linea exacta, o para mirar como se escribio algo— tiene que cruzar de una vista a
 * la otra, y esta clase es ese cruce. Sin ella, las dos APIs existirian sin forma de relacionarlas.
 *
 * <h2>Por que casi todo pide un {@link TreePath} y no un {@link Tree}</h2>
 *
 * <p>Porque un nodo suelto es ambiguo. El mismo {@code IdentifierTree} para {@code x} aparece en
 * muchos lugares y significa una variable distinta en cada uno; resolverlo necesita saber
 * <em>donde</em> esta, y eso es exactamente lo que un camino aporta y un nodo no.
 *
 * <h2>Y por que declina en esta VM</h2>
 *
 * <p>{@link #instance} pide una tarea de compilacion de {@code javac} y devuelve la implementacion
 * que el compilador trae adentro. El compilador de este proyecto esta escrito en Rust y no expone
 * esa implementacion, asi que las dos fabricas declinan en vez de devolver algo que no cruzaria
 * nada. La API queda entera para quien compile contra ella.
 */
public abstract class Trees {

    /** Para las implementaciones. */
    public Trees() {
    }

    /**
     * La instancia asociada a esa tarea de compilacion.
     *
     * @throws IllegalArgumentException si la tarea no es de un compilador que sepa proveerla — que
     *     es siempre, en esta VM
     */
    public static Trees instance(JavaCompiler.CompilationTask task) {
        throw new IllegalArgumentException(
                "el javac de este proyecto no expone la implementacion de Trees");
    }

    /**
     * La instancia asociada a un entorno de procesamiento de anotaciones.
     *
     * @throws IllegalArgumentException idem
     */
    public static Trees instance(ProcessingEnvironment env) {
        throw new IllegalArgumentException(
                "el javac de este proyecto no expone la implementacion de Trees");
    }

    /** Las posiciones en el fuente. */
    public abstract SourcePositions getSourcePositions();

    /** El nodo donde se declaro ese elemento, o {@code null} si no vino de un fuente. */
    public abstract Tree getTree(Element element);

    /** La declaracion de ese tipo. */
    public abstract ClassTree getTree(TypeElement element);

    /** La declaracion de ese metodo. */
    public abstract MethodTree getTree(ExecutableElement method);

    /** El nodo de esa anotacion sobre ese elemento. */
    public abstract Tree getTree(Element e, AnnotationMirror a);

    /** El nodo de ese valor dentro de esa anotacion. */
    public abstract Tree getTree(Element e, AnnotationMirror a, AnnotationValue v);

    /** El camino hasta ese nodo dentro de esa unidad. */
    public abstract TreePath getPath(CompilationUnitTree unit, Tree node);

    /** El camino hasta la declaracion de ese elemento. */
    public abstract TreePath getPath(Element e);

    /** El camino hasta esa anotacion. */
    public abstract TreePath getPath(Element e, AnnotationMirror a);

    /** El camino hasta ese valor de anotacion. */
    public abstract TreePath getPath(Element e, AnnotationMirror a, AnnotationValue v);

    /** El elemento al que resuelve ese camino, o {@code null}. */
    public abstract Element getElement(TreePath path);

    /** El tipo de lo que hay en ese camino, o {@code null}. */
    public abstract TypeMirror getTypeMirror(TreePath path);

    /** El alcance lexico en ese punto. */
    public abstract Scope getScope(TreePath path);

    /** El comentario de documentacion de esa declaracion, o {@code null}. */
    public abstract String getDocComment(TreePath path);

    /** Si ese tipo es accesible desde ese alcance. */
    public abstract boolean isAccessible(Scope scope, TypeElement type);

    /** Si ese miembro es accesible desde ese alcance. */
    public abstract boolean isAccessible(Scope scope, Element member, DeclaredType type);

    /**
     * El tipo que el compilador tenia antes de darse por vencido.
     *
     * <p>Cuando un tipo no resuelve, el modelo entrega un {@link ErrorType} para poder seguir
     * compilando. Esto recupera lo que se sabia de el, que es lo que permite dar un mensaje util en
     * vez de "tipo desconocido".
     */
    public abstract TypeMirror getOriginalType(ErrorType errorType);

    /**
     * Reporta un diagnostico ubicado en ese nodo.
     *
     * <p>Es lo que hace que un procesador pueda subrayar la linea exacta en vez de decir el nombre
     * del archivo y nada mas.
     */
    public abstract void printMessage(Diagnostic.Kind kind, CharSequence msg, Tree t,
            CompilationUnitTree root);

    /**
     * El supertipo comun mas ajustado de las excepciones de un {@code catch} multiple.
     *
     * <p>Hace falta porque el tipo de la variable de un {@code catch (A | B e)} no es ni {@code A}
     * ni {@code B} sino su cota superior, y esa no esta escrita en ningun lado del fuente.
     */
    public abstract TypeMirror getLub(CatchTree tree);
}
