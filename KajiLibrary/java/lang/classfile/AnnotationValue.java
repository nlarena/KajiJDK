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

// El valor de un elemento de anotación (JVMS §4.7.16.1, `element_value`). Cada forma tiene su
// etiqueta de un byte —`I` un `int`, `s` un `String`, `e` una constante de enum, `[` un arreglo— y
// esta interfaz tiene un subtipo por etiqueta.
//
// FALTA, y con razón concreta: `OfConstant.resolvedValue()` y sus catorce redefiniciones
// covariantes. El JDK las declara devolviendo `java.lang.constant.Constable` y las estrecha a
// `Integer`, `Double`, `String`, … En KajiLibrary `java.lang.Integer` y compañía implementan
// `ConstantDesc` pero NO `Constable`, así que ese estrechamiento no compila y declararlo con otro
// tipo de retorno sería declarar otro método. El valor se saca igual, y sin cajas, por
// `intValue()`, `stringValue()` y los demás accesores de cada subtipo, que sí están.
public interface AnnotationValue {

    /** La etiqueta `B`: un `byte`. */
    public static final int TAG_BYTE = 'B';
    /** La etiqueta `C`: un `char`. */
    public static final int TAG_CHAR = 'C';
    /** La etiqueta `D`: un `double`. */
    public static final int TAG_DOUBLE = 'D';
    /** La etiqueta `F`: un `float`. */
    public static final int TAG_FLOAT = 'F';
    /** La etiqueta `I`: un `int`. */
    public static final int TAG_INT = 'I';
    /** La etiqueta `J`: un `long`. */
    public static final int TAG_LONG = 'J';
    /** La etiqueta `S`: un `short`. */
    public static final int TAG_SHORT = 'S';
    /** La etiqueta `Z`: un `boolean`. */
    public static final int TAG_BOOLEAN = 'Z';
    /** La etiqueta `s`: un `String`. */
    public static final int TAG_STRING = 's';
    /** La etiqueta `e`: una constante de enum. */
    public static final int TAG_ENUM = 'e';
    /** La etiqueta `c`: un literal de clase. */
    public static final int TAG_CLASS = 'c';
    /** La etiqueta `@`: una anotación anidada. */
    public static final int TAG_ANNOTATION = '@';
    /** La etiqueta `[`: un arreglo de valores. */
    public static final int TAG_ARRAY = '[';

    /** La etiqueta de esta forma; una de las constantes `TAG_*`. */
    int tag();

    /** Un valor que es una constante del pool. */
    public interface OfConstant extends AnnotationValue {

        /** La entrada del pool que lo lleva. */
        AnnotationConstantValueEntry constant();
    }

    /** Un `String`. */
    public interface OfString extends OfConstant {

        /** El `Utf8` con el texto. */
        Utf8Entry constant();

        /** El texto. */
        String stringValue();
    }

    /** Un `double`. */
    public interface OfDouble extends OfConstant {

        /** La entrada `CONSTANT_Double`. */
        DoubleEntry constant();

        /** El valor. */
        double doubleValue();
    }

    /** Un `float`. */
    public interface OfFloat extends OfConstant {

        /** La entrada `CONSTANT_Float`. */
        FloatEntry constant();

        /** El valor. */
        float floatValue();
    }

    /** Un `long`. */
    public interface OfLong extends OfConstant {

        /** La entrada `CONSTANT_Long`. */
        LongEntry constant();

        /** El valor. */
        long longValue();
    }

    /** Un `int`. */
    public interface OfInt extends OfConstant {

        /** La entrada `CONSTANT_Integer`. */
        IntegerEntry constant();

        /** El valor. */
        int intValue();
    }

    /** Un `short`, que el formato guarda en un `CONSTANT_Integer`. */
    public interface OfShort extends OfConstant {

        /** La entrada `CONSTANT_Integer`. */
        IntegerEntry constant();

        /** El valor. */
        short shortValue();
    }

    /** Un `char`, que el formato guarda en un `CONSTANT_Integer`. */
    public interface OfChar extends OfConstant {

        /** La entrada `CONSTANT_Integer`. */
        IntegerEntry constant();

        /** El valor. */
        char charValue();
    }

    /** Un `byte`, que el formato guarda en un `CONSTANT_Integer`. */
    public interface OfByte extends OfConstant {

        /** La entrada `CONSTANT_Integer`. */
        IntegerEntry constant();

        /** El valor. */
        byte byteValue();
    }

    /** Un `boolean`, que el formato guarda en un `CONSTANT_Integer` que vale 0 o 1. */
    public interface OfBoolean extends OfConstant {

        /** La entrada `CONSTANT_Integer`. */
        IntegerEntry constant();

        /** El valor. */
        boolean booleanValue();
    }

    /** Un literal de clase, o sea `Foo.class`. */
    public interface OfClass extends AnnotationValue {

        /** El `Utf8` con el descriptor de la clase. */
        Utf8Entry className();

        /** La clase. */
        default ClassDesc classSymbol() {
            return ClassDesc.ofDescriptor(className().stringValue());
        }
    }

    /** Una constante de enum. */
    public interface OfEnum extends AnnotationValue {

        /** El `Utf8` con el descriptor del enum. */
        Utf8Entry className();

        /** El enum. */
        default ClassDesc classSymbol() {
            return ClassDesc.ofDescriptor(className().stringValue());
        }

        /** El `Utf8` con el nombre de la constante. */
        Utf8Entry constantName();
    }

    /** Una anotación anidada. */
    public interface OfAnnotation extends AnnotationValue {

        /** La anotación. */
        Annotation annotation();
    }

    /** Un arreglo de valores. */
    public interface OfArray extends AnnotationValue {

        /** Los valores, en orden. */
        List<AnnotationValue> values();
    }

    /** La constante `constantName` del enum cuyo descriptor lleva `className`. */
    public static OfEnum ofEnum(Utf8Entry className, Utf8Entry constantName) {
        return Annotations.ofEnum(className, constantName);
    }

    /** La constante `constantName` del enum `enumClass`. */
    public static OfEnum ofEnum(ClassDesc enumClass, String constantName) {
        return Annotations.ofEnum(Annotations.utf8(descriptorOf(enumClass, "enumClass")),
                Annotations.utf8(constantName));
    }

    /** El literal de la clase cuyo descriptor lleva `className`. */
    public static OfClass ofClass(Utf8Entry className) {
        return Annotations.ofClass(className);
    }

    /** El literal de clase de `value`. */
    public static OfClass ofClass(ClassDesc value) {
        return Annotations.ofClass(Annotations.utf8(descriptorOf(value, "value")));
    }

    /** El `String` que lleva `value`. */
    public static OfString ofString(Utf8Entry value) {
        return Annotations.ofString(value);
    }

    /** El `String` `value`. */
    public static OfString ofString(String value) {
        return Annotations.ofString(Annotations.utf8(value));
    }

    /** El `double` que lleva `value`. */
    public static OfDouble ofDouble(DoubleEntry value) {
        return Annotations.ofDouble(value);
    }

    /** El `double` `value`. */
    public static OfDouble ofDouble(double value) {
        return Annotations.ofDouble(Annotations.doubleEntry(value));
    }

    /** El `float` que lleva `value`. */
    public static OfFloat ofFloat(FloatEntry value) {
        return Annotations.ofFloat(value);
    }

    /** El `float` `value`. */
    public static OfFloat ofFloat(float value) {
        return Annotations.ofFloat(Annotations.floatEntry(value));
    }

    /** El `long` que lleva `value`. */
    public static OfLong ofLong(LongEntry value) {
        return Annotations.ofLong(value);
    }

    /** El `long` `value`. */
    public static OfLong ofLong(long value) {
        return Annotations.ofLong(Annotations.longEntry(value));
    }

    /** El `int` que lleva `value`. */
    public static OfInt ofInt(IntegerEntry value) {
        return Annotations.ofInt(value);
    }

    /** El `int` `value`. */
    public static OfInt ofInt(int value) {
        return Annotations.ofInt(Annotations.intEntry(value));
    }

    /** El `short` que lleva `value`. */
    public static OfShort ofShort(IntegerEntry value) {
        return Annotations.ofShort(value);
    }

    /** El `short` `value`. */
    public static OfShort ofShort(short value) {
        return Annotations.ofShort(Annotations.intEntry(value));
    }

    /** El `char` que lleva `value`. */
    public static OfChar ofChar(IntegerEntry value) {
        return Annotations.ofChar(value);
    }

    /** El `char` `value`. */
    public static OfChar ofChar(char value) {
        return Annotations.ofChar(Annotations.intEntry(value));
    }

    /** El `byte` que lleva `value`. */
    public static OfByte ofByte(IntegerEntry value) {
        return Annotations.ofByte(value);
    }

    /** El `byte` `value`. */
    public static OfByte ofByte(byte value) {
        return Annotations.ofByte(Annotations.intEntry(value));
    }

    /** El `boolean` que lleva `value`. */
    public static OfBoolean ofBoolean(IntegerEntry value) {
        return Annotations.ofBoolean(value);
    }

    /** El `boolean` `value`. */
    public static OfBoolean ofBoolean(boolean value) {
        return Annotations.ofBoolean(Annotations.intEntry(value ? 1 : 0));
    }

    /** La anotación anidada `value`. */
    public static OfAnnotation ofAnnotation(Annotation value) {
        return Annotations.ofAnnotation(value);
    }

    /** El arreglo con estos valores. */
    public static OfArray ofArray(List<AnnotationValue> array) {
        return Annotations.ofArrayOfList(array);
    }

    /** El arreglo con estos valores. */
    public static OfArray ofArray(AnnotationValue... array) {
        return Annotations.ofArray(array);
    }

    /**
     * El valor que corresponde a `value`: una caja, un `String`, un `ClassDesc`, una constante de
     * enum, o un arreglo de cualquiera de esos. Tira `IllegalArgumentException` con cualquier otra
     * cosa — que es lo correcto: un `element_value` sólo puede ser una de esas formas.
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
