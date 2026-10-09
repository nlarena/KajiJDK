package java.lang.classfile.attribute;

import java.lang.classfile.Attribute;
import java.util.List;
import jdk.internal.classfile.impl.TypedAttributes;

// `CharacterRangeTable`: the source's table of character ranges. It is not the JVMS's; `javac -Xjcov`
// emits it for coverage tools.
public interface CharacterRangeTableAttribute extends Attribute<CharacterRangeTableAttribute> {

    /** The rows, in file order. */
    List<CharacterRangeInfo> characterRangeTable();

    /** The attribute with these rows. */
    public static CharacterRangeTableAttribute of(List<CharacterRangeInfo> ranges) {
        return TypedAttributes.characterRangeTable(ranges);
    }
}
