package java.lang.classfile.attribute;

import java.lang.classfile.Attribute;
import java.lang.classfile.ClassElement;
import java.lang.classfile.constantpool.Utf8Entry;
import jdk.internal.classfile.impl.TypedAttributes;

// `SourceFile` (JVMS §4.7.10): el NOMBRE del archivo fuente, sin directorio. No es una ruta y no
// sirve para abrirlo; es lo que un depurador usa junto con el paquete de la clase para buscarlo.
public interface SourceFileAttribute extends Attribute<SourceFileAttribute>, ClassElement {

    /** El nombre del archivo fuente. */
    Utf8Entry sourceFile();

    /** El atributo con este nombre. */
    public static SourceFileAttribute of(String sourceFile) {
        return TypedAttributes.sourceFile(TypedAttributes.utf8(sourceFile));
    }

    /** El atributo con este nombre. */
    public static SourceFileAttribute of(Utf8Entry sourceFile) {
        return TypedAttributes.sourceFile(sourceFile);
    }
}
