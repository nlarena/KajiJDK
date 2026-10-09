package java.lang.classfile;

import java.lang.classfile.constantpool.Utf8Entry;
import java.lang.constant.ClassDesc;
import jdk.internal.classfile.impl.Annotations;

// An annotation's name-value pair (JVMS §4.7.16, `element_value_pairs`). The name is that of the
// annotation type's method; the value, an {@link AnnotationValue} with the tag that goes with it.
//
// The `ofXxx` factories here do not check that the value's type is the one the method declares: the
// file does not do it either, and whoever reads someone else's `.class` has to be able to represent
// what it says even when it is wrong.
public interface AnnotationElement {

    /** The `Utf8` with the element's name. */
    Utf8Entry name();

    /** The value. */
    AnnotationValue value();

    /** The pair with this name and this value. */
    public static AnnotationElement of(Utf8Entry name, AnnotationValue value) {
        return Annotations.element(name, value);
    }

    /** The pair with this name and this value. */
    public static AnnotationElement of(String name, AnnotationValue value) {
        return Annotations.element(Annotations.utf8(name), value);
    }

    /** The pair whose value is the class literal `value`. */
    public static AnnotationElement ofClass(String name, ClassDesc value) {
        return of(name, AnnotationValue.ofClass(value));
    }

    /** The pair whose value is the `String` `value`. */
    public static AnnotationElement ofString(String name, String value) {
        return of(name, AnnotationValue.ofString(value));
    }

    /** The pair whose value is the `long` `value`. */
    public static AnnotationElement ofLong(String name, long value) {
        return of(name, AnnotationValue.ofLong(value));
    }

    /** The pair whose value is the `int` `value`. */
    public static AnnotationElement ofInt(String name, int value) {
        return of(name, AnnotationValue.ofInt(value));
    }

    /** The pair whose value is the `char` `value`. */
    public static AnnotationElement ofChar(String name, char value) {
        return of(name, AnnotationValue.ofChar(value));
    }

    /** The pair whose value is the `short` `value`. */
    public static AnnotationElement ofShort(String name, short value) {
        return of(name, AnnotationValue.ofShort(value));
    }

    /** The pair whose value is the `byte` `value`. */
    public static AnnotationElement ofByte(String name, byte value) {
        return of(name, AnnotationValue.ofByte(value));
    }

    /** The pair whose value is the `boolean` `value`. */
    public static AnnotationElement ofBoolean(String name, boolean value) {
        return of(name, AnnotationValue.ofBoolean(value));
    }

    /** The pair whose value is the `double` `value`. */
    public static AnnotationElement ofDouble(String name, double value) {
        return of(name, AnnotationValue.ofDouble(value));
    }

    /** The pair whose value is the `float` `value`. */
    public static AnnotationElement ofFloat(String name, float value) {
        return of(name, AnnotationValue.ofFloat(value));
    }

    /** The pair whose value is the nested annotation `value`. */
    public static AnnotationElement ofAnnotation(String name, Annotation value) {
        return of(name, AnnotationValue.ofAnnotation(value));
    }

    /** The pair whose value is the array `values`. */
    public static AnnotationElement ofArray(String name, AnnotationValue... values) {
        return of(name, AnnotationValue.ofArray(values));
    }
}
