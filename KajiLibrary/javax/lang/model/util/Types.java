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
 * KajiLibrary's javax.lang.model.util.Types — las preguntas sobre tipos que sólo el compilador puede
 * contestar.
 *
 * <p>Un procesador de anotaciones ve el programa como un modelo: {@code TypeMirror} describe un tipo,
 * pero **no sabe nada sobre su relación con otros**. Preguntar si `A` es subtipo de `B`, o cuál es el
 * borrado de `List&lt;String&gt;`, exige la tabla de tipos del compilador. Esta interfaz es la puerta
 * a esa tabla, y por eso ninguno de sus métodos se puede contestar mirando el `TypeMirror` solo.
 *
 * <p>Es una **declaración pura**: la implementa el compilador y la entrega por
 * {@link javax.annotation.processing.ProcessingEnvironment#getTypeUtils()}. Se puede escribir entera y
 * honesta sin que haya implementación, y de hecho **esta biblioteca no trae ninguna** — lo que se
 * necesita para eso es el modelo de tipos de `javac`, que vive en `src/javac/` y no en Java.
 *
 * <p>Las tres distinciones que más se confunden, y que están acá porque son tres preguntas distintas:
 *
 * <ul>
 * <li>{@link #isSameType} es identidad de tipos. Ojo: dos comodines nunca son el mismo tipo, ni
 *     siquiera comparados consigo mismos, porque cada aparición de `?` denota un tipo desconocido
 *     **distinto**.</li>
 * <li>{@link #isSubtype} es la relación de subtipado del lenguaje (§4.10).</li>
 * <li>{@link #isAssignable} es si una asignación compila, que es más ancho: incluye conversiones de
 *     asignación como el boxing y el ensanchamiento numérico.</li>
 * </ul>
 */
public interface Types {

    /**
     * El elemento que declara ese tipo, o `null` si el tipo no declara ninguno.
     *
     * <p>Devuelve `null` --y no tira-- para un tipo primitivo o un array, porque "este tipo no tiene
     * declaración" es una respuesta y no un error.
     */
    Element asElement(TypeMirror t);

    /** Si los dos son el mismo tipo. */
    boolean isSameType(TypeMirror t1, TypeMirror t2);

    /** Si `t1` es subtipo de `t2` (§4.10). */
    boolean isSubtype(TypeMirror t1, TypeMirror t2);

    /** Si un valor de `t1` se puede asignar a una variable de `t2` (§5.2). */
    boolean isAssignable(TypeMirror t1, TypeMirror t2);

    /** Si `t1` contiene a `t2` (§4.5.1), la relación entre argumentos de tipo. */
    boolean contains(TypeMirror t1, TypeMirror t2);

    /**
     * Si la firma de `m1` es una subfirma de la de `m2` (§8.4.2).
     *
     * <p>Es la pregunta que decide si un método **redefine** a otro, y no es lo mismo que que las dos
     * firmas sean iguales: una firma genérica es subfirma de su propio borrado.
     */
    boolean isSubsignature(ExecutableType m1, ExecutableType m2);

    /** Los supertipos **directos** de ese tipo, la superclase primero si hay. */
    List<? extends TypeMirror> directSupertypes(TypeMirror t);

    /** El borrado de ese tipo (§4.6). */
    TypeMirror erasure(TypeMirror t);

    /** La clase envoltorio de ese primitivo. */
    TypeElement boxedClass(PrimitiveType p);

    /**
     * El primitivo que ese envoltorio envuelve.
     *
     * @throws IllegalArgumentException si el tipo no es un envoltorio
     */
    PrimitiveType unboxedType(TypeMirror t);

    /**
     * La captura de ese tipo (§5.1.10).
     *
     * <p>Capturar es reemplazar cada comodín por una variable de tipo fresca. Es lo que hace que
     * `lista.get(0)` tenga un tipo con el que se pueda trabajar cuando la lista es `List&lt;?&gt;`.
     */
    TypeMirror capture(TypeMirror t);

    /**
     * Ese tipo primitivo.
     *
     * @throws IllegalArgumentException si `kind` no es primitivo
     */
    PrimitiveType getPrimitiveType(TypeKind kind);

    /** El tipo nulo, el del literal `null`. */
    NullType getNullType();

    /**
     * Un pseudotipo: `VOID`, `NONE` o `PACKAGE`.
     *
     * <p>Son "no tipos" y por eso tienen su propia interfaz: `void` no es un tipo con valores, y
     * `NONE` es lo que devuelve la superclase de `Object` --que no es `null` sino la ausencia
     * explícita de superclase--.
     *
     * @throws IllegalArgumentException si `kind` no es uno de esos tres
     */
    NoType getNoType(TypeKind kind);

    /** Un array de ese tipo componente. */
    ArrayType getArrayType(TypeMirror componentType);

    /**
     * Un comodín con esas cotas.
     *
     * <p>Los dos parámetros son excluyentes: `? extends X` tiene cota superior, `? super X` inferior,
     * y `?` no tiene ninguna. Pasar las dos no tiene sentido en el lenguaje.
     *
     * @param extendsBound la cota superior, o `null`
     * @param superBound la inferior, o `null`
     */
    WildcardType getWildcardType(TypeMirror extendsBound, TypeMirror superBound);

    /** Ese tipo, parametrizado con esos argumentos. */
    DeclaredType getDeclaredType(TypeElement typeElem, TypeMirror... typeArgs);

    /**
     * Un tipo anidado parametrizado, dentro de un contenedor también parametrizado.
     *
     * <p>Existe aparte porque `Outer&lt;String&gt;.Inner&lt;Integer&gt;` tiene **dos** juegos de
     * argumentos y el de un solo `TypeElement` no puede expresarlo.
     */
    DeclaredType getDeclaredType(DeclaredType containing, TypeElement typeElem,
            TypeMirror... typeArgs);

    /**
     * El tipo de ese elemento **visto como miembro de** ese tipo.
     *
     * <p>Es la sustitución que hace útil a los genéricos: `List.get` declara devolver `E`, y visto
     * como miembro de `List&lt;String&gt;` devuelve `String`.
     */
    TypeMirror asMemberOf(DeclaredType containing, Element element);

    /**
     * El mismo tipo sin sus anotaciones de tipo.
     *
     * <p>El cuerpo por omisión devuelve el argumento tal cual, y conviene decir por qué eso es
     * correcto y no un atajo: quitar anotaciones de un tipo que no tiene ninguna es la identidad. Una
     * implementación que sí modele anotaciones de tipo lo redefine; mientras no haya ninguna en el
     * modelo, no hay nada que quitar.
     */
    default <T extends TypeMirror> T stripAnnotations(T t) {
        return t;
    }
}
