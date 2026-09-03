package java.lang.classfile.attribute;

import java.lang.classfile.constantpool.ClassEntry;
import java.lang.classfile.constantpool.Utf8Entry;
import java.lang.constant.ClassDesc;
import java.lang.reflect.AccessFlag;
import java.util.Optional;
import java.util.Set;
import jdk.internal.classfile.impl.TypedAttributes;

// Una fila de `InnerClasses` (JVMS §4.7.6). Los dos campos opcionales distinguen los tres casos que
// el formato mete en la misma tabla: con clase externa y con nombre, es una anidada normal; sin
// nombre, es anónima; sin clase externa, es local a un método.
//
// `flagsMask()` NO es el `access_flags` de la clase anidada: es el que tenía en el FUENTE. Una clase
// anidada privada se compila con `ACC_PRIVATE` acá y sin bandera de acceso en su propio archivo,
// porque en el `.class` no hay nada más externo que el paquete.
public interface InnerClassInfo {

    /** La clase anidada. */
    ClassEntry innerClass();

    /** La clase que la contiene; vacío si es local a un método. */
    Optional<ClassEntry> outerClass();

    /** El nombre simple; vacío si es anónima. */
    Optional<Utf8Entry> innerName();

    /** Las banderas del fuente, como máscara. */
    int flagsMask();

    /** Las banderas del fuente, como conjunto. */
    default Set<AccessFlag> flags() {
        return AccessFlag.maskToAccessFlags(flagsMask(), AccessFlag.Location.INNER_CLASS);
    }

    /** Si esta bandera está puesta. */
    default boolean has(AccessFlag flag) {
        return (flagsMask() & flag.mask()) != 0;
    }

    /** La fila con estos valores. */
    public static InnerClassInfo of(ClassEntry innerClass, Optional<ClassEntry> outerClass,
            Optional<Utf8Entry> innerName, int flags) {
        return TypedAttributes.innerClassInfo(innerClass, outerClass, innerName, flags);
    }

    /** La fila con estos valores. */
    public static InnerClassInfo of(ClassDesc innerClass, Optional<ClassDesc> outerClass,
            Optional<String> innerName, int flags) {
        return TypedAttributes.innerClassInfo(innerClass, outerClass, innerName, flags);
    }

    /** La fila con estos valores. */
    public static InnerClassInfo of(ClassDesc innerClass, Optional<ClassDesc> outerClass,
            Optional<String> innerName, AccessFlag... flags) {
        return TypedAttributes.innerClassInfo(innerClass, outerClass, innerName,
                TypedAttributes.mask(flags));
    }
}
