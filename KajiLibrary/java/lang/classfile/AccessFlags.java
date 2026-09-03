package java.lang.classfile;

import java.lang.reflect.AccessFlag;
import java.lang.reflect.AccessFlag.Location;
import java.util.Set;

// La máscara `access_flags` de una clase, un campo o un método (JVMS §4.1, §4.5, §4.6), con la
// ubicación que le da sentido a cada bit: el mismo `0x0020` es `ACC_SUPER` en una clase y
// `ACC_SYNCHRONIZED` en un método, así que sin la ubicación la máscara no se puede leer.
public interface AccessFlags extends ClassElement, MethodElement, FieldElement {

    /** La máscara cruda. */
    int flagsMask();

    /** Las banderas puestas, ya interpretadas para esta ubicación. */
    Set<AccessFlag> flags();

    /** Dónde vive esta máscara. */
    Location location();

    /** Si `flag` está puesta. Tira `IllegalArgumentException` si `flag` no vale acá. */
    boolean has(AccessFlag flag);
}
