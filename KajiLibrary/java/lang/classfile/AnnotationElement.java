package java.lang.classfile;

import java.lang.classfile.constantpool.Utf8Entry;
import java.lang.constant.ClassDesc;
import jdk.internal.classfile.impl.Annotations;

// Un par nombre-valor de una anotación (JVMS §4.7.16, `element_value_pairs`). El nombre es el del
// método del tipo de anotación; el valor, un {@link AnnotationValue} con la etiqueta que le
// corresponde.
//
// Las fábricas `ofXxx` de acá no verifican que el tipo del valor sea el que el método declara: el
// archivo tampoco lo hace, y quien lee un `.class` ajeno tiene que poder representar lo que dice
// aunque esté mal.
public interface AnnotationElement {

    /** El `Utf8` con el nombre del elemento. */
    Utf8Entry name();

    /** El valor. */
    AnnotationValue value();

    /** El par con este nombre y este valor. */
    public static AnnotationElement of(Utf8Entry name, AnnotationValue value) {
        return Annotations.element(name, value);
    }

    /** El par con este nombre y este valor. */
    public static AnnotationElement of(String name, AnnotationValue value) {
        return Annotations.element(Annotations.utf8(name), value);
    }

    /** El par cuyo valor es el literal de clase `value`. */
    public static AnnotationElement ofClass(String name, ClassDesc value) {
        return of(name, AnnotationValue.ofClass(value));
    }

    /** El par cuyo valor es el `String` `value`. */
    public static AnnotationElement ofString(String name, String value) {
        return of(name, AnnotationValue.ofString(value));
    }

    /** El par cuyo valor es el `long` `value`. */
    public static AnnotationElement ofLong(String name, long value) {
        return of(name, AnnotationValue.ofLong(value));
    }

    /** El par cuyo valor es el `int` `value`. */
    public static AnnotationElement ofInt(String name, int value) {
        return of(name, AnnotationValue.ofInt(value));
    }

    /** El par cuyo valor es el `char` `value`. */
    public static AnnotationElement ofChar(String name, char value) {
        return of(name, AnnotationValue.ofChar(value));
    }

    /** El par cuyo valor es el `short` `value`. */
    public static AnnotationElement ofShort(String name, short value) {
        return of(name, AnnotationValue.ofShort(value));
    }

    /** El par cuyo valor es el `byte` `value`. */
    public static AnnotationElement ofByte(String name, byte value) {
        return of(name, AnnotationValue.ofByte(value));
    }

    /** El par cuyo valor es el `boolean` `value`. */
    public static AnnotationElement ofBoolean(String name, boolean value) {
        return of(name, AnnotationValue.ofBoolean(value));
    }

    /** El par cuyo valor es el `double` `value`. */
    public static AnnotationElement ofDouble(String name, double value) {
        return of(name, AnnotationValue.ofDouble(value));
    }

    /** El par cuyo valor es el `float` `value`. */
    public static AnnotationElement ofFloat(String name, float value) {
        return of(name, AnnotationValue.ofFloat(value));
    }

    /** El par cuyo valor es la anotación anidada `value`. */
    public static AnnotationElement ofAnnotation(String name, Annotation value) {
        return of(name, AnnotationValue.ofAnnotation(value));
    }

    /** El par cuyo valor es el arreglo `values`. */
    public static AnnotationElement ofArray(String name, AnnotationValue... values) {
        return of(name, AnnotationValue.ofArray(values));
    }
}
