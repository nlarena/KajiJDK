package java.lang.classfile;

import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import jdk.internal.classfile.impl.Transforms;

/** Una transformación sobre los elementos de un método. Ver {@link ClassFileTransform}. */
public interface MethodTransform
        extends ClassFileTransform<MethodTransform, MethodElement, MethodBuilder> {

    /** La que deja pasar todo tal cual. */
    public static final MethodTransform ACCEPT_ALL = new AcceptAllMethod();

    /** Ésta y después esa otra. */
    default MethodTransform andThen(MethodTransform next) {
        return Transforms.chainMethod(this, next);
    }

    /** La que tira los elementos que cumplen el predicado. */
    public static MethodTransform dropping(Predicate<MethodElement> filter) {
        return Transforms.droppingMethod(filter);
    }

    /** La que deja pasar todo y al final corre eso. */
    public static MethodTransform endHandler(Consumer<MethodBuilder> finisher) {
        return Transforms.endHandlerMethod(finisher);
    }

    /** Una transformación con estado, fabricada de nuevo por cada uso. */
    public static MethodTransform ofStateful(Supplier<MethodTransform> supplier) {
        return Transforms.statefulMethod(supplier);
    }

    /**
     * La que transforma el `Code` del método y deja los demás elementos igual.
     *
     * <p>Distingue el cuerpo del resto porque son cosas distintas: las banderas, las excepciones
     * declaradas y las anotaciones del método pasan tal cual, y sólo el código entra a la
     * transformación de código.
     */
    public static MethodTransform transformingCode(CodeTransform xform) {
        return Transforms.transformingCode(xform);
    }
}

// La implementacion de `MethodTransform.ACCEPT_ALL`. Con nombre y no anonima: nuestro javac no emite una anonima
// en el inicializador de un campo de interfaz, y de paso el nombre aparece en los volcados de pila.
final class AcceptAllMethod implements MethodTransform {

    public void accept(MethodBuilder builder, MethodElement element) {
        builder.with(element);
    }

    public String toString() {
        return "MethodTransform.ACCEPT_ALL";
    }
}
