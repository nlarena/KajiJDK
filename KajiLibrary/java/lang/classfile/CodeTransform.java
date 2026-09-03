package java.lang.classfile;

import java.util.function.Consumer;
import java.util.function.Supplier;
import jdk.internal.classfile.impl.Transforms;

/**
 * Una transformación sobre las instrucciones de un método.
 *
 * <p>Es la que hace el trabajo interesante --instrumentar, reescribir llamadas, contar-- y la única
 * de las cuatro que **no** tiene `dropping`. No es un olvido del JDK: tirar una instrucción suelta
 * casi siempre deja el método inconsistente, porque las instrucciones dependen de lo que las de
 * antes dejaron en la pila. Filtrar código es reescribirlo, y para eso está {@link #accept}.
 */
public interface CodeTransform extends ClassFileTransform<CodeTransform, CodeElement, CodeBuilder> {

    /** La que deja pasar todo tal cual. */
    public static final CodeTransform ACCEPT_ALL = new AcceptAllCode();

    /** Ésta y después esa otra. */
    default CodeTransform andThen(CodeTransform next) {
        return Transforms.chainCode(this, next);
    }

    /** La que deja pasar todo y al final corre eso. */
    public static CodeTransform endHandler(Consumer<CodeBuilder> finisher) {
        return Transforms.endHandlerCode(finisher);
    }

    /** Una transformación con estado, fabricada de nuevo por cada uso. */
    public static CodeTransform ofStateful(Supplier<CodeTransform> supplier) {
        return Transforms.statefulCode(supplier);
    }
}

// La implementacion de `CodeTransform.ACCEPT_ALL`. Con nombre y no anonima: nuestro javac no emite una anonima
// en el inicializador de un campo de interfaz, y de paso el nombre aparece en los volcados de pila.
final class AcceptAllCode implements CodeTransform {

    public void accept(CodeBuilder builder, CodeElement element) {
        builder.with(element);
    }

    public String toString() {
        return "CodeTransform.ACCEPT_ALL";
    }
}
