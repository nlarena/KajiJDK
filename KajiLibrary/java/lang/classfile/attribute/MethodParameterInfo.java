package java.lang.classfile.attribute;

import java.lang.classfile.constantpool.Utf8Entry;
import java.lang.reflect.AccessFlag;
import java.util.Optional;
import java.util.Set;
import jdk.internal.classfile.impl.TypedAttributes;

// A row of `MethodParameters` (JVMS §4.7.24): the name and the flags of a formal parameter. The name
// is optional even though the row exists -- the format allows storing a parameter's flags without
// naming it, which is what happens with synthetic parameters.
public interface MethodParameterInfo {

    /** The parameter's name, if it is there. */
    Optional<Utf8Entry> name();

    /** The flags, as a mask. */
    int flagsMask();

    /** The flags, as a set. */
    default Set<AccessFlag> flags() {
        return AccessFlag.maskToAccessFlags(flagsMask(), AccessFlag.Location.METHOD_PARAMETER);
    }

    /** Whether this flag is set. */
    default boolean has(AccessFlag flag) {
        return (flagsMask() & flag.mask()) != 0;
    }

    /** The row with these values. */
    public static MethodParameterInfo of(Optional<Utf8Entry> name, int flags) {
        return TypedAttributes.methodParameterInfo(name, flags);
    }

    /** The row with these values. */
    public static MethodParameterInfo of(Optional<String> name, AccessFlag... flags) {
        return TypedAttributes.methodParameterInfoOfNames(name, TypedAttributes.mask(flags));
    }

    /** The row with these values. */
    public static MethodParameterInfo ofParameter(Optional<String> name, int flags) {
        return TypedAttributes.methodParameterInfoOfNames(name, flags);
    }
}
