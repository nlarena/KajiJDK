package java.lang.classfile;

import java.lang.classfile.constantpool.ClassEntry;
import java.lang.classfile.constantpool.ConstantPool;
import java.util.List;
import java.util.Optional;

// A `.class` file already read. It is at once a structure with direct accessors --version, flags,
// name, fields, methods-- and a {@link CompoundElement} walked piece by piece, which is the shape a
// transformation uses.
public interface ClassModel extends CompoundElement<ClassElement>, AttributedElement {

    /** The class's constant pool. */
    ConstantPool constantPool();

    /** The class's `access_flags`. */
    AccessFlags flags();

    /** The `this_class` entry. */
    ClassEntry thisClass();

    /** The `major_version`. */
    int majorVersion();

    /** The `minor_version`. */
    int minorVersion();

    /** The fields, in file order. */
    List<FieldModel> fields();

    /** The methods, in file order. */
    List<MethodModel> methods();

    /** The superclass; empty on `java.lang.Object` and on a `module-info`. */
    Optional<ClassEntry> superclass();

    /** The direct interfaces, in file order. */
    List<ClassEntry> interfaces();

    /** Whether this is a `module-info.class`: `ACC_MODULE` set and the name `module-info`. */
    boolean isModuleInfo();
}
