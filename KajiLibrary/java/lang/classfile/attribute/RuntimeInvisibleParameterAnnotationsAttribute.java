package java.lang.classfile.attribute;

import java.lang.classfile.Annotation;
import java.lang.classfile.Attribute;
import java.lang.classfile.MethodElement;
import java.util.List;
import jdk.internal.classfile.impl.TypedAttributes;

// `RuntimeInvisibleParameterAnnotations` (JVMS §4.7.18/§4.7.19): las anotaciones no visibles de cada parámetro formal.
//
// La trampa clásica de este atributo: la cantidad de listas NO tiene por qué coincidir con la
// cantidad de parámetros del descriptor. Un constructor de clase interna o de enum lleva parámetros
// sintéticos que `javac` a veces cuenta y a veces no, así que emparejar la lista con el descriptor
// por índice desde el principio puede desalinear todo.
public interface RuntimeInvisibleParameterAnnotationsAttribute
        extends Attribute<RuntimeInvisibleParameterAnnotationsAttribute>, MethodElement {

    /** Una lista de anotaciones por parámetro, en orden. */
    List<List<Annotation>> parameterAnnotations();

    /** El atributo con estas listas. */
    public static RuntimeInvisibleParameterAnnotationsAttribute of(List<List<Annotation>> parameterAnnotations) {
        return TypedAttributes.runtimeInvisibleParameterAnnotations(parameterAnnotations);
    }
}
