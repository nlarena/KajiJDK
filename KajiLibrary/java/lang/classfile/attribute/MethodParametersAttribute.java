package java.lang.classfile.attribute;

import java.lang.classfile.Attribute;
import java.lang.classfile.MethodElement;
import java.util.List;
import jdk.internal.classfile.impl.TypedAttributes;

// `MethodParameters` (JVMS §4.7.24): los nombres de los parámetros formales, que `javac` sólo emite
// con `-parameters`. Es lo que hace que `Parameter.getName()` devuelva `cantidad` en vez de `arg0`.
public interface MethodParametersAttribute
        extends Attribute<MethodParametersAttribute>, MethodElement {

    /** Los parámetros, en el orden de la declaración. */
    List<MethodParameterInfo> parameters();

    /** El atributo con estos parámetros. */
    public static MethodParametersAttribute of(List<MethodParameterInfo> parameters) {
        return TypedAttributes.methodParameters(parameters);
    }

    /** El atributo con estos parámetros. */
    public static MethodParametersAttribute of(MethodParameterInfo... parameters) {
        return TypedAttributes.methodParameters(TypedAttributes.listOf(parameters));
    }
}
