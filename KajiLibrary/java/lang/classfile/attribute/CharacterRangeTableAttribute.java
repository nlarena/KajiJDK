package java.lang.classfile.attribute;

import java.lang.classfile.Attribute;
import java.util.List;
import jdk.internal.classfile.impl.TypedAttributes;

// `CharacterRangeTable`: la tabla de rangos de caracteres del fuente. No es del JVMS; la emite
// `javac -Xjcov` para las herramientas de cobertura.
public interface CharacterRangeTableAttribute extends Attribute<CharacterRangeTableAttribute> {

    /** Las filas, en el orden del archivo. */
    List<CharacterRangeInfo> characterRangeTable();

    /** El atributo con estas filas. */
    public static CharacterRangeTableAttribute of(List<CharacterRangeInfo> ranges) {
        return TypedAttributes.characterRangeTable(ranges);
    }
}
