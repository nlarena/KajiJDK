package java.lang.classfile;

import java.lang.classfile.constantpool.ClassEntry;
import java.lang.classfile.constantpool.ConstantPool;
import java.util.List;
import java.util.Optional;

// Un archivo `.class` ya leído. Es a la vez una estructura con accesores directos —versión,
// banderas, nombre, campos, métodos— y un {@link CompoundElement} que se recorre pieza por pieza,
// que es la forma que usa una transformación.
public interface ClassModel extends CompoundElement<ClassElement>, AttributedElement {

    /** El pool de constantes de la clase. */
    ConstantPool constantPool();

    /** El `access_flags` de la clase. */
    AccessFlags flags();

    /** La entrada `this_class`. */
    ClassEntry thisClass();

    /** El `major_version`. */
    int majorVersion();

    /** El `minor_version`. */
    int minorVersion();

    /** Los campos, en el orden del archivo. */
    List<FieldModel> fields();

    /** Los métodos, en el orden del archivo. */
    List<MethodModel> methods();

    /** La superclase; vacío en `java.lang.Object` y en un `module-info`. */
    Optional<ClassEntry> superclass();

    /** Las interfaces directas, en el orden del archivo. */
    List<ClassEntry> interfaces();

    /** Si esto es un `module-info.class`: `ACC_MODULE` puesto y nombre `module-info`. */
    boolean isModuleInfo();
}
