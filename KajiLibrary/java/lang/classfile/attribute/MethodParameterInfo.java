package java.lang.classfile.attribute;

import java.lang.classfile.constantpool.Utf8Entry;
import java.lang.reflect.AccessFlag;
import java.util.Optional;
import java.util.Set;
import jdk.internal.classfile.impl.TypedAttributes;

// Una fila de `MethodParameters` (JVMS §4.7.24): el nombre y las banderas de un parámetro formal. El
// nombre es opcional aunque la fila exista — el formato permite guardar las banderas de un parámetro
// sin nombrarlo, que es lo que pasa con los parámetros sintéticos.
public interface MethodParameterInfo {

    /** El nombre del parámetro, si está. */
    Optional<Utf8Entry> name();

    /** Las banderas, como máscara. */
    int flagsMask();

    /** Las banderas, como conjunto. */
    default Set<AccessFlag> flags() {
        return AccessFlag.maskToAccessFlags(flagsMask(), AccessFlag.Location.METHOD_PARAMETER);
    }

    /** Si esta bandera está puesta. */
    default boolean has(AccessFlag flag) {
        return (flagsMask() & flag.mask()) != 0;
    }

    /** La fila con estos valores. */
    public static MethodParameterInfo of(Optional<Utf8Entry> name, int flags) {
        return TypedAttributes.methodParameterInfo(name, flags);
    }

    /** La fila con estos valores. */
    public static MethodParameterInfo of(Optional<String> name, AccessFlag... flags) {
        return TypedAttributes.methodParameterInfoOfNames(name, TypedAttributes.mask(flags));
    }

    /** La fila con estos valores. */
    public static MethodParameterInfo ofParameter(Optional<String> name, int flags) {
        return TypedAttributes.methodParameterInfoOfNames(name, flags);
    }
}
