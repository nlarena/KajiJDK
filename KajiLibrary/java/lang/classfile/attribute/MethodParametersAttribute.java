package java.lang.classfile.attribute;

import java.lang.classfile.Attribute;
import java.lang.classfile.MethodElement;
import java.util.List;
import jdk.internal.classfile.impl.TypedAttributes;

// `MethodParameters` (JVMS §4.7.24): the names of the formal parameters, which `javac` only emits
// with `-parameters`. It is what makes `Parameter.getName()` return `count` instead of `arg0`.
public interface MethodParametersAttribute
        extends Attribute<MethodParametersAttribute>, MethodElement {

    /** The parameters, in declaration order. */
    List<MethodParameterInfo> parameters();

    /** The attribute with these parameters. */
    public static MethodParametersAttribute of(List<MethodParameterInfo> parameters) {
        return TypedAttributes.methodParameters(parameters);
    }

    /** The attribute with these parameters. */
    public static MethodParametersAttribute of(MethodParameterInfo... parameters) {
        return TypedAttributes.methodParameters(TypedAttributes.listOf(parameters));
    }
}
