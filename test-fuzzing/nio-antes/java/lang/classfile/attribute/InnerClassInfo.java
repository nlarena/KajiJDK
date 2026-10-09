package java.lang.classfile.attribute;

import java.lang.classfile.constantpool.ClassEntry;
import java.lang.classfile.constantpool.Utf8Entry;
import java.lang.constant.ClassDesc;
import java.lang.reflect.AccessFlag;
import java.util.Optional;
import java.util.Set;
import jdk.internal.classfile.impl.TypedAttributes;

// A row of `InnerClasses` (JVMS §4.7.6). The two optional fields tell apart the three cases the
// format puts into the same table: with an outer class and with a name, it is an ordinary nested
// class; with no name, it is anonymous; with no outer class, it is local to a method.
//
// `flagsMask()` is NOT the nested class's `access_flags`: it is the one it had in the SOURCE. A
// private nested class is compiled with `ACC_PRIVATE` here and with no access flag in its own file,
// because in a `.class` there is nothing more outward than the package.
public interface InnerClassInfo {

    /** The nested class. */
    ClassEntry innerClass();

    /** The class containing it; empty if it is local to a method. */
    Optional<ClassEntry> outerClass();

    /** The simple name; empty if it is anonymous. */
    Optional<Utf8Entry> innerName();

    /** The source's flags, as a mask. */
    int flagsMask();

    /** The source's flags, as a set. */
    default Set<AccessFlag> flags() {
        return AccessFlag.maskToAccessFlags(flagsMask(), AccessFlag.Location.INNER_CLASS);
    }

    /** Whether this flag is set. */
    default boolean has(AccessFlag flag) {
        return (flagsMask() & flag.mask()) != 0;
    }

    /** The row with these values. */
    public static InnerClassInfo of(ClassEntry innerClass, Optional<ClassEntry> outerClass,
            Optional<Utf8Entry> innerName, int flags) {
        return TypedAttributes.innerClassInfo(innerClass, outerClass, innerName, flags);
    }

    /** The row with these values. */
    public static InnerClassInfo of(ClassDesc innerClass, Optional<ClassDesc> outerClass,
            Optional<String> innerName, int flags) {
        return TypedAttributes.innerClassInfo(innerClass, outerClass, innerName, flags);
    }

    /** The row with these values. */
    public static InnerClassInfo of(ClassDesc innerClass, Optional<ClassDesc> outerClass,
            Optional<String> innerName, AccessFlag... flags) {
        return TypedAttributes.innerClassInfo(innerClass, outerClass, innerName,
                TypedAttributes.mask(flags));
    }
}
