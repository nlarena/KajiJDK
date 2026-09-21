package javax.lang.model.util;

import java.util.List;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.NullType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.WildcardType;

/**
 * KajiLibrary's javax.lang.model.util.Types — the questions about types only the compiler can
 * answer.
 *
 * <p>An annotation processor sees the program as a model: {@code TypeMirror} describes a type, but
 * **knows nothing about its relation to others**. Asking whether `A` is a subtype of `B`, or what
 * the erasure of `List&lt;String&gt;` is, requires the compiler's type table. This interface is the
 * door to that table, and that is why none of its methods can be answered by looking at the
 * `TypeMirror` alone.
 *
 * <p>It is a **pure declaration**: the compiler implements it and hands it over through {@link
 * javax.annotation.processing.ProcessingEnvironment#getTypeUtils()}. It can be written whole and
 * honest without an implementation, and in fact **this library ships none** — what is needed for
 * that is `javac`'s type model, which lives in `src/javac/` and not in Java.
 *
 * <p>The three distinctions most often confused, and that are here because they are three different
 * questions:
 *
 * <ul>
 * <li>{@link #isSameType} is type identity. Careful: two wildcards are never the same type, not
 *     even compared with themselves, because each occurrence of `?` denotes a **different** unknown
 *     type.</li>
 * <li>{@link #isSubtype} is the language's subtyping relation (§4.10).</li>
 * <li>{@link #isAssignable} is whether an assignment compiles, which is wider: it includes
 *     assignment conversions such as boxing and numeric widening.</li>
 * </ul>
 */
public interface Types {

    /**
     * The element that declares that type, or `null` if the type declares none.
     *
     * <p>It returns `null` --and does not throw-- for a primitive type or an array, because "this
     * type has no declaration" is an answer and not an error.
     */
    Element asElement(TypeMirror t);

    /** Whether the two are the same type. */
    boolean isSameType(TypeMirror t1, TypeMirror t2);

    /** Whether `t1` is a subtype of `t2` (§4.10). */
    boolean isSubtype(TypeMirror t1, TypeMirror t2);

    /** Whether a value of `t1` can be assigned to a variable of `t2` (§5.2). */
    boolean isAssignable(TypeMirror t1, TypeMirror t2);

    /** Whether `t1` contains `t2` (§4.5.1), the relation between type arguments. */
    boolean contains(TypeMirror t1, TypeMirror t2);

    /**
     * Whether the signature of `m1` is a subsignature of that of `m2` (§8.4.2).
     *
     * <p>It is the question that decides whether a method **overrides** another, and it is not the
     * same as the two signatures being equal: a generic signature is a subsignature of its own
     * erasure.
     */
    boolean isSubsignature(ExecutableType m1, ExecutableType m2);

    /** The **direct** supertypes of that type, the superclass first if there is one. */
    List<? extends TypeMirror> directSupertypes(TypeMirror t);

    /** The erasure of that type (§4.6). */
    TypeMirror erasure(TypeMirror t);

    /** The wrapper class of that primitive. */
    TypeElement boxedClass(PrimitiveType p);

    /**
     * The primitive that wrapper wraps.
     *
     * @throws IllegalArgumentException if the type is not a wrapper
     */
    PrimitiveType unboxedType(TypeMirror t);

    /**
     * The capture of that type (§5.1.10).
     *
     * <p>Capturing is replacing each wildcard with a fresh type variable. It is what gives
     * `list.get(0)` a type one can work with when the list is `List&lt;?&gt;`.
     */
    TypeMirror capture(TypeMirror t);

    /**
     * That primitive type.
     *
     * @throws IllegalArgumentException if `kind` is not primitive
     */
    PrimitiveType getPrimitiveType(TypeKind kind);

    /** The null type, that of the literal `null`. */
    NullType getNullType();

    /**
     * A pseudo-type: `VOID`, `NONE` or `PACKAGE`.
     *
     * <p>They are "non-types" and that is why they have their own interface: `void` is not a type
     * with values, and `NONE` is what `Object`'s superclass returns --which is not `null` but the
     * explicit absence of a superclass--.
     *
     * @throws IllegalArgumentException if `kind` is not one of those three
     */
    NoType getNoType(TypeKind kind);

    /** An array of that component type. */
    ArrayType getArrayType(TypeMirror componentType);

    /**
     * A wildcard with those bounds.
     *
     * <p>The two parameters are mutually exclusive: `? extends X` has an upper bound, `? super X` a
     * lower one, and `?` has none. Passing both makes no sense in the language.
     *
     * @param extendsBound the upper bound, or `null`
     * @param superBound the lower one, or `null`
     */
    WildcardType getWildcardType(TypeMirror extendsBound, TypeMirror superBound);

    /** That type, parameterised with those arguments. */
    DeclaredType getDeclaredType(TypeElement typeElem, TypeMirror... typeArgs);

    /**
     * A parameterised nested type, inside a container that is parameterised too.
     *
     * <p>It exists separately because `Outer&lt;String&gt;.Inner&lt;Integer&gt;` has **two** sets
     * of arguments and the one of a single `TypeElement` cannot express that.
     */
    DeclaredType getDeclaredType(DeclaredType containing, TypeElement typeElem,
            TypeMirror... typeArgs);

    /**
     * The type of that element **seen as a member of** that type.
     *
     * <p>It is the substitution that makes generics useful: `List.get` declares it returns `E`, and
     * seen as a member of `List&lt;String&gt;` it returns `String`.
     */
    TypeMirror asMemberOf(DeclaredType containing, Element element);

    /**
     * The same type without its type annotations.
     *
     * <p>The default body returns the argument as it is, and it is as well to say why that is right
     * and not a shortcut: removing annotations from a type that has none is the identity. An
     * implementation that does model type annotations overrides it; while there are none in the
     * model, there is nothing to remove.
     */
    default <T extends TypeMirror> T stripAnnotations(T t) {
        return t;
    }
}
