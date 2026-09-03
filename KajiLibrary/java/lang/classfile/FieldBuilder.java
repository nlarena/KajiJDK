package java.lang.classfile;

import java.lang.reflect.AccessFlag;
import java.lang.reflect.AccessFlag.Location;
import jdk.internal.classfile.impl.AccessFlagsImpl;

/**
 * Donde se escribe un campo.
 *
 * <p>Tiene poco propio: un campo es su nombre, su descriptor, sus banderas y sus atributos, y los
 * dos primeros los fija quien lo crea ({@link ClassBuilder#withField}). Lo que queda es esto.
 */
public interface FieldBuilder extends ClassFileBuilder<FieldElement, FieldBuilder> {

    /** Las banderas del campo, como máscara de bits. */
    default FieldBuilder withFlags(int flags) {
        return this.with(new AccessFlagsImpl(flags, Location.FIELD));
    }

    /**
     * Las banderas del campo.
     *
     * <p>Se arma la mascara aca en vez de guardar el conjunto: `AccessFlags` expone las dos vistas y
     * la mascara es la que va al archivo, asi que es la que conviene tener de primera mano.
     */
    default FieldBuilder withFlags(AccessFlag... flags) {
        int m = 0;
        for (int i = 0; i < flags.length; i++) {
            m = m | flags[i].mask();
        }
        return this.with(new AccessFlagsImpl(m, Location.FIELD));
    }
}
