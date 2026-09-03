package java.lang.classfile;

import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import jdk.internal.classfile.impl.Transforms;

/** Una transformación sobre los elementos de un campo. Ver {@link ClassFileTransform}. */
public interface FieldTransform
        extends ClassFileTransform<FieldTransform, FieldElement, FieldBuilder> {

    /** La que deja pasar todo tal cual. */
    public static final FieldTransform ACCEPT_ALL = new AcceptAllField();

    /** Ésta y después esa otra. */
    default FieldTransform andThen(FieldTransform next) {
        return Transforms.chainField(this, next);
    }

    /** La que tira los elementos que cumplen el predicado. */
    public static FieldTransform dropping(Predicate<FieldElement> filter) {
        return Transforms.droppingField(filter);
    }

    /** La que deja pasar todo y al final corre eso. */
    public static FieldTransform endHandler(Consumer<FieldBuilder> finisher) {
        return Transforms.endHandlerField(finisher);
    }

    /** Una transformación con estado, fabricada de nuevo por cada uso. */
    public static FieldTransform ofStateful(Supplier<FieldTransform> supplier) {
        return Transforms.statefulField(supplier);
    }
}

// La implementacion de `FieldTransform.ACCEPT_ALL`. Con nombre y no anonima: nuestro javac no emite una anonima
// en el inicializador de un campo de interfaz, y de paso el nombre aparece en los volcados de pila.
final class AcceptAllField implements FieldTransform {

    public void accept(FieldBuilder builder, FieldElement element) {
        builder.with(element);
    }

    public String toString() {
        return "FieldTransform.ACCEPT_ALL";
    }
}
