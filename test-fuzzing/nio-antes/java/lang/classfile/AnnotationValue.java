package java.lang.classfile;

import java.lang.classfile.constantpool.AnnotationConstantValueEntry;
import java.lang.classfile.constantpool.DoubleEntry;
import java.lang.classfile.constantpool.FloatEntry;
import java.lang.classfile.constantpool.IntegerEntry;
import java.lang.classfile.constantpool.LongEntry;
import java.lang.classfile.constantpool.Utf8Entry;
import java.lang.constant.ClassDesc;
import java.util.List;
import jdk.internal.classfile.impl.Annotations;

// The value of an annotation element (JVMS §4.7.16.1, `element_value`). Each form has its one-byte
// tag --`I` an `int`, `s` a `String`, `e` an enum constant, `[` an array-- and this interface has one
// subtype per tag.
//
// MISSING, and for a concrete reason: `OfConstant.resolvedValue()` and its fourteen covariant
// overrides. The JDK declares them returning `java.lang.constant.Constable` and narrows them to
// `Integer`, `Double`, `String`, ... In KajiLibrary `java.lang.Integer` and company implement
// `ConstantDesc` but NOT `Constable`, so that narrowing does not compile and declaring it with
// another return type would be declaring another method. The value is obtained all the same, and
// without boxes, through `intValue()`, `stringValue()` and each subtype's other accessors, which are
// here.
public interface AnnotationValue {

    /** The `B` tag: a `byte`. */
    public static final int TAG_BYTE = 'B';
    /** The `C` tag: a `char`. */
    public static final int TAG_CHAR = 'C';
    /** The `D` tag: a `double`. */
    public static final int TAG_DOUBLE = 'D';
    /** The `F` tag: a `float`. */
    public static final int TAG_FLOAT = 'F';
    /** The `I` tag: an `int`. */
    public static final int TAG_INT = 'I';
    /** The `J` tag: a `long`. */
    public static final int TAG_LONG = 'J';
    /** The `S` tag: a `short`. */
    public static final int TAG_SHORT = 'S';
    /** The `Z` tag: a `boolean`. */
    public static final int TAG_BOOLEAN = 'Z';
    /** The `s` tag: a `String`. */
    public static final int TAG_STRING = 's';
    /** The `e` tag: an enum constant. */
    public static final int TAG_ENUM = 'e';
    /** The `c` tag: a class literal. */
    public static final int TAG_CLASS = 'c';
    /** The `@` tag: a nested annotation. */
    public static final int TAG_ANNOTATION = '@';
    /** The `[` tag: an array of values. */
    public static final int TAG_ARRAY = '[';

    /** This form's tag; one of the `TAG_*` constants. */
    int tag();

    /** A value that is a pool constant. */
    public interface OfConstant extends AnnotationValue {

        /** The pool entry carrying it. */
        AnnotationConstantValueEntry constant();
    }

    /** A `String`. */
    public interface OfString extends OfConstant {

        /** The `Utf8` with the text. */
        Utf8Entry constant();

        /** The text. */
        String stringValue();
    }

    /** A `double`. */
    public interface OfDouble extends OfConstant {

        /** The `CONSTANT_Double` entry. */
        DoubleEntry constant();

        /** The value. */
        double doubleValue();
    }

    /** A `float`. */
    public interface OfFloat extends OfConstant {

        /** The `CONSTANT_Float` entry. */
        FloatEntry constant();

        /** The value. */
        float floatValue();
    }

    /** A `long`. */
    public interface OfLong extends OfConstant {

        /** The `CONSTANT_Long` entry. */
        LongEntry constant();

        /** The value. */
        long longValue();
    }

    /** An `int`. */
    public interface OfInt extends OfConstant {

        /** The `CONSTANT_Integer` entry. */
        IntegerEntry constant();

        /** The value. */
        int intValue();
    }

    /** A `short`, which the format stores in a `CONSTANT_Integer`. */
    public interface OfShort extends OfConstant {

        /** The `CONSTANT_Integer` entry. */
        IntegerEntry constant();

        /** The value. */
        short shortValue();
    }

    /** A `char`, which the format stores in a `CONSTANT_Integer`. */
    public interface OfChar extends OfConstant {

        /** The `CONSTANT_Integer` entry. */
        IntegerEntry constant();

        /** The value. */
        char charValue();
    }

    /** A `byte`, which the format stores in a `CONSTANT_Integer`. */
    public interface OfByte extends OfConstant {

        /** The `CONSTANT_Integer` entry. */
        IntegerEntry constant();

        /** The value. */
        byte byteValue();
    }

    /** A `boolean`, which the format stores in a `CONSTANT_Integer` holding 0 or 1. */
    public interface OfBoolean extends OfConstant {

        /** The `CONSTANT_Integer` entry. */
        IntegerEntry constant();

        /** The value. */
        boolean booleanValue();
    }

    /** A class literal, that is, `Foo.class`. */
    public interface OfClass extends AnnotationValue {

        /** The `Utf8` with the class's descriptor. */
        Utf8Entry className();

        /** The class. */
        default ClassDesc classSymbol() {
            return ClassDesc.ofDescriptor(className().stringValue());
        }
    }

    /** An enum constant. */
    public interface OfEnum extends AnnotationValue {

        /** The `Utf8` with the enum's descriptor. */
        Utf8Entry className();

        /** The enum. */
        default ClassDesc classSymbol() {
            return ClassDesc.ofDescriptor(className().stringValue());
        }

        /** The `Utf8` with the constant's name. */
        Utf8Entry constantName();
    }

    /** A nested annotation. */
    public interface OfAnnotation extends AnnotationValue {

        /** The annotation. */
        Annotation annotation();
    }

    /** An array of values. */
    public interface OfArray extends AnnotationValue {

        /** The values, in order. */
        List<AnnotationValue> values();
    }

    /** The constant `constantName` of the enum whose descriptor `className` carries. */
    public static OfEnum ofEnum(Utf8Entry className, Utf8Entry constantName) {
        return Annotations.ofEnum(className, constantName);
    }

    /** The constant `constantName` of the enum `enumClass`. */
    public static OfEnum ofEnum(ClassDesc enumClass, String constantName) {
        return Annotations.ofEnum(Annotations.utf8(descriptorOf(enumClass, "enumClass")),
                Annotations.utf8(constantName));
    }

    /** The literal of the class whose descriptor `className` carries. */
    public static OfClass ofClass(Utf8Entry className) {
        return Annotations.ofClass(className);
    }

    /** `value`'s class literal. */
    public static OfClass ofClass(ClassDesc value) {
        return Annotations.ofClass(Annotations.utf8(descriptorOf(value, "value")));
    }

    /** The `String` `value` carries. */
    public static OfString ofString(Utf8Entry value) {
        return Annotations.ofString(value);
    }

    /** The `String` `value`. */
    public static OfString ofString(String value) {
        return Annotations.ofString(Annotations.utf8(value));
    }

    /** The `double` `value` carries. */
    public static OfDouble ofDouble(DoubleEntry value) {
        return Annotations.ofDouble(value);
    }

    /** The `double` `value`. */
    public static OfDouble ofDouble(double value) {
        return Annotations.ofDouble(Annotations.doubleEntry(value));
    }

    /** The `float` `value` carries. */
    public static OfFloat ofFloat(FloatEntry value) {
        return Annotations.ofFloat(value);
    }

    /** The `float` `value`. */
    public static OfFloat ofFloat(float value) {
        return Annotations.ofFloat(Annotations.floatEntry(value));
    }

    /** The `long` `value` carries. */
    public static OfLong ofLong(LongEntry value) {
        return Annotations.ofLong(value);
    }

    /** The `long` `value`. */
    public static OfLong ofLong(long value) {
        return Annotations.ofLong(Annotations.longEntry(value));
    }

    /** The `int` `value` carries. */
    public static OfInt ofInt(IntegerEntry value) {
        return Annotations.ofInt(value);
    }

    /** The `int` `value`. */
    public static OfInt ofInt(int value) {
        return Annotations.ofInt(Annotations.intEntry(value));
    }

    /** The `short` `value` carries. */
    public static OfShort ofShort(IntegerEntry value) {
        return Annotations.ofShort(value);
    }

    /** The `short` `value`. */
    public static OfShort ofShort(short value) {
        return Annotations.ofShort(Annotations.intEntry(value));
    }

    /** The `char` `value` carries. */
    public static OfChar ofChar(IntegerEntry value) {
        return Annotations.ofChar(value);
    }

    /** The `char` `value`. */
    public static OfChar ofChar(char value) {
        return Annotations.ofChar(Annotations.intEntry(value));
    }

    /** The `byte` `value` carries. */
    public static OfByte ofByte(IntegerEntry value) {
        return Annotations.ofByte(value);
    }

    /** The `byte` `value`. */
    public static OfByte ofByte(byte value) {
        return Annotations.ofByte(Annotations.intEntry(value));
    }

    /** The `boolean` `value` carries. */
    public static OfBoolean ofBoolean(IntegerEntry value) {
        return Annotations.ofBoolean(value);
    }

    /** The `boolean` `value`. */
    public static OfBoolean ofBoolean(boolean value) {
        return Annotations.ofBoolean(Annotations.intEntry(value ? 1 : 0));
    }

    /** The nested annotation `value`. */
    public static OfAnnotation ofAnnotation(Annotation value) {
        return Annotations.ofAnnotation(value);
    }

    /** The array with these values. */
    public static OfArray ofArray(List<AnnotationValue> array) {
        return Annotations.ofArrayOfList(array);
    }

    /** The array with these values. */
    public static OfArray ofArray(AnnotationValue... array) {
        return Annotations.ofArray(array);
    }

    /**
     * The value corresponding to `value`: a box, a `String`, a `ClassDesc`, an enum constant, or an
     * array of any of those. It throws `IllegalArgumentException` with anything else -- which is the
     * right answer: an `element_value` can only be one of those forms.
     */
    public static AnnotationValue of(Object value) {
        return Annotations.ofObject(value);
    }

    private static String descriptorOf(ClassDesc desc, String name) {
        if (desc == null) {
            throw new NullPointerException(name);
        }
        return desc.descriptorString();
    }
}
