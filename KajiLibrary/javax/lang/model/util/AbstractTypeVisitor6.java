package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVisitor;
import javax.lang.model.type.UnionType;
import javax.lang.model.type.UnknownTypeException;

/**
 * KajiLibrary's javax.lang.model.util.AbstractTypeVisitor6 — la base de la familia de visitantes de
 * tipos.
 *
 * <p>Mismo mecanismo que {@link AbstractElementVisitor6}, aplicado a {@link TypeVisitor}: una clase por
 * version del lenguaje, lo que existia en esa version abstracto, lo posterior con un cuerpo que cae en
 * {@link #visitUnknown} y tira. Ahi esta explicado por que, y no se repite.
 *
 * <p>Lo que cambia es **cuales** son las formas de tipo que fueron llegando, que no son las mismas que
 * las clases de declaracion:
 *
 * <ul>
 * <li>El tipo **union** es de Java 7, del `catch` multiple: en `catch (A | B e)`, el tipo de `e` no es ni
 *     `A` ni `B` sino la union de las dos.</li>
 * <li>El tipo **interseccion** es de Java 8, de las cotas multiples: en `&lt;T extends A &amp; B&gt;`, el
 *     tipo de `T` es la interseccion.</li>
 * </ul>
 *
 * <p>Los dos aparecen aca con cuerpo que delega en `visitUnknown`, y pasan a abstractos en la clase de
 * la version que los introdujo — 7 y 8 respectivamente.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public abstract class AbstractTypeVisitor6<R, P> implements TypeVisitor<R, P> {

    protected AbstractTypeVisitor6() {
    }

    /** El despacho: la que sabe que forma de tipo es, es la implementacion de `accept`. */
    public final R visit(TypeMirror t, P p) {
        return t.accept(this, p);
    }

    /** Igual, con parametro nulo. */
    public final R visit(TypeMirror t) {
        return t.accept(this, null);
    }

    public R visitUnion(UnionType t, P p) {
        return this.visitUnknown(t, p);
    }

    public R visitIntersection(IntersectionType t, P p) {
        return this.visitUnknown(t, p);
    }

    public R visitUnknown(TypeMirror t, P p) {
        throw new UnknownTypeException(t, p);
    }
}
