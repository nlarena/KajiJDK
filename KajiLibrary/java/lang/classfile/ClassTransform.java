package java.lang.classfile;

import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import jdk.internal.classfile.impl.Transforms;

/**
 * Una transformación sobre los elementos de una clase.
 *
 * <p>Ver {@link ClassFileTransform} para la forma general. Lo propio de ésta son las fábricas: casi
 * todo lo que uno quiere hacerle a una clase es una de ellas o dos encadenadas.
 */
public interface ClassTransform
        extends ClassFileTransform<ClassTransform, ClassElement, ClassBuilder> {

    /** La que deja pasar todo tal cual. Útil como punto de partida de una cadena. */
    public static final ClassTransform ACCEPT_ALL = new AcceptAllClass();

    /** Ésta y después esa otra. Ver {@link ClassFileTransform#andThen}. */
    default ClassTransform andThen(ClassTransform next) {
        return Transforms.chainClass(this, next);
    }

    /** La que tira los elementos que cumplen el predicado y deja pasar el resto. */
    public static ClassTransform dropping(Predicate<ClassElement> filter) {
        return Transforms.droppingClass(filter);
    }

    /**
     * La que deja pasar todo y al final corre eso.
     *
     * <p>Es como se agrega algo a una clase: el `Consumer` recibe el constructor cuando ya se
     * copiaron todos los elementos originales, y lo que escriba ahí queda al final.
     */
    public static ClassTransform endHandler(Consumer<ClassBuilder> finisher) {
        return Transforms.endHandlerClass(finisher);
    }

    /**
     * Una transformación **con estado**, fabricada de nuevo por cada uso.
     *
     * <p>El `Supplier` es lo que la vuelve segura: un transformador que cuenta o que recuerda lo que
     * vio no se puede compartir entre dos transformaciones, porque el estado de una contaminaría a
     * la otra. Con la fábrica, cada aplicación arranca con el suyo.
     */
    public static ClassTransform ofStateful(Supplier<ClassTransform> supplier) {
        return Transforms.statefulClass(supplier);
    }

    /** La que transforma cada campo con esa transformación y deja el resto igual. */
    public static ClassTransform transformingFields(FieldTransform xform) {
        return Transforms.transformingFields(xform);
    }

    /** La que transforma cada método. */
    public static ClassTransform transformingMethods(MethodTransform xform) {
        return Transforms.transformingMethods(Transforms.allMethods(), xform);
    }

    /** La que transforma sólo los métodos que cumplen el predicado. */
    public static ClassTransform transformingMethods(Predicate<MethodModel> filter,
            MethodTransform xform) {
        return Transforms.transformingMethods(filter, xform);
    }

    /**
     * La que transforma el **cuerpo** de cada método.
     *
     * <p>Es el atajo más usado de todos, y por eso está: `transformingMethods` con
     * `MethodTransform.transformingCode` escrito a mano es lo mismo y se lee peor.
     */
    public static ClassTransform transformingMethodBodies(CodeTransform xform) {
        return ClassTransform.transformingMethods(MethodTransform.transformingCode(xform));
    }

    /** Lo mismo, sólo para los métodos que cumplen el predicado. */
    public static ClassTransform transformingMethodBodies(Predicate<MethodModel> filter,
            CodeTransform xform) {
        return ClassTransform.transformingMethods(filter,
                MethodTransform.transformingCode(xform));
    }
}

// La implementacion de `ClassTransform.ACCEPT_ALL`. Con nombre y no anonima: nuestro javac no emite una anonima
// en el inicializador de un campo de interfaz, y de paso el nombre aparece en los volcados de pila.
final class AcceptAllClass implements ClassTransform {

    public void accept(ClassBuilder builder, ClassElement element) {
        builder.with(element);
    }

    public String toString() {
        return "ClassTransform.ACCEPT_ALL";
    }
}
