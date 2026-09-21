package java.lang.classfile.attribute;

import java.lang.classfile.Annotation;
import java.lang.classfile.Attribute;
import java.lang.classfile.MethodElement;
import java.util.List;
import jdk.internal.classfile.impl.TypedAttributes;

// `RuntimeInvisibleParameterAnnotations` (JVMS §4.7.18/§4.7.19): the annotations of each formal
// parameter that are not visible.
//
// This attribute's classic trap: the number of lists need NOT match the number of parameters in the
// descriptor. An inner class's or an enum's constructor carries synthetic parameters `javac`
// sometimes counts and sometimes does not, so pairing the list with the descriptor by index from the
// start can throw everything out of line.
public interface RuntimeInvisibleParameterAnnotationsAttribute
        extends Attribute<RuntimeInvisibleParameterAnnotationsAttribute>, MethodElement {

    /** One list of annotations per parameter, in order. */
    List<List<Annotation>> parameterAnnotations();

    /** The attribute with these lists. */
    public static RuntimeInvisibleParameterAnnotationsAttribute of(List<List<Annotation>> parameterAnnotations) {
        return TypedAttributes.runtimeInvisibleParameterAnnotations(parameterAnnotations);
    }
}
