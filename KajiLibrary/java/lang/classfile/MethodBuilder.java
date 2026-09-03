package java.lang.classfile;

import java.lang.reflect.AccessFlag;
import java.lang.reflect.AccessFlag.Location;
import jdk.internal.classfile.impl.AccessFlagsImpl;
import java.util.function.Consumer;

/** Donde se escribe un método. */
public interface MethodBuilder extends ClassFileBuilder<MethodElement, MethodBuilder> {

    /** Las banderas del método, como máscara de bits. */
    default MethodBuilder withFlags(int flags) {
        return this.with(new AccessFlagsImpl(flags, Location.METHOD));
    }

    /** Las banderas del método. Ver la nota de `FieldBuilder.withFlags`. */
    default MethodBuilder withFlags(AccessFlag... flags) {
        int m = 0;
        for (int i = 0; i < flags.length; i++) {
            m = m | flags[i].mask();
        }
        return this.with(new AccessFlagsImpl(m, Location.METHOD));
    }

    /**
     * El cuerpo del método: el `Consumer` recibe un {@link CodeBuilder} y escribe las instrucciones.
     *
     * <p>Un método abstracto o nativo no lo llama; los demás sí, y exactamente una vez.
     */
    MethodBuilder withCode(Consumer<CodeBuilder> code);

    /** El cuerpo, copiado de ese modelo a través de esa transformación. */
    MethodBuilder transformCode(CodeModel code, CodeTransform transform);
}
